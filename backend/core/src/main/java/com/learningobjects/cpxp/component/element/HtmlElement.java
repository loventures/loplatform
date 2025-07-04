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
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.concurrent.Callable;

public class HtmlElement extends AbstractComponentElement {
    @Override
    public void startTag() {
        getHtmlWriter().emit("<" + getName());
    }

    @Override
    public void attribute(String name, Object value) {
        if (value instanceof Callable) {
            try {
                ((Callable) value).call();
            } catch (Exception e) {
                throw wrapException(e);
            }
        } else if (value != null) {
            String string = value.toString();
            if (!"".equals(string)) {
                getHtmlWriter().emit(" " + name + "=\"" + StringEscapeUtils.escapeHtml4(string) + "\"");
            }
        }
    }

    private int _count = -1;

    @Override
    public void closeTag() {
        HtmlWriter writer = getHtmlWriter();
        writer.emit(">");
        _count = writer.getCount();
    }

    @Override
    public void endTag() {
        getHtmlWriter().emit("</" + getName() + ">");
    }

    @Override
    public void closeEndTag() {
        getHtmlWriter().emit(" />");
        _count = 0;
    }

    @Override
    public boolean isEmpty() {
        return _count == getHtmlWriter().getCount();
    }
    @Override
    public void close(ComponentElement until) {
        if (_count < 0) {
            closeTag();
        }
        endTag();
        super.close(until);
    }
}
