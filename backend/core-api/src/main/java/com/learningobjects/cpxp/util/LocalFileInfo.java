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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File information.
 */
public final class LocalFileInfo extends FileInfo {

    private final File _file;

    public LocalFileInfo(@Nonnull File file) {
        this(file, null);
    }

    public LocalFileInfo(@Nonnull File file, Runnable onNoRefs) {
        super(onNoRefs);
        _file = file;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(_file);
    }

    public File getFile() {
        return _file;
    }

    @Override
    protected long getActualSize() {
        return _file.length();
    }

    @Override
    public boolean exists() {
        return _file.exists();
    }

    @Override
    protected long getActualMtime() {
        return _file.lastModified();
    }

    public String toString() {
        return "LocalFileInfo[" + _file + "]";
    }

    public static LocalFileInfo tempFileInfo() throws IOException {
        return LocalFileInfo.createTempFile("localfile", ".tmp");
    }

    public static LocalFileInfo createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        return new LocalFileInfo(file, file::delete);
    }
}
