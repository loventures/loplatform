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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.learningobjects.cpxp.component.annotation.StringConvert;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import scala.Option;
import scala.Some;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A list of elements representing a path in a standard format.  The path format is of 'element1/element2/'.  The
 * forward slash character is not valid in any element.  The elements of the path do not necessarily need to be item
 * ids.
 */
@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = Path.PathDeserializer.class)
@StringConvert(using = Path.PathStringConverter.class)
public class Path implements Serializable {
    private List<String> elements;

    private static final Path EMPTY = new Path(Collections.emptyList());

    // HORRID HACK; accept either %2F (the URL standard way) or ~2F (the loser angular way)
    public static final Pattern URL_ENCODED_SLASH = Pattern.compile("[%~]2f", Pattern.CASE_INSENSITIVE);

    /**
     * Returns an empty path.
     *
     * @return an empty path
     */
    public static Path empty() {
        return EMPTY;
    }

    /**
     * Creates a new path based on the formatted string.  The string may be empty.  If non-empty, the string must be in
     * the format of 'element1/element2/', and must end in a forward slash.
     *
     * @param path the string to parse
     */
    public Path(String path) {
        if (StringUtils.isEmpty(path)) {
            elements = Collections.emptyList();
        } else if (path.endsWith("/")) {
            String withoutLastSlash = path.substring(0, path.length() - 1);
            elements = Arrays.asList(withoutLastSlash.split("/"));
        } else {
            throw new IllegalArgumentException("Non-empty path does not end with a forward slash.");
        }
    }

    /**
     * Creates a path from a list of elements.  Elements may not contain a forward slash.
     *
     * @param elements the elements of the path
     */
    public Path(List<String> elements) {
        boolean hasForwardSlash = elements.stream().anyMatch(e -> e.contains("/"));
        if (hasForwardSlash) {
            throw new IllegalArgumentException("Elements cannot contain a forward slash.");
        }

        this.elements = new ArrayList<>(elements);
    }

    /**
     * Returns a new path resulting from appending the given element at the end of this path.
     *
     * @param element the new tail element
     * @return a new <code>Path</code> containing <code>element</code> at the tail
     */
    public Path append(String element) {
        List<String> newElements = new ArrayList<>(elements);
        newElements.add(element);
        return new Path(newElements);
    }

    /**
     * Returns a copy of the element list.
     *
     * @return a copy of the element list
     */
    public List<String> getElements() {
        return Collections.unmodifiableList(elements);
    }

