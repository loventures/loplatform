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
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

/**
 * AttributeValue Regexp Filter; removes attributes whose content matches the specified regexp
 */
public class AttributeValueRegexpFilter extends DeXSSFilterImpl {
  final Map names;
  final List denied;
  final int flags;

  public AttributeValueRegexpFilter(DeXSSChangeListener xssChangeListener) {
    super(xssChangeListener);
    this.names = new HashMap();
    this.denied = new ArrayList();
    this.flags=Pattern.CASE_INSENSITIVE;
  }

    // merlin
    /** Filter this attribate on that element (or *). */
    public void filter(String elementName, String attributeName) {
        names.put(attributeName, elementName); // cheaper? to use a multivaluemap/set?
    }

    private int queryIndex = -1;

  /**
   * Adds regexp to the list of regexps for attribute values that this filter should remove.
   * @param regexp regexp to add
   */
  public void deny(String regexp) {
      if (regexp.indexOf("\\?") >= 0) { // hack to speed up the query regex.. not right.
          queryIndex = denied.size();
      }
    denied.add(Pattern.compile(regexp, flags));
  }
    // /merlin

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    int nAttrs = atts.getLength();
    for (int attNum = 0; attNum < nAttrs; attNum++) {
      String attName = atts.getLocalName(attNum);
      // merlin
      String elementName = (String) names.get(attName);
      if ("*".equals(elementName) || localName.equals(elementName)) {
        String attValue = atts.getValue(attNum);
        int nRegexps = denied.size();
        for (int regexpNum = 0; regexpNum < nRegexps; regexpNum++) {
          Pattern pattern = (Pattern)denied.get(regexpNum);
          // speed up the query check by only running it if there is a query string
          boolean prereq = (regexpNum != queryIndex) || (attValue.indexOf('?') >= 0);
          if (prereq && pattern.matcher(attValue).find()) { // TODO: Is there a way to re-use matcher in a single thread?
            atts = Utils.removeAttribute(atts, attNum);
            logXSSChange("Removing attribute", localName, attName);
            attNum--;
            nAttrs--;
            break;
          }
        }
      }
      // merlin
    }
    super.startElement(namespaceURI, localName, qName, atts);
  }
}
