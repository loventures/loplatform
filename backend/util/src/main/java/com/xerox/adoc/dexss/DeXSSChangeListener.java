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

/**
 * Objects implementing this interface are suitable for Property {@link DeXSSFilterPipeline#DeXSS_CHANGE_LISTENER}.
 * Useful mostly for debugging, or to log XSS events.
 *
 * TODO: An upgrade that accepts a SAX2 Location would be nice.
 */
public interface DeXSSChangeListener {
  /**
   * Called when a change happens but there is no other information.
   * @param message Main message
   */
  public void logXSSChange(String message);

  /**
   * Called when a change happens and there is one other informational item.
   * @param message Main message
   * @param item1 Information item
   */
  public void logXSSChange(String message, String item1);

  /**
   * Called when a change happens and there are two informational items.
   * @param message Main message
   * @param item1 Information item 1
   * @param item2 Information item 2
   */
  public void logXSSChange(String message, String item1, String item2);
}
