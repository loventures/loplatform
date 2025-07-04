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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.component.element.ComponentElement;

import java.io.Writer;
import java.util.concurrent.Callable;

public interface HtmlWriter {
    Writer getWriter();

    void setAutoFlush(boolean autoFlush);

    boolean getAutoFlush();

    void setJsonEncoding(boolean json);

    boolean getJsonEncoding();

    void startElement(String name);

    void closeElement();

    void closeEndElement();

    void endElement(String name);

    boolean isElementEmpty();

    void startAttribute(String name);

    void callable(Callable<?> value);

    void write(Object value);

    void raw(Object value);

    int getCount();

    ComponentElement getElement();

    void emit(String text);

    void unwind(ComponentElement until);

    void finish();

    void close();

    void closeAttribute();

    enum State {
        Text, Attribute, AttributeValue, AttributeText;
    }
}
