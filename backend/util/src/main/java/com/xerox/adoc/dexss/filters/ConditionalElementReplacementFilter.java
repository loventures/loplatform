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

import java.net.URI;
import java.util.*;
import java.util.regex.*;

import org.xml.sax.*;
import com.xerox.adoc.dexss.*;

// merlin

/**
 * Element Replacement filter; replaces one element name with another, but leaves content alone.
 * Only local name is compared; namespace is ignored. Skips replacement if certain conditions
 * are set.
 */
public class ConditionalElementReplacementFilter extends DeXSSFilterImpl {
  final Map exceptions;
  final BitSet matches;
  int level = 0;

  public ConditionalElementReplacementFilter(DeXSSChangeListener xssChangeListener) {
    super(xssChangeListener);
    this.exceptions = new HashMap();
    this.matches = new BitSet();
  }

  private String original;
  private String replacement;

  public void set(String from, String to) {
      original = from;
      replacement = to;
  }

  public void addException(String attr, String values) {
    exceptions.put(attr.toLowerCase(), Pattern.compile(values));
  }

  private Set _hosts;

  public void setProhibitedHosts(String hosts) {
      _hosts = new HashSet();
      for (String host : hosts.split(",")) {
          _hosts.add(host.toLowerCase());
      }
  }

  // The BitSet junk is to handle nested elements. Overkill, I'm sure.

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
      if (localName.equalsIgnoreCase(original)) {
          StringBuilder failed = new StringBuilder(); // in case attr occurs twice, once good, once bad..
          Set matched = new HashSet(); // make sure all match
          int nAttrs = atts.getLength();
          for (int attNum = 0; attNum < nAttrs; attNum++) {
              String attName = atts.getLocalName(attNum).toLowerCase();
              Pattern allowedValues = (Pattern) exceptions.get(attName);
              if (allowedValues != null) {
                  String attValue = atts.getValue(attNum);
                  boolean okay;
                  if (_hosts != null) {
                      try {
                          // it's okay to only check the exact hostname because the browser cookie
                          // is only sent to the host by name, so tricks such as IP address and the
                          // like aren't a security issue
                          URI uri = new URI(attValue);
                          okay = (uri.getHost() != null) && !_hosts.contains(uri.getHost());
                      } catch (Exception ex) {
                          okay = false;
                      }
                  } else {
                      okay = allowedValues.matcher(attValue).matches();
                  }
                  if (okay) {
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
              logXSSChange("Replacing element", localName, failed.toString());
              localName = replacement;
              matches.set(level);
          }
      }
      ++ level;
      super.startElement(namespaceURI, localName, qName, atts);
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
      -- level;
      if (matches.get(level)) {
          localName = replacement;
          matches.clear(level);
      }
      super.endElement(namespaceURI, localName, qName);
  }
}



// /merlin
