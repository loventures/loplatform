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

import com.learningobjects.de.web.MediaType;

import java.util.HashSet;
import java.util.Set;

/**
 * MIME utils.
 */
public class MimeUtils {
    public static final String MIME_TYPE_APPLICATION_ZIP = "application/zip";
    public static final String MIME_TYPE_APPLICATION_X_GZIP = "application/x-gzip";
    public static final String MIME_TYPE_APPLICATION_JSON = "application/json";
    public static final String MIME_TYPE_APPLICATION_XML = "application/xml";
    public static final String MIME_TYPE_APPLICATION_UNKNOWN = "application/unknown";
    public static final String MIME_PREFIX_TEXT = "text/";
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    public static final String MIME_TYPE_TEXT_HTML = "text/html";
    public static final String MIME_TYPE_TEXT_XML = "text/xml";
    public static final String MIME_TYPE_TEXT_CSS = "text/css";
    public static final String MIME_TYPE_TEXT_JAVASCRIPT = "text/javascript";
    public static final String MIME_TYPE_TEXT_PDF = "text/pdf";
    public static final String MIME_TYPE_TEXT_CSV = "text/csv";
    public static final String MIME_TYPE_TEXT_CACHE_MANIFEST = "text/cache-manifest";
    public static final String MIME_TYPE_TEXT_EVENT_STREAM = "text/event-stream";
    public static final String MIME_TYPE_APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CHARSET_SUFFIX_UTF_8 = ";charset=utf-8";

    /** This returns true for MIME types that are safe to render directly from our
     * application for end-user uploaded content. */
    public static boolean isSafe(final String mimeType) {
        return SAFE_MIME_TYPES.contains(mimeType);

    }

    private static final Set<String> SAFE_MIME_TYPES = new HashSet<>();

    static {
        // DO NOT ALLOW SVG, IT IS AN XSS SECURITY HOLE
        // DO NOT ALLOW HTML, IT IS AN XSS SECURITY HOLE
        // DO NOT ADD RANDOM THINGS UNLESS THEY ARE KNOWN SAFE

        SAFE_MIME_TYPES.add(MediaType.IMAGE_BMP_VALUE);
        SAFE_MIME_TYPES.add(MediaType.IMAGE_GIF_VALUE);
        SAFE_MIME_TYPES.add(MediaType.IMAGE_JPEG_VALUE);
        SAFE_MIME_TYPES.add(MediaType.IMAGE_PNG_VALUE);
        SAFE_MIME_TYPES.add("image/tiff");
        SAFE_MIME_TYPES.add(MediaType.TEXT_PLAIN_VALUE);
        SAFE_MIME_TYPES.add(MediaType.TEXT_CSV_VALUE);
        SAFE_MIME_TYPES.add(MediaType.APPLICATION_PDF_VALUE);
        SAFE_MIME_TYPES.add(MediaType.APPLICATION_JSON_VALUE);
        SAFE_MIME_TYPES.add("video/mpeg");
        SAFE_MIME_TYPES.add("video/mp4");
        SAFE_MIME_TYPES.add("video/quicktime");
        SAFE_MIME_TYPES.add("video/webm");
        SAFE_MIME_TYPES.add("video/x-flv");
        SAFE_MIME_TYPES.add("audio/basic");
        SAFE_MIME_TYPES.add("audio/mpeg");
        SAFE_MIME_TYPES.add("audio/mpegurl");
        SAFE_MIME_TYPES.add("audio/x-ms-wma");
        SAFE_MIME_TYPES.add("audio/x-aiff");
        SAFE_MIME_TYPES.add("audio/x-wav");
        SAFE_MIME_TYPES.add("audio/wav");
        SAFE_MIME_TYPES.add("application/ogg");

        // NEVER ADD THESE!!!

        SAFE_MIME_TYPES.remove(MediaType.APPLICATION_SVG_VALUE);
        SAFE_MIME_TYPES.remove(MediaType.TEXT_HTML_VALUE);
    }
}
