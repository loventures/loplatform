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

package com.xerox.adoc.dexss;

import java.io.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.learningobjects.tagsoup.XMLWriter;

/**
 * Call createInstance to specify an xssChangeWriter and a Writer.
 * Call process to parse inputString using TagSoup.
 * DeXSS notes any changes to xssChangeWriter, and serializes the resulting document to the Writer.
 */
public class DeXSS {

  private DeXSSParser dexssParser;
  private InputSource inputSource;

  /**
   * Create a DeXSS object using createInstance.  Modifications will be noted to xssChangeListener, and the
   * resulting document serialized to w.
   * @param xssChangeListener the DeXSSChangeListener which is informed of changes
   * @param w the Writer to which the parsed document is serialized
   * @return a DeXSS object
   */
  public static DeXSS createInstance(DeXSSChangeListener xssChangeListener, Writer w) throws SAXException {
    XMLWriter x = new XMLWriter(w);
    x.setOutputProperty(XMLWriter.OMIT_XML_DECLARATION, "yes");
    DeXSS dexss = new DeXSS(xssChangeListener, x);
    return dexss;
  }

  private DeXSS(DeXSSChangeListener xssChangeListener, ContentHandler h) throws SAXException {
    dexssParser = new DeXSSParser();
    dexssParser.setDeXSSChangeListener(xssChangeListener);
    dexssParser.setContentHandler(h);
    //dexssParser.setProperty(Parser.lexicalHandlerProperty, h);
    inputSource = new InputSource();
  }

  /**
   * Call createInstance to specify an xssChangeWriter and a Writer.
   * Call process to parse inputString using TagSoup.
   * DeXSS notes any changes to xssChangeWriter, and serializes the resulting document to the Writer.
   * @param inputString the String to parse
   */
  public void process(String inputString) throws IOException, SAXException {
    // http://java.sun.com/j2se/1.4.2/docs/api/index.html
    // "Once a parse is complete, an application may reuse the same XMLReader object, possibly with a different input source."
    // Might need to set the reuse flag.
    // XMLWriter.startDocument resets the XMLWriter, but doesn't clear its namespaces.
    inputSource.setCharacterStream(new StringReader(inputString));
    dexssParser.parse(inputSource);
  }
}
