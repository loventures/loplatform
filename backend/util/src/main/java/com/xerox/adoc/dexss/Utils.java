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

package com.xerox.adoc.dexss;

import org.xml.sax.Attributes;

/**
 * Utility functions
 */
public abstract class Utils {
  /**
   * Given a SAX2 Attributes and an index, remove the specified attribute as best we can.
   * If the implementation is the SAX Helpers AttributesImpl, use its removeAttribute.
   * If the implementation is TagSoup's, use its removeAttribute.
   * Otherwise, convert it to a SAX Helpers implementation and use its removeAttribute.
   */
  public static Attributes removeAttribute(Attributes atts, int attNum) {
    if (atts instanceof com.learningobjects.tagsoup.AttributesImpl) {
      ((com.learningobjects.tagsoup.AttributesImpl)atts).removeAttribute(attNum);
      return atts;
    } else if (atts instanceof org.xml.sax.helpers.AttributesImpl) {
      ((org.xml.sax.helpers.AttributesImpl)atts).removeAttribute(attNum);
      return atts;
    } else {
      org.xml.sax.helpers.AttributesImpl newatts = new org.xml.sax.helpers.AttributesImpl(atts);
      newatts.removeAttribute(attNum);
      return newatts;
    }
  }

  /**
   * Given a SAX2 Attributes and an index, remove the specified attribute as best we can.
   * If the implementation is the SAX Helpers AttributesImpl, use its setAttribute
   * If the implementation is TagSoup's, use its setAttribute
   * Otherwise, convert it to a SAX Helpers implementation and use its setAttribute
   */
  public static Attributes clearAttribute(Attributes atts, int attNum) {
    if (atts instanceof com.learningobjects.tagsoup.AttributesImpl) {
      ((com.learningobjects.tagsoup.AttributesImpl)atts).setValue(attNum, "");
      return atts;
    } else if (atts instanceof org.xml.sax.helpers.AttributesImpl) {
      ((org.xml.sax.helpers.AttributesImpl)atts).setValue(attNum, "");
      return atts;
    } else {
      org.xml.sax.helpers.AttributesImpl newatts = new org.xml.sax.helpers.AttributesImpl(atts);
      newatts.setValue(attNum, "");
      return newatts;
    }
  }

    /**
     * By default, when {@code <a href="https://www.google.com" target="_blank">} is clicked
     * the window.opener value is set for the new window which means the linked to page
     * can get access to the origin page via window.opener.
     *
     * To avoid this, the anchor tag is changed to be {@code <a href="https://www.google.com" target="_blank" rel="noopener">}
     * which causes browsers to not set window.opener for the the new window.
     *
     * @param atts the Attributes that might need modification
     * @param attName the attribute name
     */
    public static Attributes handleTargetAttribute(final Attributes atts, final String attName) {
        if(attName.equals("target")) {
            if (atts instanceof com.learningobjects.tagsoup.AttributesImpl) {
                ((com.learningobjects.tagsoup.AttributesImpl)atts).addAttribute("", "rel", "rel", "CDATA", "noopener");
                return atts;
            } else if(atts instanceof org.xml.sax.helpers.AttributesImpl) {
                ((org.xml.sax.helpers.AttributesImpl)atts).addAttribute("", "rel", "rel", "CDATA", "noopener");
                return atts;
            } else {
                org.xml.sax.helpers.AttributesImpl newatts = new org.xml.sax.helpers.AttributesImpl(atts);
                newatts.addAttribute("", "rel", "rel", "CDATA", "noopener");
                return newatts;
            }

        }
        return  atts;
    }
}
