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

import java.util.*;
import java.util.regex.*;

import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

// merlin

/**
 * Element Removal filter; removes one element and comment.
 * Only local name is compared; namespace is ignored. Skips removal if certain conditions
 * are set.
 */
public class ConditionalElementRemovalFilter extends DeXSSFilterImpl {
  final Map exceptions;
  int level = 0;

  public ConditionalElementRemovalFilter(DeXSSChangeListener xssChangeListener) {
    super(xssChangeListener);
    this.exceptions = new HashMap();
  }

  private String remove;

  public void set(String r) {
      remove = r;
  }

  public void addException(String attr, String values) {
      exceptions.put(attr.toLowerCase(), Pattern.compile(values));
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
      if (level > 0) {
          ++ level;
      } else if (localName.equalsIgnoreCase(remove)) {
          // TODO: share code w/ replacement filter
          StringBuilder failed = new StringBuilder(); // in case attr occurs twice, once good, once bad..
          Set matched = new HashSet(); // make sure all match
          int nAttrs = atts.getLength();
          for (int attNum = 0; attNum < nAttrs; attNum++) {
              String attName = atts.getLocalName(attNum).toLowerCase();
              Pattern allowedValues = (Pattern) exceptions.get(attName);
              if (allowedValues != null) {
                  String attValue = atts.getValue(attNum);
                  if (allowedValues.matcher(attValue).matches()) {
                      matched.add(attName);
                  } else {
                      failed.append(attValue);
                  }
              }
          }
          for (Object o: exceptions.entrySet()) {
              Map.Entry entry = (Map.Entry) o;
              Pattern pattern = (Pattern) entry.getValue();
              if ("".equals(pattern.pattern())) {
                  matched.add(entry.getKey()); // ugly
              }
          }
          if ((failed.length() > 0) || !matched.equals(exceptions.keySet())) {
              // TODO: What about namespace and qName?
              logXSSChange("Removing element", localName, failed.toString());
              level = 1;
          }
      }
      if (level == 0) {
          super.startElement(namespaceURI, localName, qName, atts);
      }
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
      if (level == 0) {
          super.endElement(namespaceURI, localName, qName);
      } else {
          -- level;
      }
  }

  public void characters(char[] ch, int start, int length) throws SAXException {
      if (level == 0) {
          super.characters(ch, start, length);
      }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      if (level == 0) {
          super.ignorableWhitespace(ch, start, length);
      }
  }
}



// /merlin
