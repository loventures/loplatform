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

package com.learningobjects.cpxp.component.util;

import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.lang.ProviderLike;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import scala.Product;

import java.util.*;

public abstract class ScriptIterator<T> {
    private final int _count;
    private int _index;
    private boolean _has;
    private T _next;
    private boolean _flag;

    protected ScriptIterator(int count) {
        _count = count;
        _index = -1;
    }

    public String getCss() {
        StringBuilder sb = new StringBuilder();
        if (isFirst()) {
            sb.append(" first");
        }
        if (isLast()) {
            sb.append(" last");
        }
        if (isOdd()) {
            sb.append(" odd");
        } else {
            sb.append(" even");
        }
        return sb.toString();
    }

    public boolean flag() {
        boolean old = _flag;
        _flag = true;
        return old;
    }

    private T getImpl(boolean consume) {
        if (!_has) {
            _next =  next();
            _has = true;
        }
        if (consume) {
            ++ _index;
            _has = false;
        }
        return _next;
    }

    public boolean hasNext() {
        return _index < _count - 1;
    }

    public T getNext() {
        if (_index >= _count -1) {
            throw new NoSuchElementException();
        }
        return getImpl(true);
    }

    public T getPeek() {
        return (_index >= _count -1) ? null : getImpl(false);
    }

    protected abstract T next();

    public int getIndex() {
        return _index;
    }

    public int getPosition() {
        return _index + 1;
    }

    public int getCount() {
        return _count;
    }

    public boolean isFirst() {
        return _index == 0;
    }

    public boolean isLast() {
        return _index == _count - 1;
    }

    public boolean isOdd() {
        return _index % 2 == 0;
    }

    public boolean isEven() {
        return _index % 2 == 1;
    }

    public static <T> ScriptIterator<T> getInstance(Object o) {
        if (o instanceof Collection) {
            return new CollectionIterator((Collection<?>) o);
        } else if (o instanceof Iterable) {
            return new CollectionIterator(IterableUtils.toList((Iterable<?>) o));
        } else if (o instanceof Iterator) {
            return new CollectionIterator(IteratorUtils.toList((Iterator<?>) o));
        } else if (o instanceof Enumeration) {
            return new CollectionIterator(Collections.list((Enumeration<?>) o));
        } else if (o instanceof Map) {
            return new CollectionIterator(((Map<?, ?>) o).entrySet());
        } else if (o instanceof String) {
            return new ArrayIterator(StringUtils.splitString((String) o));
        } else if (o instanceof scala.collection.Iterable) {
            return new ScalaIterator((scala.collection.Iterable) o);
        } else if (o instanceof Product) {
            return new ScalaIterator(((Product) o).productArity(), ((Product) o).productIterator());
        } else if (o != null && ProviderLike.isProviderLike(o.getClass())) {
            return getInstance(ProviderLike.gimme(o));
        } else {
            return new ArrayIterator((Object[]) o);
        }
    }

    // Don't turn a map into an iteration over its EntrySet.
    // If object isn't a collection or an array, iterate over it as a single
    // item.
    public static <T> ScriptIterator<T> getNonMapInstance(Object o) {
        if (o instanceof Collection) {
            return new CollectionIterator((Collection<?>) o);
        } else if (o instanceof Object[]) {
            return new ArrayIterator((Object[]) o);
        } else {
            return new ArrayIterator(new Object[] { o });
        }
    }
}
