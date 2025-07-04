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

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.component.element.AbstractComponentElement;
import com.learningobjects.cpxp.component.element.ComponentElement;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;

public class BaseHtmlWriter implements HtmlWriter {

    private final Writer _writer;
    private ComponentElement _element;
    private State _state;

    public BaseHtmlWriter(Writer writer) {
        _writer = writer;
        _element = AbstractComponentElement.getRoot(this);
        _state = State.Text;
    }

    @Override
    public Writer getWriter() {
        return _writer;
    }

    private boolean _autoFlush;

    @Override
    public void setAutoFlush(boolean autoFlush) {
        _autoFlush = autoFlush;
    }

    @Override
    public boolean getAutoFlush() {
        return _autoFlush;
    }

    private boolean _json;

    @Override
    public void setJsonEncoding(boolean json) {
        _json = json;
    }

    @Override
    public boolean getJsonEncoding() {
        return _json;
    }

    @Override
    public void startElement(String name) {
        _element = _element.getChild(name);
        _element.startTag();
    }

    @Override
    public void closeElement() {
        closeAttribute();
        _element.closeTag();
    }

    @Override
    public void closeEndElement() {
        closeAttribute();
        ComponentElement element = _element;
        _element = _element.getParent();
        element.closeEndTag();
        if (_autoFlush) {
            try {
                _writer.flush();
            } catch (IOException ioe) {
                throw Throwables.propagate(ioe);
            }
        }
    }

    @Override
    public void endElement(String name) {
        if (!name.equals(_element.getName())) {
            throw new IllegalStateException("Illegal </" + name + "> after <" + _element.getName() + ">");
        }
        ComponentElement element = _element;
        _element = _element.getParent();
        element.endTag();
        if (_autoFlush) {
            try {
                _writer.flush();
            } catch (IOException ioe) {
                throw Throwables.propagate(ioe);
            }
        }
    }

    @Override
    public boolean isElementEmpty() {
        return _element.isEmpty();
    }

    private String _attributeName;
    private Object _attributeValueObject;
    private final StringBuilder _attributeValue = new StringBuilder();

    @Override
    public void startAttribute(String name) {
        closeAttribute();
        _attributeName = name;
        _attributeValueObject = null;
        _attributeValue.setLength(0);
        _state = State.Attribute;
    }

    @Override
    public void callable(final Callable<?> value) {
        write(value);
    }

    @Override
    public void write(Object value) {
        write(value, false);
    }

    @Override
    public void raw(Object value) {
        write(value, true);
    }

    private void write(Object value, boolean raw) {
        switch (_state)  {
          case Attribute:
              _attributeValueObject = value;
              _state = State.AttributeValue;
              break;

          case AttributeValue:
              if (_attributeValueObject != null) {
                  _attributeValue.append(toString(_attributeValueObject));
              }
              _state = State.AttributeText;
          case AttributeText:
              if (value != null) {
                  _attributeValue.append(toString(value));
              }
              break;

          default:
              _element.content(value, raw);
              break;
        }
    }

    private int _count;

    @Override
    public int getCount() {
        return _count;
    }

    @Override
    public ComponentElement getElement() {
        return _element;
    }

    @Override
    public void emit(String text) {
        if (text != null) {
            try {
                _count += text.length();
                _writer.write(text);
            } catch (IOException ioe) {
                throw Throwables.propagate(ioe);
            }
        }
    }

    @Override
    public void unwind(ComponentElement until) {
        if (_element != until) {
            _element.close(until);
            _element = until;
        }
    }

    @Override
    public void finish() {
        _element.close(null);
    }

    @Override
    public void close() {
        try {
            finish();
            _writer.flush();
            _writer.close();
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }

    @Override
    public void closeAttribute() {
        switch (_state) {
          case Attribute:
          case AttributeValue:
              _element.attribute(_attributeName, _attributeValueObject);
              break;

          case AttributeText:
              _element.attribute(_attributeName, _attributeValue.toString());
              break;
        }
        _state = State.Text;
    }

    public static String toString(Object o) {
        return (o != null) ? o.toString() : null;
    }
}
