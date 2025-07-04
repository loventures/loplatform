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

package com.learningobjects.cpxp.filter;

import com.google.common.net.HttpHeaders;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.util.*;
import com.typesafe.config.Config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * A filter that processes send-file requests. This implemenation is now able to
 * detect whether grizzly-sendfile is installed and apply the appropriate header
 * to hand off file serving to it.
 */
public class SendFileFilter extends AbstractFilter {
    private static final Logger logger = LoggerFactory.getLogger(SendFileFilter.class.getName());

    /** serial version UID */
    private static final long serialVersionUID = 1L;

    /** the send-file request attribute */
    public static final String REQUEST_ATTRIBUTE_SEND_FILE = "ug:sendFile";

    private final FileCache _fileCache; // This is where file are stored on the FS
    private final boolean _gzip;

    public SendFileFilter(Config config) {
        _fileCache = FileCache.getInstance();
        _gzip = config.getBoolean("com.learningobjects.cpxp.sendfile.gzip");
    }

    /**
     * Process send-file requests.
     */
    protected void filterImpl(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        chain.doFilter(request, response);
        final FileInfo fileInfo = (FileInfo) request
                .getAttribute(REQUEST_ATTRIBUTE_SEND_FILE);
        if ((fileInfo != null) && Boolean.TRUE.equals(request.getAttribute("cdn"))) { // my mutating your FileInfo is improper
            // For static files served via the CDN add an ACL header that permits them to be accessed from the primary domain URL.
            // http://stackoverflow.com/questions/5008944/how-to-add-an-access-control-allow-origin-header
            // TODO: This is a hack. It should really be handled in CdnFilter, except that this filter clears all established response headers.
            fileInfo.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"); // HttpUtils.getUrl(request, "") return http://elb.eleaq-002.... TODO FIXME
        }
        // Serve files
        if (fileInfo != null) {
            try {
                FileInfo gzipped = gzipResponse(request, response, fileInfo);
                try {
                    sendFile(request, response, fileInfo, gzipped);
                } finally {
                    if (gzipped != null) {
                        gzipped.deref();
                    }
                }
            } finally {
                fileInfo.deref();
            }
        }
    }

    private FileInfo gzipResponse(HttpServletRequest request,
            HttpServletResponse response, FileInfo fileInfo) {
        // I have to ignore the browser's support of gzip if -gz is in the URL because the
        // presence of -gz means I won't send Vary/Accept-Encoding so a proxy could cache
        // the uncompressed data at the compressed URL.
        boolean wantsCompression = request.getRequestURI().contains("-gz") || HttpUtils.supportsCompression(request);
        if (!doGzip() || !wantsCompression || (fileInfo.getCachePath() == null) || !isCompressable(fileInfo)) {
            return null;
        }
        FileHandle handle = _fileCache.getFile(fileInfo.getCachePath() + ".gz");
        try {
            if (handle.isTemporary()) {
                // Someone else is already gzipping this resource
                // will be deleted by the deref
                return null;
            }
            Date lastModifiedDate = fileInfo.getLastModified();
            long lastModified = (lastModifiedDate == null) ? System
                .currentTimeMillis() : lastModifiedDate.getTime();
            boolean stale = !handle.exists() || (lastModified > handle.getFile().lastModified());
            if (stale) {
                handle.recreate();
                try {
                    long then = System.currentTimeMillis();
                    InputStream in = fileInfo.openInputStream();
                    try {
                        OutputStream out = FileUtils.openOutputStream(handle.getFile());
                        try {
                            GZIPOutputStream gzip = new GZIPOutputStream(out);
                            IOUtils.copy(in, gzip);
                            gzip.finish();
                        } finally {
                            out.close();
                        }
                    } finally {
                        in.close();
                    }
                    long delta = System.currentTimeMillis() - then;
                    logger.info("Compressed file, {}, {}", fileInfo.getCachePath(), delta);
                    handle.created();
                } catch (Exception ex) {
                    handle.failed();
                    throw new RuntimeException("Gzip error", ex);
                }
            }
            handle.ref();
            return new LocalFileInfo(handle.getFile(), handle::deref);
        } finally {
            handle.deref();
        }
    }

    // text/css text/javascript etc.
    private boolean isCompressable(FileInfo fileInfo) {
        String contentType = fileInfo.getContentType();
        return StringUtils.startsWith(contentType, MimeUtils.MIME_PREFIX_TEXT) && !StringUtils.equals(contentType, MimeUtils.MIME_TYPE_TEXT_PDF);
    }

    private void sendFile(HttpServletRequest request,
            HttpServletResponse response, final FileInfo fileInfo,
            final FileInfo gzipped) throws IOException {
        FileInfo actualFileInfo = (gzipped != null) ? gzipped : fileInfo;
        LocalFileInfo localFileInfo = actualFileInfo instanceof LocalFileInfo ?
                (LocalFileInfo) actualFileInfo : null;
        if (localFileInfo != null && !localFileInfo.exists()) {
            throw new IllegalStateException("Missing send file: "
              + fileInfo);
        }
        logger.debug("Send file, {}, {}", fileInfo, gzipped);
        response.reset(); // Kill any existing headers
        // grizzly-sendfile handles last modified on its own
        Date lastModifiedDate = fileInfo.getLastModified();
        long now = System.currentTimeMillis();
        long lastModified = (lastModifiedDate == null) ? now :
                lastModifiedDate.getTime();
        // -1 on unset
        long ifModifiedSince = request
                .getDateHeader(HttpUtils.HTTP_HEADER_IF_MODIFIED_SINCE);

        // i crudely never say not-modified for direct urls because then
        // the browser will use a cached redirect url which will
        // eventually expire and become bad. it seems that for redirect
        // urls i need a custom set of headers so that if-modified/etc
        // reflect the redirect and not the original file, so a redirect
        // with a 1 minute signature would be cacheable by the browser
        // for 1 minute. ideally public files could have a longer
        // signature and so would sit in the browser longer. ho hum.
        // TODO. basically this defeats browser caching of attachments.

        boolean useDirectUrl = getUseDirectUrl(fileInfo, request);

        // because I have ms, headers don't
        if ((ifModifiedSince != -1) && ((lastModified - ifModifiedSince) < 1000L) && !useDirectUrl) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setContentLength(0);
            return;
        }

