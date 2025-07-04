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
 * Element Content filter; removes element content if it matches cetain patterns.
 */
public class ElementContentFilter extends DeXSSFilterImpl {
  final List filters;
  final StringBuilder content;
  int level = 0;

  public ElementContentFilter(DeXSSChangeListener xssChangeListener) {
    super(xssChangeListener);
    this.filters = new ArrayList();
    this.content = new StringBuilder();
  }

  private String filter;

  public void set(String f) {
      filter = f;
  }

  public void addPattern(String regex) {
      filters.add(Pattern.compile(regex)); // case?
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
      if (level > 0) { // nested element
          ++ level;
      } else {
          if (localName.equalsIgnoreCase(filter)) { // start match
              level = 1;
              content.setLength(0);
          }
          super.startElement(namespaceURI, localName, qName, atts);
      }
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
      if (level > 1) { // nested element
          -- level;
      } else {
          if (level == 1) { // end match
              -- level;
              String text = content.toString();
              boolean matched = false;
              for (Object o: filters) {
                  Pattern pattern = (Pattern) o;
                  if (pattern.matcher(text).find()) {
                      matched = true;
                  }
              }
              if (matched) {
                  logXSSChange("Removing element content", localName, text);
              } else {
                  super.characters(text.toCharArray(), 0, text.length());
              }
          }
          super.endElement(namespaceURI, localName, qName);
      }
  }

  public void characters(char[] ch, int start, int length) throws SAXException {
      if (level == 0) {
          super.characters(ch, start, length);
      } else if (level == 1) {
          content.append(ch, start, length);
      }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      if (level == 0) {
          super.ignorableWhitespace(ch, start, length);
      } else if (level == 1) {
          content.append(ch, start, length);
      }
  }
}

// /merlin
