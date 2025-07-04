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

import java.util.Map;
import java.util.HashMap;
import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

/**
 * Element Replacement filter; replaces one element name with another, but leaves content alone.
 * Only local name is compared; namespace is ignored.
 */
public class ElementReplacementFilter extends DeXSSFilterImpl {
  final Map replacements;

  public ElementReplacementFilter(DeXSSChangeListener xssChangeListener) {
    this(xssChangeListener, new HashMap());
  }

  public ElementReplacementFilter(DeXSSChangeListener xssChangeListener, Map replacements) {
    super(xssChangeListener);
    this.replacements = replacements;
  }

  /**
   * Adds from and to to the list of element names for elements names that this filter should rename.
   * Only local name is compared; namespace is ignored.
   * @param from old name
   * @param to new name
   */
  public void add(String from, String to) {
    replacements.put(from.toLowerCase(), to); // merlin
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    String replacement = (String)(replacements.get(localName.toLowerCase())); // merlin
    if (replacement != null) {
      // TODO: What about namespace and qName?
      logXSSChange("Replacing element", localName, replacement);
      super.startElement(namespaceURI, replacement, qName, atts);
    }
    else
      super.startElement(namespaceURI, localName, qName, atts);
  }
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    String replacement = (String)(replacements.get(localName));
    if (replacement != null)
      super.endElement(namespaceURI, replacement, qName);
    else
      super.endElement(namespaceURI, localName, qName);
  }
}



