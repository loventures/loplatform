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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.util.ImageUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;

public class Uploader implements Closeable {
    private final boolean singlepart;
    private final Multimap<String, String> _parameters = LinkedHashMultimap.create();
    private final Multimap<String, UploadInfo> _uploads = LinkedHashMultimap.create();

    private Uploader(boolean singlepart) {
        this.singlepart = singlepart;
    }

    public String getParameter(String name) {
        Collection<String> parameters = _parameters.get(name);
        return parameters.isEmpty() ? null : parameters.iterator().next();
    }

    public UploadInfo getUpload(String name) {
        Collection<UploadInfo> uploads = _uploads.get(singlepart ? RequestBody.SINGLE_PART_NAME : name);
        return uploads.isEmpty() ? null : uploads.iterator().next();
    }

    public Iterable<UploadInfo> getUploads() {
        return _uploads.values();
    }

    public void destroy() {
        for (UploadInfo upload : getUploads()) {
            upload.destroy();
        }
    }

    @Override
    public void close() {
        for (UploadInfo upload : getUploads()) {
            upload.close();
        }
    }

    private void parseSinglepart(HttpServletRequest request) throws Exception {
        String headerFileName = request.getHeader("X-Filename");
        String parameterFileName = request.getParameter("fileName");
        boolean delete = Boolean.parseBoolean(StringUtils.defaultIfEmpty(request.getHeader("X-File-Delete"), "true"));
        String fileName = StringUtils.defaultIfEmpty(headerFileName, parameterFileName);
        String contentType = request.getContentType();
        upload(fileName, contentType, request.getInputStream(), delete);
    }

    private void parseMultipart(HttpServletRequest request) throws Exception {
        for (Part part : request.getParts()) {
            if (part.getSubmittedFileName() == null) {
                String value;
                try (InputStream in = part.getInputStream()) {
                    value = IOUtils.toString(in, CharEncoding.UTF_8);
                }
                _parameters.put(part.getName(), value);
            } else {
                String fileName = part.getSubmittedFileName();
                String contentType = part.getContentType();
                try (InputStream in = part.getInputStream()) {
                    upload(fileName, contentType, in, true);
                }
            }
            part.delete();

        }
    }

    private void upload(String fileName, String contentType, InputStream in, boolean delete) throws Exception {
        // TODO: mime type handling is wonky because on download the mime type
        // will be derived from the filename..
        fileName = fixFileName(fileName);
        File tmpFile = File.createTempFile("upload", ".upl");
        tmpFile.deleteOnExit();
        UploadInfo upload = new UploadInfo(fileName, contentType, tmpFile, delete);
        _uploads.put(RequestBody.SINGLE_PART_NAME, upload);
        FileOutputStream out = FileUtils.openOutputStream(tmpFile);
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
        }
        populateDimensions(upload);
    }

    private String fixFileName(String fileName) {
        if (fileName != null) { // If there is a path (Opera), strip it
            int index0 = fileName.lastIndexOf('/');
            int index1 = fileName.lastIndexOf('\\');
            fileName = fileName.substring(1 + Math.max(index0, index1));
        }
        return StringUtils.defaultIfEmpty(fileName, "unknown");
    }

    public static Uploader parse(HttpServletRequest request) throws Exception {
        Uploader uploader = null;
        try {
            if (!StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/")) {
                uploader = new Uploader(true);
                uploader.parseSinglepart(request);
            } else {
                uploader = new Uploader(false);
                uploader.parseMultipart(request);
            }
            return uploader;
        } catch (Exception ex) {
            if (uploader != null) {
                uploader.destroy();
            }
            throw ex;
        }
    }

    public static void populateDimensions(UploadInfo upload) {
        if (StringUtils.startsWith(upload.getMimeType(), "image/") && !upload.getFileName().endsWith(".ico")) {
            try {
                ImageUtils.Dim dimensions = ImageUtils.getImageDimensions(upload.getFile());
                upload.setWidth(Long.valueOf(dimensions.getWidth()));
                upload.setHeight(Long.valueOf(dimensions.getHeight()));
            } catch (Exception ignored) {
            }
        }

    }
}
