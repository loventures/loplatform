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

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ImageUtils {
    private static final Set<String> __imageSuffixes = new HashSet<String>();

    static {
        __imageSuffixes.add("gif");
        __imageSuffixes.add("jpg");
        __imageSuffixes.add("jpeg");
        __imageSuffixes.add("jpe");
        __imageSuffixes.add("png");
        __imageSuffixes.add("tif");
        __imageSuffixes.add("tiff");
        __imageSuffixes.add("bmp");
    }

    /**
     * Returns whether the filename's suffix (e.g. "gif") is one of the
     * standard supported image types. This is not strictly canonical,
     * but probably accurate.
     */
    public static boolean isImage(String filename) {
        if (filename == null) {
            return false;
        }
        String suffix = FilenameUtils.getExtension(filename);
        return __imageSuffixes.contains(suffix.toLowerCase());
    }

    public static Dim getImageDimensions(File file) throws IOException {
        if (file.getName().toLowerCase().endsWith(".ico")) { // hack
            throw new IOException("Ignoring badly supported .ico file");
        }
        ImageInputStream iis = new FileImageInputStream(file);
        return getDim(iis);
    }

    public static Dim getImageDimensions(InputStream inputStream, String fileName) throws IOException {
        if (fileName.toLowerCase().endsWith(".ico")) { // hack
            throw new IOException("Ignoring badly supported .ico file");
        }
        ImageInputStream iis = new FileCacheImageInputStream(inputStream, null);
        return getDim(iis);
    }

    private static Dim getDim(ImageInputStream iis) throws IOException {
        try {
            Iterator it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) {
                throw new IOException("Unsupported image type");
            }
            ImageReader reader = (ImageReader) it.next();
            synchronized (reader) {
                try {
                    reader.setInput(iis, true, true);
                    return new Dim(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } finally {
            try {
                iis.close();
            } catch (Exception ignored) {
            }
        }
    }

    // Instead of java.awt.Dimension because its getW/H return double
    public static class Dim implements Serializable {
        private int _width, _height;

        public Dim(int width, int height) {
            _width = width;
            _height = height;
        }

        public int getWidth() {
            return _width;
        }

        public int getHeight() {
            return _height;
        }

        public String toString() {
            return "[" + _width + ", " + _height + "]";
        }
    }
}
