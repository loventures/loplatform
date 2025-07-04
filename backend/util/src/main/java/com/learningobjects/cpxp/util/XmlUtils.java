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

/**
 * Shared functions useful to the different areas of the system that take in and
 * produce XML, such as dump/restore and export.
 */
public class XmlUtils {

    /**
     * Strips Invalid XML characters per:
     * http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char
     */
    public static String clean(String in) {
        StringBuilder sb = new StringBuilder();
        for (char c : in.toCharArray()) {
            if (isValidCharacter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isValidCharacter(char c) {
        return (c == 0x9) || (c == 0xA) || (c == 0xD)
                || ((c >= 0x20) && (c <= 0xD7FF))
                || ((c >= 0xE000) && (c <= 0xFFFD))
                || ((c >= 0x10000) && (c <= 0x10FFFF));
    }
}
