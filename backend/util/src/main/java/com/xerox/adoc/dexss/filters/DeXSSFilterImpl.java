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

import org.xml.sax.*;
import org.xml.sax.helpers.XMLFilterImpl;
import com.xerox.adoc.dexss.*;

/**
 * Base class for XSS filters
 * Extends {@link XMLFilterImpl} and provides the methods for DeXSSChangeListener.
 */
public class DeXSSFilterImpl extends XMLFilterImpl implements XMLFilter {
  protected DeXSSChangeListener xssChangeListener;

  public DeXSSFilterImpl(DeXSSChangeListener xssChangeListener) {
    super();
    this.xssChangeListener = xssChangeListener;
  }

  /*
   * Permit re-use by resetting state on setParent
   */
  public void setParent(XMLReader r) {
    super.setParent(r);
  }

  public void setDeXSSChangeListener(DeXSSChangeListener xssChangeListener) {
    this.xssChangeListener = xssChangeListener;
  }

  public DeXSSChangeListener getXSSChangeListener() {
    return xssChangeListener;
  }

  protected void logXSSChange(String message) {
    xssChangeListener.logXSSChange(message);
  }

  protected void logXSSChange(String message, String item1) {
    if (xssChangeListener != null)
      xssChangeListener.logXSSChange(message, item1);
  }

  protected void logXSSChange(String message, String item1, String item2) {
    if (xssChangeListener != null)
      xssChangeListener.logXSSChange(message, item1, item2);
  }

}

