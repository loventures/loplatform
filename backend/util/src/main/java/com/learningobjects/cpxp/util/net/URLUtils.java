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

package com.learningobjects.cpxp.util.net;

import com.google.common.base.Throwables;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * This class contains utility methods for manipulating {@link URL} objects.
 */
public class URLUtils {

    /**
     * Safely URI-decode the given string with the UTF-8 charset.
     *
     * @param str the string to decode, may be null
     * @return the given string URI-decoded
     */
    public static String decode(@Nullable final String str) {

        if (str == null) {
            return null;
        }

        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // never going to happen; UTF-8 is supported
            throw Throwables.propagate(e);
        }
    }

    public static String encode(@Nullable final String str) {
        if (str == null) {
            return null;
        }

        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // never going to happen; UTF-8 is supported
            throw Throwables.propagate(e);
        }
    }


}
