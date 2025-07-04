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

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Properties;
import java.util.regex.Pattern;

public class FileUtils extends org.apache.commons.io.FileUtils {
    private static final Pattern NON_FILE_CHAR_RE = Pattern.compile("[ .]+$|[/<>:\"|?*\\\\]");
    private static final int BUFFER = 2048;

    /**
     * Strips all sequences of unsafe characters.
     */
    public static String cleanFilename(String name) {
        return NON_FILE_CHAR_RE.matcher(name).replaceAll("");
    }

    public static boolean isIllegalFilename(final String name) {
        return NON_FILE_CHAR_RE.matcher(name).matches();
    }

    public static Properties loadProperties(File propsFile) throws IOException {
        assert propsFile.exists() : "Cannot load properties from file.  File does not exist.";
        Properties props = new Properties();
        try (InputStream in = new BufferedInputStream(FileUtils.openInputStream(propsFile))) {
            props.load(in);
        }
        return props;
    }


    //the following code was found here:  http://www.java2s.com/Code/Java/I18N/Howtoautodetectafilesencoding.htm
    /**
     * Given a set of charsets to check, returns which charset  apply to the given file
     * @param f the file to pass in
     * @param charsets the charsets, example "UTF-8", etc
     * @return
     */
    public static Charset detectCharset(@Nonnull File f, String... charsets) {


        Charset charset = null;

        for (String charsetName : charsets) {
            charset = detectCharset(f, Charset.forName(charsetName));
            if (charset != null) {
                break;
            }
        }

        return charset;
    }

    private static Charset detectCharset(File f, Charset charset) {
        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));

            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();

            byte[] buffer = new byte[512];
            boolean identified = false;
            while ((input.read(buffer) != -1) && (!identified)) {
                identified = identify(buffer, decoder);
            }

            input.close();

            if (identified) {
                return charset;
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    private static boolean identify(byte[] bytes, CharsetDecoder decoder) {
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            return false;
        }
        return true;
    }

    public static void saveProperties(File propsFile, Properties properties) throws IOException {
        try (OutputStream out = FileUtils.openOutputStream(propsFile)) {
            properties.store(out, null);
        }
    }

}
