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
 * Attribute Value Filter; removes attributes whose value contains the specified content
 */
public class AttributeValueFilter extends DeXSSFilterImpl {
  final String name;
  final List contents;

  public AttributeValueFilter(DeXSSChangeListener xssChangeListener, List contents) {
    super(xssChangeListener);
    this.contents = contents;
    this.name = null;
  }

  public AttributeValueFilter(DeXSSChangeListener xssChangeListener, String name) {
    super(xssChangeListener);
    this.contents = new ArrayList();
    this.name = name;
  }

  /**
   * Adds contains to the list strings for attribute values that this filter should remove.
   * @param contains String to add
   */
  public void add(String contains) {
    contents.add(contains);
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    int nAttrs = atts.getLength();
    for (int attNum = 0; attNum < nAttrs; attNum++) {
      String attName = atts.getLocalName(attNum);
      String attValue = atts.getValue(attNum);
      if (attName.equals(name)) {
        int nContains = contents.size();
        for (int containsNum = 0; containsNum < nContains; containsNum++) {
          String contains = (String)contents.get(containsNum);
          if (attValue.indexOf(contains) != -1) {
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
