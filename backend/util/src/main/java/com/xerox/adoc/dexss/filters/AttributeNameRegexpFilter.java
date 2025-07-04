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
import java.util.regex.Pattern;
import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

/**
 * Attribute Removal Filter;
 * Removes attributes matching regexps added with {@link #add(String)}.
 */
public class AttributeNameRegexpFilter extends DeXSSFilterImpl {
  final List regexps;
  final int flags;

  public AttributeNameRegexpFilter(DeXSSChangeListener xssChangeListener, List regexps) {
    super(xssChangeListener);
    this.flags = 0;
    this.regexps = regexps;
  }

  public AttributeNameRegexpFilter(DeXSSChangeListener xssChangeListener, String regexp) {
    this(xssChangeListener, new ArrayList());
    add(regexp);
  }

  /**
   * Adds regexp to the list of regexps for attribute names that this filter should remove.
   * @param regexp regexp to add
   */
  public void add(String regexp) {
    regexps.add(Pattern.compile(regexp, flags));
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    int nAttrs = atts.getLength();
    for (int attNum = 0; attNum < nAttrs; attNum++) {
      String attName = atts.getLocalName(attNum);
      int nRegexps = regexps.size();
      for (int regexpNum = 0; regexpNum < nRegexps; regexpNum++) {
        Pattern pattern = (Pattern)regexps.get(regexpNum);
        if (pattern.matcher(attName).find()) { // TODO: Is there a way to re-use matcher in a single thread?
          atts = Utils.removeAttribute(atts, attNum);
          logXSSChange("Removing attribute", localName, attName);
          attNum--;
          nAttrs--;
          break;
        }
      }
    }
    super.startElement(namespaceURI, localName, qName, atts);
  }
}
