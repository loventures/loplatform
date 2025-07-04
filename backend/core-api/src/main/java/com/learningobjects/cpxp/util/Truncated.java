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

public class Truncated {
    private final String _text;
    private final boolean _truncated;
    private final boolean _stripped;

    public Truncated(String text, boolean truncated, boolean stripped) {
        _text = text;
        _truncated = truncated;
        _stripped = stripped;
    }

    public String getText() {
        return _text;
    }

    public boolean isTruncated() {
        return _truncated;
    }

    public boolean isStripped() {
        return _stripped;
    }
}
