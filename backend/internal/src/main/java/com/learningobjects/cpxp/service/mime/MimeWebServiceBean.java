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

package com.learningobjects.cpxp.service.mime;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.FileInfo;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.io.FilenameUtils;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.learningobjects.cpxp.service.mime.MimeConstants.ATTACHMENT_ID_MIME_TYPES;

/**
 * Mime web service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MimeWebServiceBean extends BasicServiceBean implements MimeWebService {
    private static final Logger logger = Logger.getLogger(MimeWebServiceBean.class.getName());
    @Inject
    private AttachmentService _attachmentService;

    @Inject
    private FacadeService _facadeService;

    public String getMimeType(String fileName) {

        // ACL: This should be OK
        String mimeTypeStr = null;
        if (fileName != null) {
            String suffix = FilenameUtils.getExtension(fileName);
            if (!StringUtils.isEmpty(suffix)) {
                mimeTypeStr = getMimeMap().getMimeType(suffix);
            }
        }

        return mimeTypeStr;
    }

    public String getSuffix(String mimeType) {

        // ACL: This should be OK
        String suffix = null;
        if (mimeType != null) {
            suffix = getMimeMap().getSuffix(mimeType);
        }

        return suffix;
    }

    private static final Map<Long, MimeMap> __mimeMaps = Collections.synchronizedMap(new HashMap<Long, MimeMap>());

    private MimeMap getMimeMap() {
        Item item = getMimeTypesItem();
        AttachmentFacade facade = _facadeService.getFacade(item, AttachmentFacade.class);
        MimeMap mimeMap = __mimeMaps.get(getCurrentDomain().getId());
        Long generation = NumberUtils.longValue(facade.getGeneration());
        if ((mimeMap == null) || !mimeMap.getVersion().equals(generation)) {
            Properties properties = new Properties();
            try {
                try (InputStream in = getClass().getResourceAsStream("/mime.types")) {
                    properties.load(in);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error parsing stock mime.types", ex);
            }
            FileInfo blob = _attachmentService.getAttachmentBlob(item, false);
            try {
                try (InputStream in = blob.openInputStream()) {
                    properties.load(in);
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error parsing domain mime.types", ex);
            }
            mimeMap = new MimeMap(generation);
            Enumeration mimeTypes = properties.keys();
            while (mimeTypes.hasMoreElements()) {
                String mimeType = (String) mimeTypes.nextElement();
                String suffixes = (String) properties.get(mimeType);
                for (String suffix: suffixes.split("[ ,]+")) {
                    if (!StringUtils.isEmpty(suffix)) {
                        mimeMap.put(suffix, mimeType);
                    }
                }
            }
            __mimeMaps.put(getCurrentDomain().getId(), mimeMap);
        }
        return mimeMap;
    }

    public AttachmentFacade getMimeTypes() {
        return _facadeService.getFacade(getMimeTypesItem(), AttachmentFacade.class);
    }

    public void setMimeTypes(File file) {

        Item item = getMimeTypesItem();
        Set<Data> datas = Collections.emptySet();
        _attachmentService.updateAttachment(item, datas, file);

    }

    private Item getMimeTypesItem() {

        Item item = getDomainItemById(ATTACHMENT_ID_MIME_TYPES);

        return item;
    }

    private static class MimeMap {
        private Long _version;
        private Map<String, String> _mimeMap;
        private Map<String, String> _suffixMap;

        public MimeMap(Long version) {
            _version = version;
            _mimeMap = new  HashMap<String, String>();
            _suffixMap = new  HashMap<String, String>();
        }

        public void put(String suffix, String mimeType) {
            suffix = suffix.toLowerCase();
            mimeType = mimeType.toLowerCase();
            _mimeMap.put(suffix, mimeType);
            if (!_suffixMap.containsKey(mimeType)) {
                _suffixMap.put(mimeType, suffix);
            }
        }

        public Long getVersion() {
            return _version;
        }

        public String getMimeType(String suffix) {
            return _mimeMap.get(suffix.toLowerCase());
        }

        public String getSuffix(String mimeType) {
            return _suffixMap.get(mimeType.toLowerCase());
        }
    }
}
