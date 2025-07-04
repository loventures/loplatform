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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

public class ZipOutputStream2 extends ZipOutputStream {
    private File _dir;
    private OutputStream _out;

    public ZipOutputStream2(OutputStream out) {
        super(out);
    }

    public void setDirectory(File dir) {
        _dir = dir;
    }

    @Override
    public void putNextEntry(ZipEntry entry) throws IOException {
        super.putNextEntry(entry);
        if (_dir != null) {
            File file = new File(_dir, entry.getName());
            if (entry.getName().endsWith("/")) {
                file.mkdirs();
            } else {
                _out = FileUtils.openOutputStream(file);
            }
        }
    }

    @Override
    public void closeEntry() throws IOException {
        super.closeEntry();
        if (_out != null) {
            _out.close();
            _out = null;
        }
    }

    @Override
    public void write(int x) throws IOException {
        byte[] a = new byte[] { (byte) x };
        this.write(a, 0, 1);
    }

    @Override
    public void write(byte[] x) throws IOException {
        this.write(x, 0, x.length);
    }

    @Override
    public void write(byte[] x, int a, int b) throws IOException {
        super.write(x, a, b);
        if (_out != null) {
            _out.write(x, a, b);
        }
    }
}
