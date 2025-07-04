/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.learningobjects.cpxp.util.resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Localizes XML files in a ZIP.
 */
public class ZipXmlLocalizer  implements ResourceFilter {
    private static final Logger logger = Logger.getLogger(ZipXmlLocalizer.class.getName());
    private ResourceBundle _bundle;
    private String _path;

    private ZipXmlLocalizer(ResourceBundle bundle, String path) {
        _bundle = bundle;
        _path = path;
    }

    public void filter(InputStream in, OutputStream out) throws Exception {
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(out));

        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            zipOut.putNextEntry(new ZipEntry(entry.getName()));
            if (!entry.isDirectory()) {
                if (entry.getName().endsWith(".xml")) {
                    logger.log(Level.FINE, "Localizing XML file, {0}", entry.getName());
                    localize(zipIn, zipOut);
                } else {
                    IOUtils.copy(zipIn, zipOut);
                }
            }
        }
        zipOut.finish();
        zipOut.flush();
    }

    private void localize(InputStream in, OutputStream out) throws Exception {
        // Suppress close so the zip stream is not closed.
        in = new FilterInputStream(in) { public void close() {} };

        // Process XIncludes because java sucks
        in = processXInclude(in);

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEventReader reader = inputFactory.createXMLEventReader(in);
        XMLEventWriter writer = outputFactory.createXMLEventWriter(out, "UTF-8");
        while (reader.hasNext()) {
            XMLEvent event = (XMLEvent) reader.next();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                List<Attribute> attributes = new ArrayList<Attribute>();
                Iterator i = startElement.getAttributes();
                while (i.hasNext()) {
                    Attribute attribute = (Attribute) i.next();
                    String value = localize(attribute.getValue());
                    if (value != null) {
                        attribute = eventFactory.createAttribute(attribute.getName(), value);
                    }
                    attributes.add(attribute);
                }
                event = eventFactory.createStartElement(startElement.getName(), attributes.iterator(), startElement.getNamespaces());
            } else if (event.isCharacters()) {
                Characters characters = event.asCharacters();
                String data = localize(characters.getData());
                if (data != null) {
                    event = characters.isCData() ? eventFactory.createCData(data)
                        : eventFactory.createCharacters(data);
                }
            }
            writer.add(event);
        }
        writer.flush();
    }

    public static final String XINCLUDE_NS_URI = "http://www.w3.org/2001/XInclude";

    // I cannot get the built-in XInclude support in the DOM and SAX
    // parsers to work, so do it by hand..
    private InputStream processXInclude(InputStream in) throws Exception {
        DocumentBuilderFactory _domParserFactory = DocumentBuilderFactory.newInstance();
        _domParserFactory.setNamespaceAware(true);

        DocumentBuilder parser = _domParserFactory.newDocumentBuilder();
        Document document = parser.parse(in);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        NodeList nodes = document.getElementsByTagNameNS(XINCLUDE_NS_URI, "include");
        // I remove the include nodes as I go, and the NodeList is live, so
        // the nodes are removed from it to. Ugh.
        while (nodes.getLength() > 0) {
            Element element = (Element) nodes.item(0);
            String xpointer = element.getAttribute("xpointer");
            if (!xpointer.startsWith("xpointer(")) {
                if (xpointer.indexOf('(') >= 0) {
                    throw new RuntimeException("Invalid xpointer: " + xpointer);
                }
                xpointer = "xpointer(id('" + xpointer + "'))";
            }
            xpointer = xpointer.substring(9, xpointer.length() - 1);

            NodeList result = (NodeList) xPath.evaluate(xpointer, document, XPathConstants.NODESET);

            Node nextSibling = element.getNextSibling();
            Node parent = element.getParentNode();
            parent.removeChild(element);

            for (int j = 0; j < result.getLength(); ++ j) {
                Node child = result.item(j);
                parent.insertBefore(child.cloneNode(true), nextSibling);
            }
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(bytes));

        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static final Pattern TOKEN_RE = Pattern.compile("\\$\\{([^}]+)\\}");

    private String localize(String msg) {
        Matcher matcher = TOKEN_RE.matcher(msg);
        if (!matcher.find()) {
            return null;
        } else {
            StringBuffer sb = new StringBuffer();
            do {
                String key = matcher.group(1), replacement;
                try {
                    replacement = _bundle.getString(key);
                } catch (MissingResourceException ex) {
                    throw new RuntimeException("Unknown resource bundle key: " + key);
                }
                matcher.appendReplacement(sb,replacement);
            } while (matcher.find());
            matcher.appendTail(sb);
            return sb.toString();
        }
    }

    public String getPath() {
        return _path;
    }

    public static ZipXmlLocalizer getInstance(ResourceBundle bundle, String path) {
        return new ZipXmlLocalizer(bundle, path);
    }
}
