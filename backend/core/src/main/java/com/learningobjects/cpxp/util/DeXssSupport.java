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

package com.learningobjects.cpxp.util;

import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.tagsoup.Parser;
import com.learningobjects.tagsoup.XMLWriter;
import com.xerox.adoc.dexss.DeXSSChangeListener;
import com.xerox.adoc.dexss.DeXSSParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consolidated support code for using the tagsoup HTML/JavaScript defanger to
 * strip out any potential cross site script attack code on demand.
 */
public class DeXssSupport  {
    private static final Logger logger = Logger.getLogger(DeXssSupport.class.getName());
    private static final DeXssSupport SINGLETON = new DeXssSupport();

    public static String deXss(String html, String location) {
        return SINGLETON.deXssvalue(html, location, null);
    }

    public static String wellFormed(String html) {
        return SINGLETON.wellFormedvalue(html);
    }

    /**
     * **NOT GENERAL PURPOSE**
     * DeXSS and rewrite img srcs as absolute  with a ?rss=true suffix.
     */
    public static String deXssRss(String html, String location, String baseUrl) {
        return SINGLETON.deXssvalue(html, location, baseUrl);
    }

    private String deXssvalue(String html, String location, String baseUrl) {

        if (html == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        try {
            XMLWriter xw;
            if (baseUrl == null) {
                xw = new XMLWriter(sw);
            } else {
                xw = new RssXMLWriter(sw, baseUrl);
            }
            xw.setOutputProperty(XMLWriter.OMIT_XML_DECLARATION, "yes");
            xw.setOutputProperty(XMLWriter.METHOD, "html");
            xw.setOutputProperty(XMLWriter.ENCODING, "UTF-8");
            DeXSSParser dexssParser = new DeXSSParser(getFilterProperties());
            dexssParser.setDeXSSChangeListener(new ChangeListener(location));
            dexssParser.setContentHandler(xw);
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(html));
            dexssParser.parse(inputSource);
            String filtered = sw.toString();
            return filtered;
        } catch (Exception ex) {
            throw new RuntimeException("Dexss error", ex);
        }
    }

    private static class RssXMLWriter extends XMLWriter {
        private String _base;

        public RssXMLWriter(StringWriter sw, String base) {
            super(sw);
            _base = base;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement(uri, localName, qName, fixAttributes(qName, atts));
        }

        @Override
        public void emptyElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.emptyElement(uri, localName, qName, fixAttributes(qName, atts));
        }

        private Attributes fixAttributes(String qName, Attributes atts) {
            atts = fixHrefAttribute(qName, atts);
            atts = fixSrcAttribute(qName, atts);
            return atts;
        }

        private Attributes fixHrefAttribute(String qName, Attributes atts) {
            String href = atts.getValue("href");
            if (href == null || !href.startsWith("/")) {
                return atts;
            }
            AttributesImpl attributes = new AttributesImpl(atts);
            attributes.setValue(attributes.getIndex("href"), _base + href.substring(1));
            return attributes;
        }

        private Attributes fixSrcAttribute(String qName, Attributes atts) {
            String src = atts.getValue("src");
            if (!"img".equals(qName) || ((src == null) || !src.startsWith("/"))) {
                return atts;
            }
            String suffix = src.contains("?") ? "&rss=true" : "?rss=true";
            AttributesImpl attributes = new AttributesImpl(atts);
            attributes.setValue(attributes.getIndex("src"), _base + src.substring(1) + suffix);
            return attributes;
        }
    }

    private Properties getFilterProperties() {
        Properties properties = new Properties();
        // used to be used to carve out allowed media types, see prior version
        // of this file for details
        DomainDTO dto = Current.getDomainDTO();
        if (dto != null && dto.getHostName() != null) { // hostName == null on ovërlord
            properties.setProperty("prohibitedIFrameSrcs", dto.getHostName());
        }
        return properties;
    }

    private class ChangeListener implements DeXSSChangeListener {
        private String _location;

        public ChangeListener(String location) {
            _location = location;
        }

        public void logXSSChange(String message) {
            logger.log(Level.FINE,
                    "XSS detected, location=" + _location + ": " + message);
        }

        public void logXSSChange(String message, String item1) {
            logger.log(
                    Level.FINE,
                    "XSS detected, location=" + _location + ": " + message
                            + " / " + item1);
        }

        public void logXSSChange(String message, String item1, String item2) {
            logger.log(
                    Level.FINE,
                    "XSS detected, location=" + _location + ": " + message
                            + " / " + item1 + " / " + item2);
        }
    }

    private String wellFormedvalue(String html) {

        if (html == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        try {
            XMLWriter xw = new XMLWriter(sw);
            xw.setOutputProperty(XMLWriter.OMIT_XML_DECLARATION, "yes");
            xw.setOutputProperty(XMLWriter.METHOD, "html");
            xw.setOutputProperty(XMLWriter.ENCODING, "UTF-8");

            Parser parser = new Parser();
            parser.setFeature(Parser.ignoreBogonsFeature, true);
            parser.setFeature(Parser.defaultAttributesFeature, false);
            parser.setFeature(Parser.outerImplicitElementsFeature, false);
            parser.setContentHandler(xw);

            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(html));
            parser.parse(inputSource);

            String filtered = sw.toString();
            return filtered;
        } catch (Exception ex) {
            throw new RuntimeException("Wellformed error", ex);
        }
    }

}
