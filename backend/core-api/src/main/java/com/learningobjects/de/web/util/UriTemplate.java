/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.learningobjects.de.web.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a URI template. A URI template is a URI-like String that contains variables enclosed
 * by braces ({@code {}}), which can be expanded to produce an actual URI.
 *
 * Most of this class is copied from Spring Framework 4.0.1.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see <a href="http://bitworking.org/projects/URI-Templates/">URI Templates</a>
 */
@SuppressWarnings("serial")
public class UriTemplate implements Serializable {


    /**
     * A {@link Function} for getting the number of variables in the URI template
     */
    public static final Function<UriTemplate, Integer> GET_VARIABLE_NAMES_SIZE = new Function<UriTemplate, Integer>() {

        @Override
        public Integer apply(final UriTemplate template) {
            return template.getVariableNames().size();
        }
    };

    /**
     * A {@link Function} for {@link #getNumSegments()}
     */
    public static final Function<UriTemplate, Integer> GET_NUMBER_OF_SEGMENTS = new Function<UriTemplate, Integer>() {

        @Override
        public Integer apply(final UriTemplate template) {
            return template.getNumSegments();
        }
    };

    /**
     * An {@link Ordering} of {@link UriTemplate} by the number of segments in the template.
     *
     * <p>The following URI templates are ordered by this {@link Ordering}</p>
     * <pre>
     *     foo
     *     foo/bar
     *     /foo
     *     foo/bar/baz
     *     foo/bar/baz/
     *     foo/bar/baz/qaz
     *
     * </pre>
     */
    public static final Ordering<UriTemplate> BY_NUMBER_OF_SEGMENTS = Ordering.natural().onResultOf
            (GET_NUMBER_OF_SEGMENTS);

    /**
     * An {@link Ordering} of {@link UriTemplate} by the template's explicitness. A URI template is more explicit if it
     * has fewer path variables in it.
     *
     * <p>The following URI templates are ordered by this {@link Ordering}</p>
     * <pre>
     *     foo/current
     *     foo/{fooId}
     *     foo/{fooId}/bar/current
     *     foo/{fooId}/bar/{barId}
     * </pre>
     */
    public static final Ordering<UriTemplate> BY_EXPLICITNESS = Ordering.natural().onResultOf(GET_VARIABLE_NAMES_SIZE);

    /**
     * A compound {@link Ordering} of the longest matching {@link UriTemplate} as defined by {@link #BY_NUMBER_OF_SEGMENTS} with {@link #BY_EXPLICITNESS} breaking ties.
     *
     * <p>The following URI templates are ordered by this {@link Ordering}</p>
     * <pre>
     *     foo/current/bar
     *     foo/{fooId}/bar
     *     foo/{fooId}
     *     foo/current
     *     foo
     * </pre>
     */
    public static final Ordering<UriTemplate> GREEDY_ORDERING = BY_NUMBER_OF_SEGMENTS.reverse().compound
            (BY_EXPLICITNESS);

    /** Captures URI template variable names. */
    private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    /** Replaces template variables in the URI template. */
    private static final String DEFAULT_VARIABLE_PATTERN = "([^/]*)";

    private final List<String> variableNames;

    private final Pattern matchPattern;

    private final String uriTemplate;

    private final int numSegments;

    /**
     * Construct a new {@code UriTemplate} with the given URI String.
     * @param uriTemplate the URI template string
     */
    public UriTemplate(String uriTemplate) {
        Parser parser = new Parser(uriTemplate);
        this.uriTemplate = uriTemplate;
        this.variableNames = parser.getVariableNames();
        this.matchPattern = parser.getMatchPattern();

        // count segments
        String s = this.uriTemplate;
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        numSegments = CharMatcher.anyOf("/").countIn(s);

    }


    /**
     * Return the names of the variables in the template, in order.
     * @return the template variable names
     */
    public List<String> getVariableNames() {
        return this.variableNames;
    }

