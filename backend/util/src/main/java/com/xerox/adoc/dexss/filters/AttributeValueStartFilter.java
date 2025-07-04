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

import java.util.List;
import java.util.ArrayList;
import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

/**
 * Attribute Start Filter; removes attributes whose content starts with the specified content
 */
public class AttributeValueStartFilter extends DeXSSFilterImpl {
  final String name;
  final List starts;

  public AttributeValueStartFilter(DeXSSChangeListener xssChangeListener, String name, List starts) {
    super(xssChangeListener);
    this.starts = starts;
    this.name = name;
  }

  public AttributeValueStartFilter(DeXSSChangeListener xssChangeListener, String name) {
    super(xssChangeListener);
    this.starts = new ArrayList();
    this.name = name;
  }

  /**
   * Adds startsWith to the list of startting strings for attribute values that this filter should remove.
   * @param startsWith String to add
   */
  public void add(String startsWith) {
    starts.add(startsWith);
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    int nAttrs = atts.getLength();
    for (int attNum = 0; attNum < nAttrs; attNum++) {
      String attName = atts.getLocalName(attNum);
      String attValue = atts.getValue(attNum);
      if (attName.equals(name)) {
        int nStarts = starts.size();
        for (int startsWithNum = 0; startsWithNum < nStarts; startsWithNum++) {
          String startsWith = (String)starts.get(startsWithNum);
          if (attValue.startsWith(startsWith)) { // BUG: Needs to ignore leading whitespace!
            atts = Utils.removeAttribute(atts, attNum);
            logXSSChange("Removing attribute", localName, attName);
            attNum--;
            nAttrs--;
            break;
          }
        }
      }
    }
    super.startElement(namespaceURI, localName, qName, atts);
  }
}
