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

import java.util.Set;
import java.util.HashSet;
import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

/**
 * Element Lifting Filter; Lifts content of matching element (and its attributes) by eliding it and replacing it with its own content.
 */
public class ElementLiftingFilter extends DeXSSFilterImpl {
  final Set tagnames;

  public ElementLiftingFilter(DeXSSChangeListener xssChangeListener) {
    this(xssChangeListener, new HashSet());
  }

  public ElementLiftingFilter(DeXSSChangeListener xssChangeListener, Set tagnames) {
    super(xssChangeListener);
    this.tagnames = tagnames;
  }

  /**
   * Adds tagname to the list of names for element names that this filter should "lift".
   * @param tagname tagname to add
   */
  public void add(String tagname) {
    tagnames.add(tagname);
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    if (! tagnames.contains(localName)) {
      logXSSChange("Lifting element", localName);
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    if (! tagnames.contains(localName))
      super.endElement(namespaceURI, localName, qName);
  }
}



