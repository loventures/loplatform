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

/*
 * Repeater
 *
 * Support for recording and replay of calls to an object.  Alternative to
 * previous solution of looping on the input stream.  Will probably work with
 * any kind of class.  One issue is mutable arguments on method invocations
 * (which might have changed before a later re-invocation); user can supply an
 * InvocationArgumentProtector in order to deal with these.
 */

package com.learningobjects.cpxp.component.template;

import org.apache.commons.collections4.Closure;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class Repeater implements InvocationHandler {

    // The protect method replaces any mutable args with copies of themselves
    // in order to ensure that later repeat invocations won't get mutated
    // args...  protect() is guaranteed to only be invoked if args is non-null
    // and has at least one element.
    public interface InvocationArgumentProtector {
        void protect(Method m, Object[] args);
    }

    private Object                      _target;
    private InvocationArgumentProtector _argumentProtector;
    private List<Closure>               _invocations = new ArrayList<Closure>();
    private int                         _pos = 0;
    private boolean                     _posSet = false;

    public static Object newInstance(Object target, Class clas,
            InvocationArgumentProtector argumentProtector) {
        return Proxy.newProxyInstance(clas.getClassLoader(),
                clas.getInterfaces(), new Repeater(target, argumentProtector));
    }

    public static Object newInstance(Object target,
            InvocationArgumentProtector argumentProtector) {
        return newInstance(target, target.getClass(), argumentProtector);
    }

    private Repeater(Object target,
            InvocationArgumentProtector argumentProtector) {
        _target = target;
        _argumentProtector = argumentProtector;
    }

/*
    private String attributesToString(Attributes a) {
        String rv = "{";
        int len = a.getLength();
        for (int i = 0; i < len; ++i) {
            rv += (0 == i ? "" : ", ") + a.getQName(i) + "=\"" +
                    a.getValue(i) + "\"";
        }
        return rv + "}";
    }

    private String locatorToString(Locator l) {
        return "loc:" + l.getSystemId() + ":" + l.getLineNumber() + "." +
                l.getColumnNumber();
    }

    private String invocationToString(Method m, Object[] args) {
        String rv = m.getName() + "(";
        boolean first = true;
        if (args != null) {
            for (Object o: args) {
                String s = o == null ? "<null>" : o instanceof String ?
                        "\"" + o + "\"" : o instanceof char[] ? "char[]:\"" +
                        new String((char[]) o) + "\"" :
                        o instanceof Attributes ?
                        attributesToString((Attributes) o) :
                        o instanceof Locator ?
                        locatorToString((Locator) o) : o.toString();
                if (first) {
                    rv += s;
                    first = false;
                } else {
                    rv += ", " + s;
                }
            }
        }
        return rv + ")";
    }
*/

    public Object invoke(final Object proxy, final Method m,
            final Object[] args) {
        if (args != null && args.length > 0 && _argumentProtector != null) {
            _argumentProtector.protect(m, args);
        }
        _invocations.add(new Closure() {
            public void execute(Object input) {
                _posSet = false;
//              if (null != input) { // It's the indent to use...
//                  System.err.println(((String) input) + "  At " + _pos +
//                          ":" + invocationToString(m, args));
//              } else {
//                  System.err.println(_pos + ":" +
//                          invocationToString(m, args));
//              }
                try {
                    m.invoke(_target, args);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    throw new RuntimeException(
                        "InvocationTargetException on call replay: " +
                        t.getMessage(), t);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                        "IllegalAccessException on call replay: " +
                        e.getMessage(), e);
                } finally {
                    if (!_posSet) {
                        ++_pos;
                    }
                }
            }
        });
        int sz = _invocations.size();
        do {
            _invocations.get(_pos).execute(null);
        } while (_pos < sz);
        return null;
    }

//  String _indent = "";

    public void repeat(int pos, int end) {
        int oldPos = _pos;
        try {
//      String oldIndent = _indent;
            _pos = pos;
//      _indent += "  ";
//      System.err.println(oldIndent + "Repeating [" + pos + "," + end +
//              ") from " + _pos + ":");
            do {
//          _invocations.get(_pos).execute(oldIndent);
                _invocations.get(_pos).execute(null);
            } while (_pos < end);
//      System.err.println(oldIndent + "Done [" + pos + "," + end +
//              ") from " + _pos + ".");
//      _indent = oldIndent;
        } finally {
            _pos = oldPos;
        }
    }

    public int getPos() {
        return _pos;
    }

    public void setPos(int pos) {
        _pos = pos;
        _posSet = true;
    }

}
