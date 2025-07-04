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

import com.learningobjects.tagsoup.AttributesImpl;
import com.xerox.adoc.dexss.DeXSSChangeListener;
import com.xerox.adoc.dexss.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.HashSet;
import java.util.Set;

/**
 * Attribute Removal Filter;
 * Removes attributes matching names added with {@link #add(String)}.
 */
public class AttributeNameRemovalFilter extends DeXSSFilterImpl {
  final Set attributeLocalNames;

  public AttributeNameRemovalFilter(DeXSSChangeListener xssChangeListener) {
    this(xssChangeListener, new HashSet());
  }

  /**
   * Adds name to the list names regexps for attribute names that this filter should remove.
   * @param name name to add
   */
  public void add(String name) {
    attributeLocalNames.add(name);
  }

  public AttributeNameRemovalFilter(DeXSSChangeListener xssChangeListener, Set attributeLocalNames) {
    super(xssChangeListener);
    this.attributeLocalNames = attributeLocalNames;
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    int nAttrs = atts.getLength();
    for (int attNum = 0; attNum < nAttrs; attNum++) {
      String attName = atts.getLocalName(attNum);
      if (attributeLocalNames.contains(attName)) {
        if (atts instanceof com.learningobjects.tagsoup.AttributesImpl) {
          atts = Utils.removeAttribute(atts, attNum);
          if (xssChangeListener != null)
            xssChangeListener.logXSSChange("Removing attribute", localName, attName);
          nAttrs--;
          attNum--;
        }
      }
     atts =  Utils.handleTargetAttribute((AttributesImpl) atts, attName);
    }
    super.startElement(namespaceURI, localName, qName, atts);
  }


}
