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

package com.learningobjects.cpxp.controller.upload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.util.LocalFileInfo;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.web.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class UploadInfo implements AutoCloseable, Serializable {
    public static final UploadInfo REMOVE = new UploadInfo("remove", null, null, false);
    private static final Logger logger = Logger.getLogger(UploadInfo.class.getName());

    private String _fileName;
    private String _mimeType;
    private File _file;
    private Long _width;
    private Long _height;
    private List<String> _thumbnailSizes = Collections.emptyList();
    private boolean _delete;
    private int _ref = 1;

    public UploadInfo(String fileName, String mimeType, File file, boolean delete) {
        _fileName = fileName;
        _mimeType = mimeType;
        _file = file;
        _delete = delete;
    }

    @JsonProperty
    public String getFileName() {
        return _fileName;
    }
    public void setFileName(String fileName) {
        _fileName = fileName;
    }

    @JsonProperty
    public String getMimeType() {
        return _mimeType;
    }
    public void setMimeType(String mimeType) {
        _mimeType = mimeType;
    }

    @JsonProperty
    public Long getSize() {
        return _file.length();
    }

    public Optional<MediaType> getMediaType() {
        if (_mimeType == null) {
            return Optional.empty();
        }
        else {
            try {
                return Optional.of(MediaType.parseMediaType(_mimeType));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    @JsonIgnore
    public File getFile() {
        return _file;
    }

    @JsonProperty
    public Long getWidth() {
        return _width;
    }

    public void setWidth(Long width) {
        _width = width;
    }

    @JsonProperty
    public Long getHeight() {
        return _height;
    }

    public void setHeight(Long height) {
        _height = height;
    }

    /**
     * This property is used to track client-requested thumbnails.
     * @return the thumbnail sizes to generate
     */
    @JsonProperty
    public List<String> getThumbnailSizes() {
        return _thumbnailSizes;
    }

    public void setThumbnailSizes(final List<String> sizes) {
        _thumbnailSizes = sizes;
    }

    public synchronized void destroy() {
        if (_delete && (_file != null)) {
            _file.delete();
        }
        _file = null;
    }

    @Override
    public void close() {
        deref();
    }

    public synchronized void ref() {
        if (_delete) {
            _ref++;
        }
    }

    public synchronized void deref() {
        if (_delete) {
            if (_ref > 0) {
                _ref--;
                if (_ref == 0) {
                    destroy();
                }
            } else {
                logger.warning("Double-deref of upload: " + this);
            }
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        destroy();
    }

    public String toString() {
        return "UploadInfo[" + _fileName + "]";
    }

    public LocalFileInfo toLocalFileInfo() {
        return new LocalFileInfo(_file);
    }

    public static UploadInfo apply(File file) {
        return apply(file, file.getName(), false);
    }

    public static UploadInfo tempFile() throws IOException {
        return tempFile("upload.tmp");
    }

    public static UploadInfo tempFile(String fileName) throws IOException {
        return apply(File.createTempFile("upload", ".tmp"), fileName, true);
    }

    private static UploadInfo apply(File file, String fileName, boolean delete) {
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        return new UploadInfo(fileName, StringUtils.defaultIfEmpty(mimeType, "application/unknown"), file, delete);
    }
}

