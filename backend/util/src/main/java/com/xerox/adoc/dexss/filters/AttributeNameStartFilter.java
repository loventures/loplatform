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
 * Attribute Start Filter; removes attributes whose name starts with the specified name
 */
public class AttributeNameStartFilter extends DeXSSFilterImpl {
  final List starts;

  public AttributeNameStartFilter(DeXSSChangeListener xssChangeListener) {
    this(xssChangeListener, new ArrayList());
  }


  public AttributeNameStartFilter(DeXSSChangeListener xssChangeListener, List starts) {
    super(xssChangeListener);
    this.starts = starts;
  }

  public AttributeNameStartFilter(DeXSSChangeListener xssChangeListener, String startsWith) {
    this(xssChangeListener, new ArrayList());
    add(startsWith);
  }

  /**
   * Adds startsWith to the list of starting strings of attribute names that this filter should remove.
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
      int nStarts = starts.size();
      for (int startsWithNum = 0; startsWithNum < nStarts; startsWithNum++) {
        String startsWith = (String)starts.get(startsWithNum);
        if (attName.startsWith(startsWith)) {
          atts = Utils.removeAttribute(atts, attNum);
          if (xssChangeListener != null)
            xssChangeListener.logXSSChange("Removing attribute", localName, attName);
          attNum--;
          nAttrs--;
          break;
        }
      }
    }
    super.startElement(namespaceURI, localName, qName, atts);
  }
}
