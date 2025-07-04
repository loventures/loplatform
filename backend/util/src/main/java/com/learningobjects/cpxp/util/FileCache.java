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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// I don't bother with an orphan (unreleased, old) file handle sweeper
// because the GC should always release them, and that will call through
// to my release method and clean up.

// The only pathological case is if a file is left open in a bad state;
// I won't be able to delete it, so it will become uncreatable. But, as
// always, the GC should save the day.

/**
 * A file cache. Used to cache objects on the filesystem.
 */
public class FileCache  {
    private static final Logger logger = Logger.getLogger(FileCache.class.getName());

    private static FileCache __instance;

    public static synchronized FileCache getInstance() {
        if (__instance == null) {
            __instance = new FileCache();
        }
        return __instance;
    }

    private Map<String, CacheEntry> _cache;
    private Set<CacheEntry> _lru;
    private long _size;
    private File _cacheDir;
    private int _generation;
    private boolean _persist = false;
    private long _cacheAttachmentSize;

    private FileCache() {
        _cache = new HashMap<String, CacheEntry>();
        _lru = new LinkedHashSet<CacheEntry>();
        _size = 0;
    }

    public synchronized void configure(File cacheDir, Long attachmentSize) {
        logger.log(Level.FINE, "Initializing file cache");
        ++ _generation;
        _cache.clear();
        _lru.clear();
        _size = 0;
        _cacheDir = cacheDir;
        logger.log(Level.FINE, "FileCache.directory, {0}", _cacheDir);
        _cacheDir.mkdirs(); // make it
        _cacheAttachmentSize = attachmentSize;
    }

    public long getCacheAttachmentSize() {
        return _cacheAttachmentSize;
    }

    public File getCacheDir() {
        return _cacheDir;
    }

    // Used in case the cache is reinitialized, so that old file handles
    // can be ignored.
    int getGeneration() {
        return _generation;
    }

    // TODO: hit/miss statistics

    /** Get a file from the cache. */
    public final FileHandle getFile(String path) {
        return getFile(path, Optional.empty(), Optional.empty());
    }

    /** Get a file from the cache, allowing reuse of existing filesystem files post restart
     * if the file digest matches an expected value. */
    public synchronized FileHandle getFile(final String path, final Optional<String> md5, final Optional<String> version) {
        if (_cacheDir == null) {
            throw new RuntimeException("File cache uninitialized");
        }
        final String rawversion = version.orElse(null);
        final File file = new File(_cacheDir, path);
        CacheEntry entry = _cache.get(file.getPath());
        logger.log(Level.FINE, "Get file, {0}, {1}, {2}", new Object[]{path, file, entry});
        // the exists test is a workaround in case the cache is wiped
        if ((entry != null)
         && (entry.getState() == CacheState.ready)
         && entry.getFile().exists()
         && Objects.equals(entry.getVersion(), rawversion)
        ) {
            if (entry.ref() == 1) {
                _lru.remove(entry);
            }
        } else if ((entry == null) && file.exists() && md5.map(digest -> digestMatches(file, digest)).orElse(_persist)) {
            logger.log(Level.FINE, "Assuming already-present file is valid, {0}, {1}", new Object[]{path, file});
            entry = new CacheEntry(path, file, CacheState.ready, rawversion);
            _cache.put(file.getPath(), entry);

        } else {
            logger.log(Level.FINE, "Create file, {0}, {1}, {2}", new Object[]{path, file, entry});
            if ((entry == null) || ((entry.getState() == CacheState.ready) && (!entry.getFile().exists() || !Objects.equals(entry.getVersion(), rawversion)))) {
                if (entry != null) {
                    logger.log(Level.FINE, "Cache file disappeared, {0}", entry);
                }
                entry = new CacheEntry(path, file, CacheState.creating, rawversion);
            } else {
                // The file was still being created; so I'll create it anew
                // in a temporary file which will be purged when released.
                entry = getTempCacheEntry(path);
            }
            // set up the file that we'll create (temporary or not)
            final File entryFile = entry.getFile();
            _cache.put(entryFile.getPath(), entry);
            entryFile.getParentFile().mkdirs();
            entryFile.delete();
        }
        return new FileHandle(
          entry.getFile(),
          this,
          rawversion,
          entry.getState() == CacheState.temporary
        );
    }

