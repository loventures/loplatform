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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.collections4.IteratorUtils;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

public class Uploads {
    private Uploads() {
    }

    // should be session synchronized but this may be safer

    public static synchronized UploadInfo consumeUpload(String guid) {
        UploadInfo info = retrieveUpload(guid);
        removeUpload(guid);
        return info;
    }

    public static synchronized UploadInfo consumeUpload(UploadedFile file) {
        UploadInfo info = consumeUpload(file.guid());
        if (file instanceof UploadedFile.Image) {
            info.setThumbnailSizes(CollectionConverters
              .asJava(((UploadedFile.Image) file).sizes()));
        }
        return info;
    }

    public static synchronized UploadInfo retrieveUpload(String guid) {
        if (StringUtils.isEmpty(guid)) {
            return null;
        } else if ("remove".equals(guid)) {
            return UploadInfo.REMOVE;
        }
        UploadState state = getUpload(guid);
        if (state == null) {
            throw /*logThrowing*/(new RuntimeException("Unknown upload: " + guid));
        }
        if (state.getUploadInfo() == null) {
            throw /*logThrowing*/(new RuntimeException("Invalid upload: " + guid));
        }
        return state.getUploadInfo();
    }

    public static synchronized String createUpload(String fileName, String guid) {
        if (StringUtils.isEmpty(guid)) {
            guid = UUID.randomUUID().toString();
        }

        HttpServletRequest request = getRequest();
        HttpSession session = request.getSession(false);
        UploadState uploadState = new UploadState(fileName);
        session.setAttribute("ug:Upload-" + guid, uploadState);
        return guid;
    }

    public static synchronized String createUpload(String fileName) {
        return createUpload(fileName, null);
    }

    public static synchronized String createUpload(UploadInfo upload) {
        String guid = createUpload(upload.getFileName());
        UploadState state = getUpload(guid);
        state.setUploadInfo(upload);
        return guid;
    }

    static synchronized UploadState getUpload(String guid) {
        // I put this as an attribute on the request so that if a transaction is retried
        // after an input has been consumed, it can still be retrieved. Ideally I'd do a
        // destroy once the request was complete but...
        UploadState upload = null;
        HttpServletRequest request = getRequest();
        if (request != null) {
            upload = (UploadState) getRequest().getAttribute("ug:Upload-" + guid);
        }
        if (upload == null) {
            HttpSession session = getSession();
            if (session != null) {
                upload = (UploadState) session.getAttribute("ug:Upload-" + guid);
                if (upload != null && request != null) {
                    request.setAttribute("ug:Upload-" + guid, upload);
                }
            }
        }
        return upload;
    }

    public static synchronized void removeUpload(String guid) {
        HttpSession session = getSession();
        if (session != null) {
            session.removeAttribute("ug:Upload-" + guid);
        }
    }

    private static HttpServletRequest getRequest() {
        return BaseWebContext.getContext().getRequest();
    }

    private static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getSession();
        } else { // for purposes of async stuff...
            return Current.getTypeSafe(HttpSession.class);
        }
    }

    public static class UploadInfoModule extends SimpleModule {
        public UploadInfoModule() {
            super("UploadInfoModule");
            addDeserializer(UploadInfo.class, new UploadInfoDeserializer());
        }
    }

    static class UploadInfoDeserializer extends StdDeserializer<UploadInfo> {
        UploadInfoDeserializer() {
            super(UploadInfo.class);
        }

        @Override
        public UploadInfo deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final JsonNode node = jp.readValueAsTree();
            final UploadInfo upload = Uploads.retrieveUpload(node.isTextual() ? node.textValue() : node.path("guid").textValue());
            final ArrayNode sizes = (ArrayNode) node.get("sizes");
            if (sizes != null) {
                upload.setThumbnailSizes(IteratorUtils.toList(sizes.elements()).stream().map(JsonNode::textValue).collect(Collectors.toList()));
            }
            return upload;
        }
    }
}