    /**
     * Returns the first element of the path, without a forward slash.
     *
     * @return the first element of the path
     */
    public String getHead() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot get head of empty list");
        }

        return elements.get(0);
    }

    /**
     * Returns the last element of the path, without a forward slash.
     *
     * @return the last element of the path
     */
    public String getTail() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot get tail of empty list");
        }

        return elements.get(elements.size() - 1);
    }

    /**
     * Returns the root <code>Path</code> for this <code>Path</code>.  If the path is <code>empty</code>, this function
     * instead returns <code>null</code>. If this path is a root, this function returns the object itself.
     *
     * @return the root <code>Path</code> for this <code>Path</code>, or <code>Path.empty</code> if there are no
     * elements in the path, or <code>this</code> if the path itself a root
     */
    public Optional<Path> getRoot() {
        if (isEmpty()) {
            return Optional.empty();
        } else if (elements.size() == 1) {
            return Optional.of(this);
        }

        List<String> rootElement = Collections.singletonList(elements.get(0));
        return Optional.of(new Path(rootElement));
    }

    /**
     * Returns the <code>Path</code> without the tail element.  The parent <code>Path</code> of a single element
     * <code>Path</code> is <code>Path.empty</code>.
     *
     * @return the <code>Path</code> without the tail element, or <code>Path.empty</code> if there are no other elements
     * in the path
     */
    public Optional<Path> getParentPath() {
        if (elements.size() <= 1) {
            return Optional.empty();
        }

        List<String> newElements = new ArrayList<>(elements.subList(0, elements.size() - 1));
        return Optional.of(new Path(newElements));
    }

    /**
     * Returns whether this path is a subpath of the given <code>Path</code>.  If <code>p</code> is <code>null</code> or
     * <code>empty</code> this function returns false.  Equivalent paths are not considered subpaths.
     *
     * @param p the path to check against
     * @return whether this path is a subpath of the given <code>Path</code>
     */
    public boolean isSubpathOf(Path p) {
        if (p == null || p.isEmpty() || this.equals(p)) {
            return false;
        }

        return toString().startsWith(p.toString());
    }

    /**
     * Returns whether this path is a subpath or the same path as a given <code>Path</code>.
     *
     * @param p the path to check against
     * @return whether this path is a subpath or the same path as a given <code>Path</code>
     * @see Path#isSubpathOf(Path)
     */
    public boolean equalsOrIsSubpathOf(Path p) {
        return equals(p) || isSubpathOf(p);
    }

    /**
     * Expands this path into the path with all its ancestors.
     *
     * @return the set of the path with all its ancestors
     */
    public Set<Path> expandAncestorPaths() {
        Set<Path> paths = new LinkedHashSet<>();

        Path current = this;
        while (!current.isEmpty()) {
            paths.add(current);
            current = current.getParentPath().orElse(Path.empty());
        }

        return paths;
    }

    /**
     * Returns whether this path contains any elements.
     *
     * @return whether this path contains any elements
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Returns whether this path contains no elements.
     *
     * @return whether this path contains no elements
     */
    public boolean nonEmpty() {
        return !elements.isEmpty();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }

        String collected = elements.stream().collect(Collectors.joining("/"));
        return collected + "/";
    }

    // lookit me, I put these guys in matrix parameters because they're sooooooo
    // much better than query params, and also they never have encoding problems
    public String obscurate() {
        return elements.stream().collect(Collectors.joining("%2F")) + "%2F";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Path path = (Path) o;

        return new EqualsBuilder()
          .append(elements, path.elements)
          .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(49, 99)
          .append(elements)
          .toHashCode();
    }

    static class PathDeserializer extends JsonDeserializer<Path> {
        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String str = mapper.readValue(p, String.class);
            return fromEncodedString(str);
        }
    }

    // it's public for the framework, but don't go explicitly using it now
    public static class PathStringConverter implements StringConverter<Path> {
        @Override
        public Option<Path> apply(Raw<Path> v1) {
            try {
                return Some.apply(fromEncodedString(v1.value()));
            } catch(IllegalArgumentException ex) {
                return Option.empty();
            }
        }
    }

    public static Path fromEncodedString(String encoded) {
        String unencoded = URL_ENCODED_SLASH.matcher(encoded).replaceAll("/");
        return new Path(unencoded);
    }

    /**
     * A utility method for creating a path from a list of ids.
     *
     * @param ids the elements of the path
     * @return a <code>Path</code> of ids
     */
    public static Path from(List<Long> ids) {
        List<String> path = ids.stream()
                               .map(Object::toString)
                               .collect(Collectors.toList());

        return new Path(path);
    }

    public static List<Long> toIds(Path p) {
        return p.getElements().stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Returns all relative roots in a given set of <code>paths</code>.  If [01/02, 01/02/03, 02] is passed to this
     * function [01/02, 02] is returned since 01/02 is a relative root of 01/02/03.
     *
     * @param paths the paths to reduce
     * @return all paths that are relative roots in the set
     */
    public static Set<Path> reduceRoots(Collection<Path> paths) {
        // Removes dups
        Set<Path> candidates = new HashSet<>(paths);

        // For each path we are give
        for (Path current : paths) {
            // Check if our current path we are checking is a subpath of any of the candidates
            boolean currentHaveASubpath = candidates.stream()
                                                    .anyMatch(current::isSubpathOf);

            if (currentHaveASubpath) {
                // If it is a subpath, it cannot be a relative root
                candidates.remove(current);
            }
        }

        return candidates;
    }

    /**
     * Returns all relative leaves in a given set of <code>paths</code>.  If [01/02, 01/02/03, 02] is passed to this
     * function [01/02/03, 02] is returned since 01/02 is a relative leaf of 01/02/03.
     *
     * @param paths the paths to reduce
     * @return all paths that are relative leaves in the set
     */
    public static Set<Path> reduceLeaves(Collection<Path> paths) {
        // Removes dups
        Set<Path> candidates = new HashSet<>(paths);

        // For each path we are give
        for (Path current : paths) {
            // Check if our current path we are checking is a subpath of any of the candidates
            boolean currentHaveASubpath = candidates.stream()
                                                    .anyMatch(candidate -> candidate.isSubpathOf(current));

            if (currentHaveASubpath) {
                // If it is a subpath, it cannot be a relative root
                candidates.remove(current);
            }
        }

        return candidates;
    }

    public static Collector<String, ?, Path> toPath() {
        return Collector.<String, List<String>, Path>of(
          ArrayList::new,
          List::add,
          ListUtils::union,
          Path::new
        );
    }
}
