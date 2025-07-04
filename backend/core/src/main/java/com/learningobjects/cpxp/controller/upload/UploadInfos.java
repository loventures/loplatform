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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Companion utilities for {@code UploadInfo}.
 *
 * <p>NOTE: I wish UploadInfo validated its inputs.
 */
public enum UploadInfos {

    INSTANCE;

    /**
     * Utility for creating UploadInfo (and subsequently Attachments) from in memory sources.
     *
     * @param fileName name of upload
     * @param mediaType mediatype of upload
     * @param charSource text representation of upload
     * @return an UploadInfo that you can now add as an attachment
     * @see com.learningobjects.cpxp.controller.upload.UploadInfo
     * @see com.learningobjects.cpxp.service.attachment.AttachmentWebService#createAttachment(Long, UploadInfo)
     */
    public UploadInfo from(String fileName, MediaType mediaType, CharSource charSource) {
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(mediaType);
        Objects.requireNonNull(charSource);
        Preconditions.checkArgument(StringUtils.isNotBlank(fileName));

        final File file;
        try {
            file = File.createTempFile("UploadInfos", ".tmp");
            file.deleteOnExit();
            charSource.copyTo(Files.asCharSink(file, Charsets.UTF_8));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return new UploadInfo(fileName, mediaType.toString(), file, true);
    }
}
