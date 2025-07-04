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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    // Stop Jackson from closing the zip stream after writing one zip entry
    private static final ObjectMapper MAPPER =
      JacksonUtils.getMapper().copy().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    //TODO -- see where else I can leverage this
    public static ZipEntry findEntry(ZipFile zip, String pattern) {
        ZipEntry foundEntry = null;

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(pattern)) {
                foundEntry = entry;
                break;
            }
        }

        return foundEntry;
    }

    /**
     * Puts a new entry in the zip. Uses Jackson to write the value into the entry.
     * Primarily exists to ensure we use an ObjectMapper that does not close the zip
     *
     * @param zip zip that holds the new entry
     * @param name name of zip entry (aka full path of file)
     * @param value entry value, serialized with Jackson
     */
    public static void writeJsonZipEntry(final ZipOutputStream zip, final String name,
      final Object value) throws IOException {

        zip.putNextEntry(new ZipEntry(name));
        MAPPER.writeValue(zip, value);
        zip.closeEntry();

    }

}
