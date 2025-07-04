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

import com.learningobjects.cpxp.service.attachment.Disposition;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class FileInfo {
    private final Runnable _onNoRefs;
    private int _refs;
    private Date _lastModified;
    private String _contentType;
    private String _disposition;
    private boolean _isLocalized;
    private boolean _doCache;
    private boolean _noRedirect;
    private long _size;
    private long _expires;
    private List<Path> _dependencies = Collections.emptyList();
    private Map<String, String> _headers = Collections.emptyMap();
    private String _cachePath;
    private boolean _eternal;

    public abstract InputStream openInputStream() throws IOException;
    public abstract boolean exists();
    protected abstract long getActualSize();
    protected abstract long getActualMtime();

    protected FileInfo(Runnable onNoRefs) {
        _onNoRefs = onNoRefs;
        _refs = 1;
        _size = -1;
    }

    public synchronized void ref() {
        if (_refs == 0) {
            throw new IllegalStateException("Reference file info: " + this);
        }
        ++ _refs;
    }

    public synchronized void deref() {
        if (_refs <= 0) {
            throw new IllegalStateException("Dereference file info: " + this);
        }
        if (-- _refs == 0 && _onNoRefs != null) {
            _onNoRefs.run();
        }
    }

    public long getSize() {
        if (_size < 0) {
            _size = getActualSize();
        }
        return _size;
    }

    public void setLastModified(Date lastModified) {
        _lastModified = lastModified;
    }

    public Date getLastModified() {
        return _lastModified;
    }

    public void setContentType(String contentType) {
        _contentType = contentType;
    }

    public String getContentType() {
        return _contentType;
    }

    public void setDisposition(String disposition) {
        _disposition = disposition;
    }

    public void setDisposition(Disposition disposition, String filename) {
        _disposition = HttpUtils.getDisposition(disposition.name(), filename);
    }

    public String getDisposition() {
        return _disposition;
    }

    public void setIsLocalized(boolean isLocalized) {
        _isLocalized = isLocalized;
    }

    public boolean getIsLocalized() {
        return _isLocalized;
    }

    public void setDoCache(boolean doCache) {
        _doCache = doCache;
    }

    public boolean getDoCache() {
        return _doCache;
    }

    public void setNoRedirect(boolean noRedirect) {
        _noRedirect = noRedirect;
    }

    public boolean getNoRedirect() {
        return _noRedirect;
    }

    public void setExpires(long expires) {
        _expires = expires;
    }

    public long getExpires() {
        return _expires;
    }

    public void addDependency(File dependency) {
        addDependency(dependency.toPath());
    }
    public void addDependency(Path dependency) {
        if(_dependencies.isEmpty()) {
            _dependencies = new ArrayList<>();
        } else {
           _dependencies.add(dependency);
        }
    }

    public List<File> getDependencies() {
        return _dependencies.stream()
          .map(Path::toFile)
          .collect(Collectors.toList());
    }

    public List<Path> getPathDependencies() {
        return Collections.unmodifiableList(_dependencies);
    }

    public void addHeader(String key, String value) {
        if (_headers.isEmpty()) {
            _headers = new HashMap<String, String>();
        }
        _headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return _headers;
    }

    public boolean isStale() {
        Date lastModifiedDate = getLastModified();
        Long lastModified = (lastModifiedDate == null) ? 0L :
                lastModifiedDate.getTime();
        boolean stale = !exists();
        List<File> dependencies = getDependencies();
        if (dependencies.isEmpty()) {
            // if the file has no deps, then this is the file we care about. if it has deps
            // then this file was generated and so we need to ignore its modified time.
            stale |= getActualMtime() > lastModified;
        } else {
            for (File dependency : dependencies) {
                stale |= !dependency.exists() || (dependency.lastModified() > lastModified);
            }
        }
        return stale;
    }

    public void setCachePath(String cachePath) {
        _cachePath = cachePath;
    }

    public String getCachePath() {
        return _cachePath;
    }

    public void setEternal(boolean eternal) {
        _eternal = eternal;
    }

    public boolean isEternal() {
        return _eternal;
    }

    // If this file can be served directly by another service, returns a URL
    // for same.  If not, returns null.
    // method is GET, POST, etc.
    // expires is milliseconds-since-1971 (System.currentTimeMills etc.),
    //    but implementation may only operate at a resolution of seconds.
    // issecure, if non-null, overrides the http/https status of the redirect.
    public @Nullable String getDirectUrl(String method, long expires) {
        return null;
    }

    public boolean supportsDirectUrl() {
        return false;
    }

    @Override
    protected void finalize() {
        while (_refs > 0) {
            deref();
        }
    }
}
