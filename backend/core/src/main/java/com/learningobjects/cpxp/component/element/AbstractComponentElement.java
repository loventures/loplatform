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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.component.util.Html;
import com.learningobjects.cpxp.component.util.HtmlOps;
import com.learningobjects.cpxp.component.util.ScriptToken;
import com.learningobjects.cpxp.util.JsonUtils;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.concurrent.Callable;

// Much of the HtmlWriter handling of tags could be hidden inside this class,
// but to what end...
public abstract class AbstractComponentElement implements ComponentElement {
    private String _name;
    private HtmlWriter _xmlWriter;
    private ComponentElement _parent;

    private void init(String name, HtmlWriter xmlWriter, ComponentElement parent) {
        _name = name;
        _xmlWriter = xmlWriter;
        _parent = parent;
        init();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public HtmlWriter getHtmlWriter() {
        return _xmlWriter;
    }

    @Override
    public ComponentElement getParent() {
        return _parent;
    }

    protected void init() {
    }

    @Override
    public void startTag() {
    }

    @Override
    public void attribute(String name, Object value) {
    }

    @Override
    public void closeTag() {
    }

    @Override
    public void endTag() {
    }

    @Override
    public void closeEndTag() {
        closeTag();
        endTag();
    }

    @Override
    public boolean isEmpty() {
        return (_parent != this) && _parent.isEmpty();
    }

    @Override
    public void content(Object value, boolean raw) {
        HtmlWriter writer = getHtmlWriter();
        // this has to actually render the html
        if (value instanceof Html) {
            HtmlOps.render((Html) value, getHtmlWriter());
        //} else if (value instanceof HtmlResponse) { // should never happen
        //    HtmlOps.render(((HtmlResponse<Html>) value).html(), getHtmlWriter());
        } else if (value instanceof Callable) {
            try {
                ((Callable) value).call();
            } catch (Exception e) {
                throw wrapException(e);
            }
        } else if (!raw) {
            if (writer.getJsonEncoding() && !(value instanceof ScriptToken)) {
                try {
                    writer.emit(JsonUtils.toJson(value)); // escapeHtml4 too?
                } catch (JsonProcessingException jpe) {
                    throw wrapException(jpe);
                }
            } else if (value != null) {
                writer.emit(StringEscapeUtils.escapeHtml4(value.toString()));
            }
        } else if (value != null) {
            writer.emit(value.toString());
        }
    }

    @Override
    public void close(ComponentElement until) {
        if ((_parent != this) && (_parent != until)) {
            _parent.close(until);
        }
    }

    @Override
    public ComponentElement getChild(String name) {
        AbstractComponentElement element;
        if (name.startsWith("@")) {
            element = new AttributeElement();
        } else if (StringUtils.contains(name, ":")) {
            element = new TagElement();
        } else {
            element = new HtmlElement();
        }
        element.init(name, _xmlWriter, this);
        return element;
    }

    public static ComponentElement getRoot(HtmlWriter writer) {
        AbstractComponentElement element = new RootElement();
        try {
            element.init("", writer, element);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return element;
    }

    private static class RootElement extends AbstractComponentElement {
    }

    protected final RuntimeException wrapException(Exception ex) {
        throw new RuntimeException("While expanding <" + _name + ">", ex);
    }
}
