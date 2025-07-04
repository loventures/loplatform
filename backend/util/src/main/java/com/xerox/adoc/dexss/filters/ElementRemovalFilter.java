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

package com.xerox.adoc.dexss.filters;

import org.xml.sax.*;
import com.xerox.adoc.dexss.*;
/**
 * Element Removal Filter;
 * Removes the specified element and all its content.
 */
public class ElementRemovalFilter extends DeXSSFilterImpl {
  int elementLevel = 0 ;
  final String elementName;

  /**
   * Element Removal Filter
   * Removes Elements matching elementName.
   */
  public ElementRemovalFilter(DeXSSChangeListener xssChangeListener, String elementName) {
    super(xssChangeListener);
    this.elementName = elementName;
  }

  public void startElement(String namespaceURI, String localName,
   String qName, Attributes atts) throws SAXException {
    if (localName.equalsIgnoreCase(elementName)) {
      elementLevel++;
      logXSSChange("Removing element", elementName);
    } else {
      if (elementLevel == 0)
        super.startElement(namespaceURI, localName, qName, atts);
    }
  }
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    if (localName.equalsIgnoreCase(elementName)) {
      elementLevel--;
    } else {
      if (elementLevel == 0)
        super.endElement(namespaceURI, localName, qName);
    }
  }
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (elementLevel == 0)
      super.characters(ch, start, length);
  }

  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (elementLevel == 0)
      super.ignorableWhitespace(ch, start, length);
  }
}



