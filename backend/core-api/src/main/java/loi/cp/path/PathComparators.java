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

package loi.cp.path;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A utility class to compare <code>Path</code>s.
 */
public final class PathComparators {
    /**
     * Builds a <code>Comparator</code> of <code>Path</code>s that casts all elements as <code>Long</code>s and compares
     * them numerically.  In the case of otherwise equivalent paths, the shorter path is considered 'less than' the
     * other path.
     *
     * @return a numeric based <code>Comparator</code>
     */
    public static Comparator<Path> buildBaseComparator() {
        Comparator<String> comparison = (lhs, rhs) -> {
            Long lhsNumber = Long.MIN_VALUE;
            try {
                lhsNumber = Long.valueOf(lhs);
            } catch (NumberFormatException ignored) {
            }

            Long rhsNumber = Long.MIN_VALUE;
            try {
                rhsNumber = Long.valueOf(rhs);
            } catch (NumberFormatException ignored) {
            }

            return Objects.compare(lhsNumber, rhsNumber, Long::compareTo);
        };

        return buildComparator(comparison);
    }

    /**
     * Builds a <code>Comparator</code> of <code>Path</code>s that uses the given element comparator as how to compare
     * individual elements.  In the case of otherwise equivalent paths, the shorter path is considered 'less than' the
     * other path.
     *
     * @param elementComparator a <code>Comparator</code> for individual elements
     * @return a <code>Comparator</code> based on the given element <code>Comparator</code>
     */
    public static Comparator<Path> buildComparator(Comparator<String> elementComparator) {
        return new ElementThenSizeComparator(elementComparator);
    }

    public static final Comparator<Path> LEXICOMPARATOR =
      buildComparator(String::compareToIgnoreCase);

    /**
     * A comparator of <code>Path</code>s that delegates to a given element comparator for individual elements.
     */
    private static class ElementThenSizeComparator implements Comparator<Path> {
        final Comparator<String> elementComparator;

        public ElementThenSizeComparator(Comparator<String> elementComparator) {
            this.elementComparator = elementComparator;
        }

        @Override
        public int compare(Path lhs, Path rhs) {
            List<String> lhsElements = lhs.getElements();
            List<String> rhsElements = rhs.getElements();

            for (int i = 0; i < Math.max(lhsElements.size(), rhsElements.size()); i++) {
                if (lhsElements.size() == i) {
                    // exhausted lhs, so it's smaller
                    return -1;
                } else if (rhsElements.size() == i) {
                    // exhausted rhs, so lhs is bigger
                    return 1;
                }

                String lhsElement = lhsElements.get(i);
                String rhsElement = rhsElements.get(i);
                int elementComparison = elementComparator.compare(lhsElement, rhsElement);
                if (elementComparison != 0) {
                    return elementComparison;
                }
            }

            // exhausted both, so they are equal
            return 0;
        }
    }
}
