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

package com.learningobjects.cpxp.util;

import net.htmlparser.jericho.*;

import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.Stack;

/**
 * HTML utility methods.
 */
public class HtmlUtils {
    /**
     * Converts HTML content into plaintext. All HTML elements are stripped and
     * replaced by whitespace, all whitespace is normalized and then all entity
     * references are expanded. Really?
     */
    public static String toPlaintext(String string) {
        Source source = new Source(string);
        return source.getTextExtractor().toString();
    }

    /**
     * Truncates an HTML string to a given length.  Ensures all tags are closed after the cut.
     *
     * @param string the HTML source string
     * @param maxLength the desired text length after truncation
     *
     * @return the truncated HTML string.
     */
    public static Truncated truncate(String string, int maxLength) {
        return truncate(string, maxLength, false);
    }

    public static Truncated truncate(String string, int maxLength, boolean stripTags) {
        StringBuilder sb = new StringBuilder();
        boolean truncated = false, stripped = false;
        try {
            StreamedSource source = new StreamedSource(string);
            Iterator<Segment> iterator = source.iterator();
            Stack<StartTag> tags = new Stack<StartTag>();
            int lengthSoFar = 0; // the sb may contain additional markup..
            while (iterator.hasNext() && (lengthSoFar <= maxLength)) { // <= on purpose
                Segment segment = iterator.next();
                if (segment instanceof Tag) {
                    Tag tag = (Tag) segment;
                    String name = tag.getName();
                    if ("br".equals(name) || "div".equals(name) || "p".equals(name) || "li".equals(name)) {
                        if ((sb.length() > 0) && !Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                            sb.append(' ');
                            ++ lengthSoFar;
                        }
                        stripped = true; // meh
                    } else if (!stripTags && ("em".equals(name) || "strong".equals(name) || "i".equals(name) || "b".equals(name) || "a".equals(name))) {
                        if (tag instanceof StartTag) {
                            // TODO: only push it it's not an empty element tag???
                            tags.push((StartTag) tag);
                        } else if (tag instanceof EndTag) {
                            // TODO: double check that the parser won't give us misnested tags
                            StartTag start = tags.peek();
                            if (start.getName().equals(name)) {
                                tags.pop();
                            } // else the string contains improperly nested tags or a stray closing tag.  We'll try to close things up at the end.
                            // TODO: How does the streaming version of  the parser handle this invalidity?
                        }
                        // Documentation states this is faster than appending segment.toString().
                        CharBuffer charBuffer = source.getCurrentSegmentCharBuffer();
                        sb.append(charBuffer.array(), charBuffer.position(), charBuffer.length());
                    } else { // I stripped some markup..
                        stripped = true;
                    }
                } else if (segment instanceof CharacterReference) {
                    if (lengthSoFar >= maxLength) {
                        truncated = true;
                        break;
                    }
                    CharacterReference characterReference=(CharacterReference)segment;
                    characterReference.appendCharTo(sb);
                    ++ lengthSoFar;
                } else {
                    CharBuffer charBuffer = source.getCurrentSegmentCharBuffer();
                    char[] array = charBuffer.array();
                    int remaining = maxLength - lengthSoFar, length = charBuffer.length(), position = charBuffer.position();
                    // Try not to cutwords.
                    if (length <= remaining) {
                        sb.append(array, position, length);
                        lengthSoFar += length;
                    } else {
                        int use = remaining;
                        if (Character.isLetterOrDigit(array[position + use])) {
                            while ((use > 0) && (remaining - use < 16) && Character.isLetterOrDigit(array[position + use - 1])) {
                                -- use;
                            }
                            if (remaining - use >= 16) {
                                use = remaining;
                            }
                        }
                        sb.append(array, position, use);
                        truncated = true;
                        break;
                    }
                }
            }
            while ((sb.length() > 0) && isStrippable(sb.charAt(sb.length() - 1), truncated)) {
                sb.deleteCharAt(sb.length() - 1);
            }
            Iterator<StartTag> tagIterator = tags.iterator();
            while (tagIterator.hasNext()) {
                StartTag tag = tagIterator.next();
                if (!tag.isSyntacticalEmptyElementTag()) {
                    sb.append("</".concat(tag.getName()).concat(">"));
                }
            }
        } catch (Exception ignored) {
        }

        return new Truncated(sb.toString(), truncated, stripped);
    }

    private static boolean isStrippable(char c, boolean truncated) {
        // strip trailing whitespace and, if this is truncated, punctuation, because an ellipsis will be added..
        return Character.isWhitespace(c) || (truncated && ((c == '.') || (c == ',') || (c == ':') || (c == ';') || (c == '!') || (c == '?')));
    }

}
