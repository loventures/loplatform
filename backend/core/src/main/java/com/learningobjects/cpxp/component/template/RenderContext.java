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

package com.learningobjects.cpxp.component.template;

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.PropertyAccessorException;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.FormattingUtils;
import com.learningobjects.cpxp.util.JsonUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderContext {
    private final ComponentDescriptor _scope;
    private final Object _context;
    private final HtmlWriter _writer;
    private final Map<String, Object> _bindings = new HashMap<>();
    private final List<Pair<String, Object>> _stack = new ArrayList<>();
    private final String _url;

    public RenderContext(ComponentDescriptor scope, Object context, HtmlWriter writer, Map<String, ?> bindings, String url) {
        _scope = scope;
        _context = context;
        _writer = writer;
        _url = url;
        if (bindings != null) {
            _bindings.putAll(bindings);
        }
    }

    public ComponentDescriptor getScope() { return _scope; }

    public String getUrl() {
        return _url;
    }

    public Object getContext() {
        return _context;
    }

    public HtmlWriter getWriter() {
        return _writer;
    }

    public void renderTag(String ns, String name, Map<String, String> attributes) {
        // TODO
    }

    private static final Map<String, Object> UTILS = new HashMap<>();

    static {
        UTILS.put("ExceptionUtils", new ExceptionUtils());
        UTILS.put("ComponentUtils", new ComponentUtils());
        UTILS.put("FormattingUtils", new FormattingUtils());
        UTILS.put("JsonUtils", new JsonUtils());
        UTILS.put("DateUtils", new DateUtils());
    }

    public Object lookup(String key) throws PropertyAccessorException {
        if (_bindings.containsKey(key)) {
            return _bindings.get(key);
        } else if (Character.isUpperCase(key.charAt(0)) && Character.isLowerCase(key.charAt(1))) {
            if (UTILS.containsKey(key)) {
                return UTILS.get(key);
            }
            Object service = ComponentSupport.lookupService(key);
            if (service == null) {
                throw new RuntimeException("Unknown service: " + key);
            }
            return service;
        } else {
            return ComponentUtils.dereference0(_context, key);
        }
    }

    public Object lookupBinding(String key) {
        return _bindings.get(key);
    }

    private static final Object ABSENT = new Object();

    public void pushBinding(String key) {
        Object value = _bindings.containsKey(key) ? _bindings.get(key) : ABSENT;
        Pair<String, Object> pair = Pair.of(key, value);
        _stack.add(pair);
    }

    public void pushBinding(String key, Object value) {
        pushBinding(key);
        setBinding(key, value);
    }

    public void setBinding(String key, Object value) {
        _bindings.put(key, value);
    }

    public void setBindings(Map<String, Object> bindings) {
        _bindings.putAll(bindings);
    }

    public void popBinding(String key) {
        if (_stack.isEmpty()) {
            throw new IllegalStateException("Binding overrides empty");
        }
        Pair<String, Object> pair = _stack.remove(_stack.size() - 1);
        if ((key != null) && !key.equals(pair.getLeft())) {
            throw new IllegalStateException("Invalid binding pop: " + key + "/" + pair.getLeft());
        }
        key = pair.getLeft();
        if (pair.getRight() == ABSENT) {
            _bindings.remove(key);
        } else {
            _bindings.put(key, pair.getRight());
        }
    }
}
