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

/**
 * A file handle. Used to manage files stored in the file cache.
 */
public class FileHandle implements FileRef, AutoCloseable {
    private File _file;
    private FileCache _cache;
    private boolean _temporary;
    private int _refs;
    private int _generation;
    private String _version;

    FileHandle(File file, FileCache cache, String version, boolean temporary) {
        _file = file;
        _cache = cache;
        _temporary = temporary;
        _version = version;
        _refs = 1;
        _generation = cache.getGeneration();
    }

    public String getVersion() {
        return _version;
    }

    public void setVersion(String version) {
        this._version = version;
    }

    void setFile(File file) {
        _file = file;
    }

    int getGeneration() {
        return _generation;
    }

    public File getFile() {
        return _file;
    }

    void setTemporary(boolean temporary) {
        _temporary = temporary;
    }

    // A temporary file handle should not be cached.
    public boolean isTemporary() {
        return _temporary;
    }

    public void recreate() {
        _cache.recreateFile(this);
    }

    public synchronized void ref() {
        if (_refs == 0) {
            throw new IllegalStateException("Reference file handle: " + this);
        }
        ++ _refs;
    }

    public synchronized void deref() {
        if (_refs == 0) { // unreferenced
            throw new IllegalStateException("Dereference file handle: " + this);
        } else if (-- _refs == 0) { // referenced and becomes unrefenced
            if (_generation != -1) { // already failed
                _cache.releaseFile(_file);
            }
        }
    }

    @Override
    public void close() {
        deref();
    }

    public boolean exists() {
        return _file.exists();
    }

    public void created() {
        _cache.fileCreated(this);
    }

    public void failed() {
        _cache.fileFailed(this);
        _generation = -1; // subsequent operations will have no effect
    }

    @Override
    protected void finalize() {
        while (_refs > 0) {
            deref();
        }
    }

    @Override
    public String toString() {
        return "FileHandle[" + _file + ", refs = " + _refs + "]";
    }
}