    private static boolean digestMatches(File file, String md5) {
        try (InputStream in = FileUtils.openInputStream(file)) {
            return MessageDigest.isEqual(DigestUtils.md5(in), Hex.decodeHex(md5.toCharArray()));
        } catch (Exception ex) {
            return false;
        }
    }

    private CacheEntry getTempCacheEntry(String path) {
        String base = "tmp/" + path + ".";
        int index = 0;
        File file;
        do {
            path = base + index;
            file = new File(_cacheDir, path);
            ++ index;
        } while (_cache.containsKey(file.getPath()));
        return new CacheEntry(path, file, CacheState.temporary, null);
    }

    synchronized void releaseFile(File file) {
        CacheEntry entry = _cache.get(file.getPath());
        logger.log(Level.FINE, "Release file, {0}, {1}", new Object[]{file, entry});
        if (entry == null) {
            return;
        }

        if (entry.deref() == 0) {
            // Releasing a file that was still being created, or was
            // temporary, kills it
            if (entry.getState() != CacheState.ready) {
                logger.log(Level.FINE, "Remove file, {0}, {1}", new Object[]{file, entry});
                _cache.remove(file.getPath());
                file.delete();
            } else {
                _lru.add(entry);
            }
        }
    }

    private void checkGeneration(FileHandle handle) {
        if (handle.getGeneration() != _generation) {
            throw new IllegalStateException("Generational mismatch");
        }
    }

    synchronized void recreateFile(FileHandle handle) {
        checkGeneration(handle);
        File file = handle.getFile();
        CacheEntry entry = _cache.get(file.getPath());
        logger.log(Level.FINE, "Recreate file, {0}, {1}", new Object[]{file, entry});
        if (entry.getState() == CacheState.ready) { // otherwise recreate is fine
            if (entry.getRefCount() == 1) { // no one is using it so i can recreate in-place
                _size -= entry.getSize();
                entry.setSize(0);
                entry.setState(CacheState.creating);
                file.delete();
            } else { // is busy so switch to temp file
                entry.deref();
                entry = getTempCacheEntry(entry.getName());
                file = entry.getFile();
                _cache.put(file.getPath(), entry);
                handle.setFile(file);
                handle.setTemporary(true);
            }
        }
    }

    synchronized void fileCreated(FileHandle handle) {
        checkGeneration(handle);
        File file = handle.getFile();
        CacheEntry entry = _cache.get(file.getPath());
        logger.log(Level.FINE, "File created, {0}, {1}", new Object[]{file, entry});
        if (entry.getState() == CacheState.creating) {
            entry.setState(CacheState.ready);
            long size = file.length();
            entry.setSize(size);
            _size += size;
            logger.log(Level.FINE, "Cache statistics: Files: {0}, Size: {1}", new Object[]{_cache.size(), _size});
        } else if (entry.getState() != CacheState.temporary) {
            throw new RuntimeException("Invalid file created call: " + entry);
        }
    }

    /**
     * There's no need to release the handle after calling this method.
     */
    synchronized void fileFailed(FileHandle handle) {
        checkGeneration(handle);
        File file = handle.getFile();
        CacheEntry entry = _cache.get(file.getPath());
        logger.log(Level.FINE, "File failed, {0}, {1}", new Object[]{file, entry});
        if ((entry.getState() != CacheState.creating) &&
                (entry.getState() != CacheState.temporary)) {
            throw new RuntimeException("Invalid file failed call: " + entry);
        }
        _cache.remove(file.getPath());
        file.delete();
    }

    private static enum CacheState {
        creating, ready, temporary;
    }

    private static class CacheEntry {
        private String _name;
        private File _file;
        private CacheState _state;
        private int _refCount;
        private long _size;
        private String _version;

        public CacheEntry(String name, File file, CacheState state, String version) {
            _name = name;
            _file = file;
            _state = state;
            _version = version;
            _refCount = 1;
            _size = -1L;
        }

        public String getName() {
            return _name;
        }

        public File getFile() {
            return _file;
        }

        public CacheState getState() {
            return _state;
        }

        public void setState(CacheState state) {
            _state = state;
        }

        public int getRefCount() {
            return _refCount;
        }

        public int ref() {
            return ++ _refCount;
        }

        public int deref() {
            return -- _refCount;
        }

        public long getSize() {
            return _size;
        }

        public void setSize(long size) {
            _size = size;
        }

        public String getVersion() {
            return _version;
        }
    }
}
