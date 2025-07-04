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

package com.learningobjects.cpxp.service.domain;

import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.attachment.AttachmentAccess;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.data.DataUtil;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.script.ScriptService;
import com.learningobjects.cpxp.service.session.SessionFacade;
import com.learningobjects.cpxp.service.session.SessionService;
import com.learningobjects.cpxp.service.session.SessionState;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.util.LogUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.learningobjects.cpxp.service.domain.DomainConstants.*;

/**
 * Domain web service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DomainWebServiceBean extends BasicServiceBean implements DomainWebService {
    private static final Logger logger = LoggerFactory.getLogger(DomainWebServiceBean.class.getName());

    /** The attachment service. */
    @Inject
    private AttachmentService _attachmentService;

    /** The data service. */
    @Inject
    private DataService _dataService;

    /** The facade service. */
    @Inject
    private FacadeService _facadeService;

    /** The name service. */
    @Inject
    private NameService _nameService;

    /** The script service. */
    @Inject
    private ScriptService _scriptService;

    /** The session service. */
    @Inject
    private SessionService _sessionService;

    /** The user web service. */
    @Inject
    private UserWebService _userWebService;

    public CurrentStatus checkCurrentStatus(CurrentInfo info, boolean extendSession) {

        CurrentStatus status = new CurrentStatus();

        SessionFacade session = _sessionService.lookupSession(info.getSessionId(), info.getIpAddress(), extendSession);
        Item domain;
        if (session == null) {
            String did = info.getDomainId();
            if (did != null) {
                logger.debug("Searching for domain, {}", did);
                domain = findDomainByDomainId(did);
                if (domain == null)
                    throw new RuntimeException("Unknown cross domain: " + did);
            } else {
                String host = info.getHostName();
                // Look up domain from request
                logger.debug("Searching for domain, {}", host);
                domain = getDomainByHost(host);
            }
            status.setSessionState(SessionState.Expired);
        } else {
            domain = _itemService.get(session.getParentId());
            status.setSessionPk(session.getId());
            status.setSessionState(session.getState());
        }
        if (domain != null) {
            final DomainDTO domainDTO = getDomainDTO(domain.getId());
            Current.setDomainDTO(domainDTO);
            status.setDomain(domainDTO);
            status.setAnonymousUser(_userWebService.getAnonymousUser());
            if ((session != null) && (session.getUser() != null) && SessionState.Okay.equals(session.getState())) {
                status.setUser(_userWebService.getUserDTO(session.getUser().getId()));
            }
            if ((status.getUser() == null) || Boolean.TRUE.equals(status.getUser().getDisabled())) {
                status.setUser(_userWebService.getUserDTO(status.getAnonymousUser()));
            } else {
                status.setRememberSession(Boolean.TRUE.equals(session.getRemember()));
            }
            status.setSecurityLevel(domainDTO.getSecurityLevel());
            UserType uType = status.getUser().getUserType();
            if (!UserType.Overlord.equals(uType)) {
                status.setLicenseRequired(Boolean.TRUE.equals(domainDTO.getLicenseRequired()) ||
                        (UserType.Guest.equals(uType) && (DataTransfer.getItemData(domain,DomainConstants.DATA_TYPE_TOS_FILE) != null))); // grr
            }
        }

        return status;
    }

    public PathStatus checkPathStatus(String path) {

        PathStatus status = new PathStatus();

        if (Current.getDomain() == null) {
            status.setNotFound(true);
            return status;
        }

        Item item;
        int index = path.length();
        do {
            while ((index > 1) && (path.charAt(index - 1) == '/')) { // skip trailing slashes
                --index;
            }
            if (index <= 1) {
                item = getCurrentDomain();
            } else {
                item = _nameService.getItem(path.substring(0, index));
            }
            if (item == null) {
                // if not found, then search for the nearest ancestor so
                // we can decide whether to issue an access denied or not found
                // error
                status.setNotFound(true);
                while ((index > 1) && (path.charAt(index - 1) != '/')) { // skip
                    // bar
                    --index;
                }
            }
        } while (item == null);

        status.setItem(item);

        return status;
    }

    public DomainFacade addDomain() {
        Item domain = _itemService.create(ITEM_TYPE_DOMAIN);
        domain.setRoot(domain);
        return getDomain(domain.getId());
    }

    public void removeDomain(Long id) {

        Item domain = _itemService.get(id);
        assertItemType(domain, ITEM_TYPE_DOMAIN);

        // _attachmentService.destroyDomainBinaries(domain); // keep these around for undo
        _itemService.deleteDomain(domain);
        invalidateQuery(INVALIDATION_KEY_HOST_NAME);

    }

    public DomainDTO getDomainDTO(Long id) {
        DomainFacade domain = getDomain(id);
        return (domain == null) ? null : DomainDTO.apply(domain);
    }

    public DomainFacade getDomain(Long id) {
        return _facadeService.getFacade(id, DomainFacade.class);
    }

    public Long getItemRoot(Long id) {
        Item item = _itemService.get(id);
        return getId(item.getRoot());
    }

    public Long getDomainIdByHost(String host) {

        Item domain = getDomainByHost(host);

        return getId(domain);
    }

    private Item getDomainByHost(String host) {
        // This could be done by looking through all domains and using regex
        // First look for exact domain
        Item item = getDomainByExactHost(host);
        if (item == null) {
            // Then look for wildcards: www.example.org -> *.example.org
            String prefix = StringUtils.substringBefore(host, ".");
            String wildHost = "*" + host.substring(prefix.length());
            item = getDomainByExactHost(wildHost);
            if (item == null) {
                item = getDomainByExactHost("*");
            }
        }
        return item;
    }

    public String getCurrentDomainPath() {
        return "/Domain";

        // Item folder = getDomainItemById(FOLDER_ID_DOMAIN);
        // return DataTransfer.getStringData(folder, DataTypes.DATA_TYPE_URL);
    }

    // These two methods are ugly hacks for expediency
    public void setDomainType(Long domain, String type) {

        Item item = _itemService.get(domain);
        _dataService.setString(item, DataTypes.DATA_TYPE_TYPE, type);

    }

    public Long findDomainByType(String type) {

        QueryBuilder qb = querySystem(DomainConstants.ITEM_TYPE_DOMAIN);
        qb.addCondition(DataTypes.DATA_TYPE_TYPE, "eq", type);
        Item domain = (Item) qb.getResult();

        return getId(domain);
    }

    public Item getDomainByExactHost(String host) {

        QueryBuilder qb = querySystem(ITEM_TYPE_DOMAIN);
        qb.addCondition(BaseCondition.getInstance(DataTypes.DATA_TYPE_HOST_NAME,
                "eq", host.toLowerCase(), "lower"));
        qb.addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE,
                "ne", DomainState.Deleted);
        qb.addInvalidationKey(INVALIDATION_KEY_HOST_NAME);
        Item item = (Item) qb.getResult();

        return item;
    }

    public void setImage(String type, String fileName, Long width,
            Long height, File file) {
        try {
            Item domain = getCurrentDomain();
            Item folder = getDomainItemById(FOLDER_ID_MEDIA);
            Item item = DataTransfer.getItemData(domain, type);
            List<Data> datas = new ArrayList<Data>();
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME,
                                    fileName, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas.add(DataUtil.getInstance(AttachmentConstants.DATA_TYPE_ATTACHMENT_WIDTH,
                    width, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas.add(DataUtil.getInstance(AttachmentConstants.DATA_TYPE_ATTACHMENT_HEIGHT,
                    height, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_DISPOSITION,
                                    "inline", BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
              .add(DataUtil
                .getInstance(
                  AttachmentConstants.DATA_TYPE_ATTACHMENT_ACCESS,
                  AttachmentAccess.Anonymous.name(), BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));

            if (item == null) {
                item = _attachmentService.createAttachment(folder, datas, file);
            } else {
                _attachmentService.updateAttachment(item, datas, file);
            }
            String path = _nameService.getPath(folder);
            if (DATA_TYPE_DOMAIN_CSS.equals(type)) {
                path = path + "/css";
                clearCssAttachments();
            }
            String pattern = getFilenameBindingPattern(path, fileName, "unknown");
            _nameService.setBindingPattern(item, pattern);
            _dataService.setItem(getCurrentDomain(), type, item);
            if (DATA_TYPE_DOMAIN_CSS.equals(type)) {
                _dataService.setItem(getCurrentDomain(),
                       DATA_TYPE_CSS_FILE, item);
            }
            if (DATA_TYPE_FAVICON.equals(type) && StringUtils.endsWithIgnoreCase(fileName, ".ico") && (file != null)) { // also update the intrinsic /favicon.ico
                Item favicon = getDomainItemById(ID_FAVICON);
                if (favicon != null) {
                    _attachmentService.storeAttachment(favicon, file);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error setting image", ex);
        }

    }

    @Override
    public void setCssZip(String fileName, File zipFile) {
        try {
            ZipFile zip = new ZipFile(zipFile);
            try {
                ZipEntry domainCSSEntry = null;
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (FilenameUtils.getName(entry.getName()).equals("domain.css")) {
                        domainCSSEntry = entry;
                        break;
                    }
                }
                if ((domainCSSEntry == null) || domainCSSEntry.isDirectory()) {
                    throw new RuntimeException("No domain.css entry");
                } else {
                    File tmpFile = File.createTempFile("css", ".css");
                    try {
                        InputStream in = zip.getInputStream(domainCSSEntry);
                        try {
                            OutputStream out = FileUtils.openOutputStream(tmpFile);
                            try {
                                IOUtils.copy(in, out);
                            } finally {
                                out.close();
                            }
                        } finally {
                            in.close();
                        }
                        setImage(DomainConstants.DATA_TYPE_DOMAIN_CSS, "domain.css", null, null, tmpFile);
                    } finally {
                        tmpFile.delete();
                    }
                }
                String name = domainCSSEntry.getName();
                String prefix = FilenameUtils.getPath(name);

                entries = zip.entries();
                clearCssAttachments();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String baseName = entry.getName();
                    String entryName = StringUtils.substringAfter(baseName, prefix);
                    if (!entry.isDirectory() && baseName.startsWith(prefix) && !entryName.equals("domain.css")) {
                        File tmpFile = File.createTempFile("css", ".att");
                        try {
                            InputStream in = zip.getInputStream(entry);
                            try {
                                OutputStream out = FileUtils.openOutputStream(tmpFile);
                                try {
                                    IOUtils.copy(in, out);
                                } finally {
                                    out.close();
                                }
                            } finally {
                                in.close();
                            }
                            addCssAttachment(entryName, tmpFile, false);
                        } finally {
                            tmpFile.delete();
                        }
                    }
                }
            } finally {
                zip.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Css zip error", ex);
        }
        addCssAttachment(fileName, zipFile, true);
    }

    private void clearCssAttachments() {

        Item domain = getCurrentDomain();
        Item css = DataTransfer.getItemData(domain,
               DATA_TYPE_DOMAIN_CSS);
        for (Item attachment : findByParentAndType(css,
               AttachmentConstants.ITEM_TYPE_ATTACHMENT)) {
            logger.debug("Removing CSS attachment, {}", attachment);
            _attachmentService.destroyAttachment(attachment);
        }

    }

    private void addCssAttachment(String filePath, File file, Boolean isZip) {

        try {
            String fileName = FilenameUtils.getName(filePath);
            String prefix = FilenameUtils.getFullPath(filePath);

            Item domain = getCurrentDomain();
            Item css = DataTransfer.getItemData(domain,
                   DATA_TYPE_DOMAIN_CSS);
            List<Data> datas = new ArrayList<Data>();
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME,
                                    fileName, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_DISPOSITION,
                                    "inline", BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
              .add(DataUtil
                .getInstance(
                  AttachmentConstants.DATA_TYPE_ATTACHMENT_ACCESS,
                  AttachmentAccess.Anonymous.name(), BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            Item item = _attachmentService.createAttachment(css, datas, file);
            String path = _nameService.getPath(css.getParent()) + "/css/"
                    + prefix;
            String pattern = getFilenameBindingPattern(path, fileName,
                    "unknown");
            _nameService.setBindingPattern(item, pattern);
            // just
            // want
            // "public"
            if (isZip) {
                _dataService.setItem(getCurrentDomain(),
                       DATA_TYPE_CSS_FILE, item);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error setting image", ex);
        }

    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void logStatistics() {
        SessionFactory factory = ManagedUtils.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = factory.getStatistics();
        LogUtils.prod(logger, "Hibernate session statistics: " + stats);
        for (String entity : stats.getEntityNames()) {
            EntityStatistics es = stats.getEntityStatistics(entity);
            if ((es != null) && (es.getFetchCount() > 1)) {
                LogUtils.prod(logger, "Entity fetch: " + entity + ": " + es.getFetchCount());
            }
        }
        stats.clear();
    }

    public String getAdministrationLink() {
        return "/Administration";
    }

    private Item findDomainByDomainId(String domainId) {
        QueryBuilder query = querySystem(DomainConstants.ITEM_TYPE_DOMAIN);
        query.addCondition(DomainConstants.DATA_TYPE_DOMAIN_ID, "eq", domainId);
        query.addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, "ne", DomainState.Deleted);
        return com.google.common.collect.Iterables.getFirst(query.getItems(), null);
    }

    public Long getDomainById(String domainId) {
        Item domain = findDomainByDomainId(domainId);
        return getId(domain);
    }

    public void setState(Long domainId, DomainState state, String message) {

        Item domain = _itemService.get(domainId);

        List<Data> datas = new ArrayList<Data>();
        datas.add(DataUtil.getInstance(DATA_TYPE_DOMAIN_STATE, state.name(), BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
        datas.add(DataUtil.getInstance(DATA_TYPE_DOMAIN_MESSAGE, message, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
        _dataService.setData(domain, datas);

    }

    @Override
    public ComponentEnvironment setupContext(Long domainId) {
        return setupContext(domainId, _userWebService.getRootUser(domainId));
    }

    @Override
    public ComponentEnvironment setupUserContext(Long userId) {
        Item user = _itemService.get(userId, UserConstants.ITEM_TYPE_USER);
        return setupContext(user.getRoot().getId(), user.getId());
    }

    ComponentEnvironment setupContext(Long domainId, Long userId) {
        DomainDTO dto = getDomainDTO(domainId);
        if (dto.getState() == DomainState.Deleted) {
            throw new IllegalStateException("Deleted domain: " + domainId);
        }
        Current.setTime(new Date());
        Current.setDomainDTO(dto);
        Current.setUserDTO(_userWebService.getUserDTO(userId));
        return _scriptService.initComponentEnvironment();
    }

    @Override
    public void invalidateHostnameCache() {
        invalidateQuery(DomainConstants.INVALIDATION_KEY_HOST_NAME);
    }
}
