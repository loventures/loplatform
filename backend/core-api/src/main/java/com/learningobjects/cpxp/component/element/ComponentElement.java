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

package com.learningobjects.cpxp.component.element;

import com.learningobjects.cpxp.component.HtmlWriter;

public interface ComponentElement {
    String getName();

    HtmlWriter getHtmlWriter();

    ComponentElement getParent();

    void startTag();

    void attribute(String name, Object value);

    void closeTag();

    void endTag();

    void closeEndTag();

    boolean isEmpty();

    void content(Object content, boolean raw);

    void close(ComponentElement until);

    ComponentElement getChild(String name);
}
