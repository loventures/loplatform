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

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A simple map of temporary files. Allows for the creation of
 * multiple temporary files and their automatic deletion upon
 * VM exit, removal from the map or finalization of the map.
 * Not threadsafe.
 */
public class TempFileMap extends HashMap<String, File> implements AutoCloseable {
    private String _prefix;
    private String _suffix;
    private List<TempFileMap> _maps = new ArrayList<>();

    public TempFileMap(String prefix, String suffix) {
        this._prefix = prefix;
        this._suffix = suffix;
    }

    public String getFullName() {
        return _prefix+_suffix;
    }

    public String getPrefix() {
        return _prefix;
    }

    public InputStream getInputStream(String name) throws IOException {
        File file = super.get(name);
        if (file == null) {
            throw new FileNotFoundException(name);
        }
        return FileUtils.openInputStream(file);
    }

    public OutputStream createOutputStream(String name) throws IOException {
        if (containsKey(name)) {
            throw new IOException("File exists: " + name);
        }
        return FileUtils.openOutputStream(create(name));
    }

    public File create(String key) throws IOException {
        String filePrefix = StringUtils.left(FileUtils.cleanFilename(_prefix), 12);
        String fileSuffix = StringUtils.left(FileUtils.cleanFilename(_suffix),  4);
        try {
            File file = File.createTempFile(filePrefix, fileSuffix);
            file.deleteOnExit();
            super.put(key, file);
            return file;
        } catch (IOException ex) {
            throw new IOException("Temp file error: " + filePrefix + "/" + fileSuffix + " (" + _prefix + "/" + _suffix + ")", ex);
        }
    }

    public File remove(String key) {
        File file = super.remove(key);
        file.delete();
        return file;
    }

    public void clear() {
        values().forEach(File::delete);
        _maps.forEach(TempFileMap::clear);
        _maps.clear();
        super.clear();
    }

    @Override
    public void close() {
        clear();
    }

    protected void finalize() {
        clear();
    }

    public void importZip(InputStream in) throws IOException{
        String name = "";
        try {
            ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                name = entry.getName();
                if (!name.endsWith("/")) {
                    File file = create(name);
                    try (final OutputStream out = FileUtils.openOutputStream(file)) {
                        IOUtils.copy(zipIn, out);
                    } catch (IOException e){
                        throw new IOException("Error reading " + file, e);
                    }
                }
            }
        } catch (IOException e){
            String msg = "Could not read ZIP file";
            if(!StringUtils.isBlank(name)){
                msg += ": " + name;
            }
            throw new IOException(msg, e);
        }
        finally {
            in.close();
        }
    }

    public void importZip(File zip) throws IOException {
        importZip(FileUtils.openInputStream(zip));
    }

    public TempFileMap newTempFileMap() {
        final TempFileMap map = new TempFileMap(_prefix + "-" + _maps.size(), _suffix);
        _maps.add(map);
        return map;
    }
}