        setupHeaders(response, fileInfo, lastModified);

        if (HttpUtils.HTTP_REQUEST_METHOD_HEAD.equals(request.getMethod())) {
            return;
        }

        final long validity = 60L * 1000L; // 60s
        long expires = now + validity;
        // We can't just use the request's method here since sometimes (e.g.
        // Overlord's "Download Logs") we send a file in response to a POST.
        // Should always be GET anyway.
        String redirect = useDirectUrl ? fileInfo.getDirectUrl(
                HttpUtils.HTTP_REQUEST_METHOD_GET, expires) : null;
        if (redirect != null) {
            logger.debug("Redirecting to " + redirect);
            HttpUtils.setExpired(response); // We used to let the browser cache this redirect but we ran into browsers caching the redirect for too long
            response.sendRedirect(redirect);
            return;
        }

        if (gzipped != null) {
            String acceptEncoding = request.getHeader(HttpUtils.HTTP_HEADER_ACCEPT_ENCODING);
            String encoding = StringUtils.containsIgnoreCase(acceptEncoding, HttpUtils.ENCODING_X_GZIP) ? HttpUtils.ENCODING_X_GZIP : HttpUtils.ENCODING_GZIP;
            response.setHeader(HttpUtils.HTTP_HEADER_CONTENT_ENCODING, encoding);
            // Internet Explorer will not cache anything with a Vary header.
            // http://www.ilikespam.com/blog/internet-explorer-meets-the-vary-header
            if (!request.getRequestURI().contains("-gz")) {
                response.addHeader(HttpUtils.HTTP_HEADER_VARY, HttpUtils.HTTP_HEADER_ACCEPT_ENCODING);
            }
        }
        response.setContentLength((int) actualFileInfo.getSize());

        try(InputStream in = actualFileInfo.openInputStream()) {
            streamFile(response, in);
        }
    }

    private void streamFile(final HttpServletResponse response,
            final InputStream in) {
        try {
            OutputStream out = response.getOutputStream();
            try {
                    IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            // minor, quite likely a network error
            logger.debug("Error streaming file", ex);
        }
    }

    private void setupHeaders(HttpServletResponse response, FileInfo fileInfo, long lastModified) {
        response.setContentType(StringUtils.defaultString(fileInfo
                .getContentType(), MimeUtils.MIME_TYPE_APPLICATION_UNKNOWN));
        response.setDateHeader(HttpUtils.HTTP_HEADER_LAST_MODIFIED,
                lastModified);
        if (fileInfo.getDisposition() != null) {
            response.setHeader(HttpUtils.HTTP_HEADER_CONTENT_DISPOSITION,
                    fileInfo.getDisposition());
        }
        if (fileInfo.getIsLocalized()) {
            response.addHeader(HttpUtils.HTTP_HEADER_VARY,
                    HttpUtils.HTTP_HEADER_ACCEPT_LANGUAGE);
        }
        if (fileInfo.getExpires() > 0) {
            // Ought to use current time but this is outside current
            // filter..
            HttpUtils.setExpires(response, System.currentTimeMillis(), fileInfo
                    .getExpires());
        } else {
            HttpUtils.setExpired(response);
        }
        for (Map.Entry<String, String> entry : fileInfo.getHeaders().entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }

    private boolean doGzip() {
        return _gzip;
    }

    private boolean getUseDirectUrl(FileInfo fileInfo, HttpServletRequest request) {
        //IE8/SSL doesn't like direct direct urls unless you mess with the registry
        boolean msie7or8 = false;
        String userAgent = request.getHeader(HttpUtils.HTTP_HEADER_USER_AGENT);
        if ((userAgent != null) && ((userAgent.indexOf("MSIE 7") >= 0) || (userAgent.indexOf("MSIE 8") >= 0))) {
            msie7or8 = true;
        }
        boolean ssl = request.isSecure();
        if (msie7or8 && ssl) {
            return false;
        }
        // because Quicktime doesn't like Direct URLs. TODO: FIXME: KILLME: NONONONO.
        return fileInfo.supportsDirectUrl() && !fileInfo.getNoRedirect() && !StringUtils.endsWith(request.getPathInfo(), ".mov"); // Temporary hack
    }

    private static boolean __nio, __nioSet;

    public static boolean isNioAvailable() {
        if (!__nioSet) {
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                Set<ObjectName> names = mbs.queryNames(new ObjectName("*:type=Connector,port=" + BaseServiceMeta.getServiceMeta().getPort()),null);
                assert !names.isEmpty();
                ObjectName connector = names.toArray(new ObjectName[0])[0];
                String name = (String) mbs.getAttribute(connector, "protocolHandlerClassName");
                __nio = StringUtils.contains(name, "Nio") || StringUtils.contains(name, "DEProtocol");
            } catch (Exception ex) {
                throw new RuntimeException("Nio check error", ex);
            }
            __nioSet = true;
        }
        return __nio;
    }
}
