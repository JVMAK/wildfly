package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.NAME;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.Constants.VALUE;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

final class WebservicesSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final WebservicesSubsystemParser INSTANCE = new WebservicesSubsystemParser();

    static WebservicesSubsystemParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode subsystem = context.getModelNode();
        if (has(subsystem, MODIFY_WSDL_ADDRESS)) {
            final String modifyWsdlAddressValue = subsystem.require(MODIFY_WSDL_ADDRESS).asString();
            writeElement(writer, MODIFY_WSDL_ADDRESS, modifyWsdlAddressValue);
        }
        if (has(subsystem, WSDL_HOST)) {
            final String wsdlHostValue = subsystem.require(WSDL_HOST).asString();
            writeElement(writer, WSDL_HOST, wsdlHostValue);
        }
        if (has(subsystem, WSDL_PORT)) {
            final String wsdlPortValue = subsystem.require(WSDL_PORT).asString();
            writeElement(writer, WSDL_PORT, wsdlPortValue);
        }
        if (has(subsystem, WSDL_SECURE_PORT)) {
            final String wsdlSecurePortValue = subsystem.require(WSDL_SECURE_PORT).asString();
            writeElement(writer, WSDL_SECURE_PORT, wsdlSecurePortValue);
        }
        if (has(subsystem, ENDPOINT_CONFIG)) {
            final ModelNode endpointConfigs = subsystem.get(ENDPOINT_CONFIG);
            writeEndpointConfigs(writer, endpointConfigs);
        }
        writer.writeEndElement();
    }

    private void writeEndpointConfigs(final XMLExtendedStreamWriter writer, final ModelNode endpointConfigs) throws XMLStreamException {
        ModelNode endpointConfig = null;
        for (final String configName : endpointConfigs.keys()) {
            endpointConfig = endpointConfigs.get(configName);
            // start endpoint-config element
            writer.writeStartElement(Constants.ENDPOINT_CONFIG);
            writer.writeAttribute(Constants.NAME, configName);
            // write pre-handler-chain elements
            if (endpointConfig.hasDefined(Constants.PRE_HANDLER_CHAIN)) {
                final ModelNode handlerChains = endpointConfig.get(Constants.PRE_HANDLER_CHAIN);
                writeHandlerChains(writer, handlerChains, true);
            }
            // write post-handler-chain elements
            if (endpointConfig.hasDefined(Constants.POST_HANDLER_CHAIN)) {
                final ModelNode handlerChains = endpointConfig.get(Constants.POST_HANDLER_CHAIN);
                writeHandlerChains(writer, handlerChains, false);
            }
            // write property elements
            if (endpointConfig.hasDefined(Constants.PROPERTY)) {
                final ModelNode properties = endpointConfig.get(PROPERTY);
                writeProperties(writer, properties);
            }
            // close endpoint-config element
            writer.writeEndElement();
        }
    }

    private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode properties) throws XMLStreamException {
        ModelNode property = null;
        String propertyValue = null;
        for (final String propertyName : properties.keys()) {
            property = properties.get(propertyName);
            writer.writeStartElement(PROPERTY);
            writer.writeAttribute(NAME, propertyName);
            if (property.hasDefined(VALUE)) {
                propertyValue = property.get(VALUE).asString();
                writer.writeAttribute(VALUE, propertyValue);
            }
            writer.writeEndElement();
        }
    }

    private void writeHandlerChains(final XMLExtendedStreamWriter writer, final ModelNode handlerChains, final boolean isPre) throws XMLStreamException {
        ModelNode handlerChain = null;
        ModelNode handler = null;
        for (final String handlerChainId : handlerChains.keys()) {
            handlerChain = handlerChains.get(handlerChainId);
            writer.writeStartElement(isPre ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN);
            writer.writeAttribute(NAME, handlerChainId);
            if (handlerChain.hasDefined(PROTOCOL_BINDINGS)) {
                final String protocolBinding = handlerChain.get(PROTOCOL_BINDINGS).asString();
                writer.writeAttribute(PROTOCOL_BINDINGS, protocolBinding);
            }
            if (handlerChain.hasDefined(HANDLER)) {
                for (final String handlerName : handlerChain.require(HANDLER).keys()) {
                    handler = handlerChain.get(HANDLER).get(handlerName);
                    writer.writeStartElement(HANDLER);
                    writer.writeAttribute(NAME, handlerName);
                    writer.writeAttribute(CLASS, handler.get(CLASS).asString());
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
    }

    private void writeElement(final XMLExtendedStreamWriter writer, final String elementName, final String elementValue) throws XMLStreamException {
        writer.writeStartElement(elementName);
        writer.writeCharacters(elementValue);
        writer.writeEndElement();
    }

    private boolean has(final ModelNode node, final String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME);

        final List<ModelNode> endpointConfigs = new ArrayList<ModelNode>();

        // elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEBSERVICES_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element != Element.ENDPOINT_CONFIG && !encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case MODIFY_WSDL_ADDRESS: {
                            boolean b = Boolean.parseBoolean(parseElementNoAttributes(reader));
                            subsystem.get(MODIFY_WSDL_ADDRESS).set(b);
                            break;
                        }
                        case WSDL_HOST: {
                            subsystem.get(WSDL_HOST).set(parseElementNoAttributes(reader));
                            break;
                        }
                        case WSDL_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_PORT).set(port);
                            break;
                        }
                        case WSDL_SECURE_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_SECURE_PORT).set(port);
                            break;
                        }
                        case ENDPOINT_CONFIG: {
                            readEndpointConfig(reader, subsystem.get(OP_ADDR), endpointConfigs);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        list.add(subsystem);
        list.addAll(endpointConfigs);
    }

    private String parseElementNoAttributes(XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        return reader.getElementText().trim();
    }

    private void readEndpointConfig(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> operationList) throws XMLStreamException {
        String configName = null;

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.PROPERTY
                    && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case CONFIG_NAME: {
                    configName = parseElementNoAttributes(reader);
                    final ModelNode node = new ModelNode();
                    node.get(OP).set(ADD);
                    node.get(OP_ADDR).set(address).add(ENDPOINT_CONFIG, configName);
                    operationList.add(node);
                    break;
                }
                case PRE_HANDLER_CHAINS: {
                    parseHandlerChains(reader, configName, operationList, true);
                    break;
                }
                case POST_HANDLER_CHAINS: {
                    parseHandlerChains(reader, configName, operationList, false);
                    break;
                }
                case PROPERTY : {
                    final ModelNode operation = parseProperty(reader, configName);
                    operationList.add(operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseProperty(XMLExtendedStreamReader reader, final String configName) throws XMLStreamException {
        String propertyName = null;
        String propertyValue = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PROPERTY_NAME: {
                    propertyName = parseElementNoAttributes(reader);
                    break;
                }
                case PROPERTY_VALUE : {
                    propertyValue = parseElementNoAttributes(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(PROPERTY, propertyName);
        if (propertyValue != null) {
            operation.get(VALUE).set(propertyValue);
        }
        return operation;
    }

    private ModelNode parseHandlerChains(final XMLExtendedStreamReader reader, final String configName, final List<ModelNode> operationList, final boolean isPreHandlerChain) throws XMLStreamException {
        ModelNode chainsNode = new ModelNode();
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER_CHAIN && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }

            switch (element) {
                case HANDLER_CHAIN: {
                    parseHandlerChain(reader, configName, operationList, isPreHandlerChain);
                    break;
                }

                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return chainsNode;

    }

    private void parseHandlerChain(final XMLExtendedStreamReader reader, final String configName, final List<ModelNode> operationList, final boolean isPreHandlerChain) throws XMLStreamException {
        String handlerChainId = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case ID:
                handlerChainId = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        if (handlerChainId == null) {
            handlerChainId = "auto-generated-" + System.currentTimeMillis();
        }
        String protocolBindings = null;
        final List<ModelNode> addHandlerOperations = new LinkedList<ModelNode>();
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PROTOCOL_BINDINGS: {
                    protocolBindings = parseElementNoAttributes(reader);
                    break;
                }
                case HANDLER: {
                    parseHandler(reader, configName, handlerChainId, isPreHandlerChain, addHandlerOperations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId);
        if (protocolBindings != null) {
            operation.get(Constants.PROTOCOL_BINDINGS).set(protocolBindings);
        }
        operationList.add(operation);
        operationList.addAll(addHandlerOperations);
    }

    private void parseHandler(XMLExtendedStreamReader reader, final String configName, final String handlerChainId, final boolean isPreHandlerChain, final List<ModelNode> operations) throws XMLStreamException {
        String handlerName = null;
        String handlerClass = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case HANDLER_NAME: {
                    handlerName = parseElementNoAttributes(reader);
                    break;
                }
                case HANDLER_CLASS: {
                    handlerClass = parseElementNoAttributes(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId).add(HANDLER, handlerName);
        operation.get(CLASS).set(handlerClass);
        operations.add(operation);
    }
}