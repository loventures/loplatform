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

package com.learningobjects.cpxp.util.collection;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An {@link Iterable} of a {@link Class}'s supertypes. The iteration order is a the breadth-first traversal order of
 * the given {@link Class}'s inheritance tree, with any superclass traversed before any interfaces.
 *
 * <p>For example:</p>
 * <pre>
 *     // the iteration order of A is A, B, C, D, E, F
 *     class A extends B implements C,D {}
 *     class B extends E implements F
 *     interface C {}
 *     interface D {}
 *     class E {}
 *     interface F{}
 *</pre>
 */
public class BreadthFirstSupertypeIterable implements Iterable<Class<?>> {

    private final Class<?> clazz;

    public BreadthFirstSupertypeIterable(final Class<?> clazz) {
        this.clazz = clazz;
    }

    public static Stream<Class<?>> from(final Class<?> clazz) {
        return StreamSupport.stream(new BreadthFirstSupertypeIterable(clazz).spliterator(), false);
    }

    @Override
    public Iterator<Class<?>> iterator() {
        return new BreadthFirstInterfaceIterator(clazz);
    }

    private static class BreadthFirstInterfaceIterator implements Iterator<Class<?>> {

        private final Queue<Class<?>> queue = new LinkedList<>();
        private final Set<Class<?>> visited = new HashSet<>();

        public BreadthFirstInterfaceIterator(final Class<?> clazz) {
            queue.add(clazz);
            visited.add(clazz);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public Class<?> next() {
            final Class<?> next = queue.poll();

            final Class<?> superClazz = next.getSuperclass();
            if (superClazz != null) { // Object's superclass is null
                queue(superClazz);
            }

            for (final Class<?> iface : next.getInterfaces()) {
                queue(iface);
            }
            return next;
        }

        private void queue(final Class<?> iface) {
            if (!visited.contains(iface)) {
                visited.add(iface);
                queue.add(iface);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
