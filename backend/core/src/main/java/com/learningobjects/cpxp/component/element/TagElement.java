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

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.component.TagInstance;
import com.learningobjects.cpxp.util.StringUtils;

public class TagElement extends AbstractComponentElement {
    private TagInstance _instance;
    private CustomTag _component;

    @Override
    protected void init() {
        String identifier = StringUtils.substringBefore(getName(), ":");
        String name = StringUtils.substringAfter(getName(), ":");
        if ("self".equals(identifier)) {
            throw new RuntimeException("Self namespace unsupported");
        }
        _instance = ComponentSupport.getTag(identifier, name);
        if (_instance == null) {
            throw new RuntimeException("Unknown tag: " + identifier + " / " + name);
        }
        if (_instance.getObject() instanceof CustomTag) {
            _component = (CustomTag) _instance.getObject();
        }
    }

    @Override
    public void attribute(String name, Object value) {
        if (_component != null) {
            _component.attribute(name, value);
        } else {
            _instance.setParameter(name, value);
        }
    }

    @Override
    public void content(Object value, boolean raw) {
        if (_component != null) {
            _component.content(value, raw);
        } else {
            _instance.setParameter("body", value);
        }
    }

    @Override
    public ComponentElement getChild(String name) {
        if (_component != null) {
            return _component.getChild(name);
        } else {
            return super.getChild(name);
        }
    }

    @Override
    public void endTag() {
        HtmlWriter writer = getHtmlWriter();
        ComponentElement until = writer.getElement();
        try {
            writer.write(_instance.invoke()); // hmm...
        } catch (Exception ex) {
            writer.unwind(until);
            throw wrapException(ex);
        }
    }
}