    /**
     * Return the number of segments in the template.
     *
     * @return the number of segments in the template
     */
    public int getNumSegments() {
        return numSegments;
    }



    /**
     * Returns a URI formed by replacing the positional parameters with the specified values.
     * @param parameters The parameter values.
     * @return The resulting URI.
     */
    public String createURI(String... parameters) {
        int i = 0;
        StringBuffer sb = new StringBuffer();
        Matcher m = NAMES_PATTERN.matcher(this.uriTemplate);
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(parameters[i ++]));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Indicate whether the given URI matches this template.
     * @param uri the URI to match to
     * @return {@code true} if it matches; {@code false} otherwise
     */
    public boolean matches(String uri) {
        if (uri == null) {
            return false;
        }
        Matcher matcher = this.matchPattern.matcher(uri);
        return matcher.matches();
    }

    /**
     * Match the given URI to a map of variable values. Keys in the returned map are variable names,
     * values are variable values, as occurred in the given URI.
     * <p>Example:
     * <pre class="code">
     * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
     * System.out.println(template.match("http://example.com/hotels/1/bookings/42"));
     * </pre>
     * will print: <blockquote>{@code {hotel=1, booking=42}}</blockquote>
     * @param uri the URI to match to
     * @return a map of variable values
     */
    public Map<String, String> match(String uri) {
        assert uri != null : "'uri' must not be null";
        Map<String, String> result = new LinkedHashMap<String, String>(this.variableNames.size());
        Matcher matcher = this.matchPattern.matcher(uri);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = this.variableNames.get(i - 1);
                String value = matcher.group(i);
                result.put(name, value);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return this.uriTemplate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchPattern.pattern());
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean equals;

        if (this == obj) {
            equals = true;
        } else if (obj instanceof UriTemplate) {
            final UriTemplate that = (UriTemplate) obj;
            equals = Objects.equals(this.matchPattern.pattern(), that.matchPattern.pattern());
        } else {
            equals = false;
        }

        return equals;
    }

    /**
     * Static inner class to parse URI template strings into a matching regular expression.
     */
    private static class Parser {

        private final List<String> variableNames = new LinkedList<String>();

        private final StringBuilder patternBuilder = new StringBuilder();

        private Parser(String uriTemplate) {
            Matcher m = NAMES_PATTERN.matcher(uriTemplate);
            int end = 0;
            while (m.find()) {
                this.patternBuilder.append(quote(uriTemplate, end, m.start()));
                String match = m.group(1);
                int colonIdx = match.indexOf(':');
                if (colonIdx == -1) {
                    this.patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                    this.variableNames.add(match);
                }
                else {
                    if (colonIdx + 1 == match.length()) {
                        throw new IllegalArgumentException(
                                "No custom regular expression specified after ':' in \"" + match + "\"");
                    }
                    String variablePattern = match.substring(colonIdx + 1, match.length());
                    this.patternBuilder.append('(');
                    this.patternBuilder.append(variablePattern);
                    this.patternBuilder.append(')');
                    String variableName = match.substring(0, colonIdx);
                    this.variableNames.add(variableName);
                }
                end = m.end();
            }
            this.patternBuilder.append(quote(uriTemplate, end, uriTemplate.length()));
            int lastIdx = this.patternBuilder.length() - 1;
            if (lastIdx >= 0 && this.patternBuilder.charAt(lastIdx) == '/') {
                this.patternBuilder.deleteCharAt(lastIdx);
            }
            // append non-capturing group to match longer URIs
            this.patternBuilder.append("(?:/.*?)*");

        }

        private String quote(String fullPath, int start, int end) {
            if (start == end) {
                return "";
            }
            return Pattern.quote(fullPath.substring(start, end));
        }

        private List<String> getVariableNames() {
            return Collections.unmodifiableList(this.variableNames);
        }

        private Pattern getMatchPattern() {
            return Pattern.compile(this.patternBuilder.toString());
        }
    }

}
