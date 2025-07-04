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

import com.learningobjects.cpxp.service.attachment.AttachmentProvider;
import com.learningobjects.de.web.MediaType;
import org.jclouds.blobstore.domain.Blob;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * File information.
 */
public final class BlobInfo extends FileInfo {

    private Blob _blob;
    private final AttachmentProvider _provider;
    private final String _niceName;
    private final String _blobName;
    private final long _size;

    public BlobInfo(@Nonnull Blob blob, @Nonnull
    AttachmentProvider provider,
                    @Nonnull String niceName, @Nonnull String blobName, long size) {
        this(blob, provider, niceName, blobName, size, null);
    }

    public BlobInfo(@Nonnull Blob blob, @Nonnull AttachmentProvider provider,
                    @Nonnull String niceName, @Nonnull String blobName, long size, Runnable onNoRefs) {
        super(onNoRefs);
        _blob = blob;
        _provider = provider;
        _niceName = niceName;
        _blobName = blobName;
        _size = size;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return getBlob().getPayload().openStream();
    }

    public Blob getBlob() {
        if (_blob != null) {
            return _blob;
        }
        return _provider.getBlob(_blobName);
    }

    public String getBlobName() {
        return _blobName;
    }

    public String getNiceName() {
        return _niceName;
    }

    /**
     * Always returns the size asserted by the database, not the blob store. This is
     * not generally needed.
     */
    @Override
    protected long getActualSize() {
        /*
        return _provider.getBlobStore().blobMetadata(...).getContentMetadata().getContentLength();
        */
        return _size;
    }

    @Override
    public boolean exists() {
        return true;
    }

    /**
     * Unsupported. Always returns 0. This is only used to detect changes to local file
     * assets so I only need this on LocalFileInfo.
     */
    @Override
    protected long getActualMtime() {
        /*
        MutableBlobMetadata mbm = getBlob().getMetadata();
        Date when = mbm.getLastModified();
        // CJCH: the jclouds filesystem blobstore doesn't retrieve blob
        // metadata from the filesystem.  So it'll happily return null for
        // lastModified.  W00t.
        return when == null ? 0 : when.getTime();
        */
        return 0L;
    }

    @Override
    public boolean supportsDirectUrl() {
        return _provider.isS3();
    }

    @Override
    public String getDirectUrl(String method, long expires) {
        // I'm choosing to let use this get call here fail loudly its only use
        // is in the send file filter, which doesn't have any options to handle
        // the failure case
        String mimeType = ObjectUtils.defaultIfNull(getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return _provider.getDirectUrl(this, method, getDisposition(), _niceName,
          mimeType, expires).get();
    }

}
