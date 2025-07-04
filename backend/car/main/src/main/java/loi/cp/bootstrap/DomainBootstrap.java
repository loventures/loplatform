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

package loi.cp.bootstrap;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.domain.*;
import com.learningobjects.cpxp.service.dump.DumpService;
import com.learningobjects.cpxp.service.overlord.OverlordWebService;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.FileHandle;
import com.learningobjects.cpxp.util.ObjectUtils;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.cors.AmazonS3CorsService;
import scala.Option;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class DomainBootstrap extends AbstractComponent {
    private static final Logger logger = Logger.getLogger(DomainBootstrap.class.getName());
    @Inject
    private DomainWebService _domainWebService;

    @Inject
    private DumpService _dumpService;

    @Inject
    private UserWebService _userWebService;

    @Inject
    private AttachmentService _attachmentService;

    @Inject
    private OverlordWebService _overlordWebService;


    @Bootstrap("core.domain.delete")
    public void deleteDomain(JsonDomain domain) {
        logger.log(Level.INFO, "Delete domain {0}", domain.domainId);

        Long id = _domainWebService.getDomainById(domain.domainId);
        if (id == null) {
            logger.log(Level.INFO, "Domain not found");
            return;
        }
        _domainWebService.removeDomain(id);

        logger.log(Level.INFO, "Deleted");
    }

    @Bootstrap("core.domain.create")
    public void createDomain(JsonDomain domain) throws Exception {
        logger.log(Level.INFO, "Create domain");

        if (_domainWebService.getDomainById(domain.domainId) != null)
            throw new Exception("Duplicate domain id: " + domain.domainId);

        List<String> hostNames = new ArrayList<>();
        hostNames.add(domain.hostName);
        if (domain.additionalHostNames != null) {
            hostNames.addAll(domain.additionalHostNames);
        }
        for (String hostName : hostNames) {
            if (_domainWebService.getDomainByExactHost(hostName) != null) {
                throw new Exception("Duplicate hostname: " + hostName);
            }
        }

        DomainFacade d = _domainWebService.addDomain();
//        d.setUrl("/");
        d.setDomainId(domain.domainId);
        d.setName(domain.name);
        d.setShortName(domain.shortName);
        d.setPrimaryHostName(domain.hostName);
        d.setHostNames(hostNames);

        d.setLocale(StringUtils.defaultIfEmpty(domain.locale, "en_US"));
        d.setTimeZone(StringUtils.defaultIfEmpty(domain.timeZone, "US/Eastern"));
        d.setState(ObjectUtils.defaultIfNull(domain.state, DomainState.Normal));
        d.setSecurityLevel(ObjectUtils.defaultIfNull(domain.securityLevel, SecurityLevel.NoSecurity));

        d.setStartDate(DataSupport.MIN_TIME);
        d.setEndDate(DataSupport.MAX_TIME);

        DomainDTO globalDomain = Current.getDomainDTO();
        UserDTO globalUser = Current.getUserDTO();

        try {
            Current.setTime(new Date());
            Current.setDomainDTO(_domainWebService.getDomainDTO(d.getId()));

            logger.log(Level.INFO, "Import base content");

            Locale locale = ClassUtils.parseLocale(d.getLocale());
            try (FileHandle domainDump = ClassUtils.getLocalizedZipResourceAsTempFile("/zips/dump-domain.zip", locale, "Dump", null)) {
                _dumpService.restoreInto(d.getId(), "", domainDump.getFile());
            }

            Current.setUserDTO(_userWebService.getUserDTO(_userWebService.getRootUser()));

            logger.log(Level.INFO, "Upgrade domain");

            if (domain.favicon != null) {
                _domainWebService.setImage(DomainConstants.DATA_TYPE_FAVICON, domain.favicon.getFileName(), domain.favicon.getWidth(), domain.favicon.getHeight(), domain.favicon.getFile());
            }

            AmazonS3CorsService.updateS3CorsConfiguration(
              Option.apply(d),
              _overlordWebService,
              _attachmentService);
        } finally {
            Current.setDomainDTO(globalDomain);
            Current.setUserDTO(globalUser);
        }
    }

    @Bootstrap("core.domain.select")
    public void selectDomain(JsonDomain domain) throws Exception {
        logger.log(Level.INFO, "Select domain {0}", domain.domainId);

        Long id = _domainWebService.getDomainById(domain.domainId);
        if (id == null) {
            throw new Exception("Domain not found: " + domain.domainId);
        }
        Current.setTime(new Date());
        Current.setDomainDTO(_domainWebService.getDomainDTO(id));
        Current.setUserDTO(_userWebService.getUserDTO(_userWebService.getRootUser()));
    }

    @Bootstrap("core.domain.unsuspend")
    public void unsuspendDomain() throws Exception {
        _domainWebService.setState(Current.getDomain(), DomainState.Normal, null);
    }

    public static class JsonDomain {
        public String domainId;
        public String name;
        public String shortName;
        public String hostName;
        public List<String> additionalHostNames;
        public SecurityLevel securityLevel;
        public String locale;
        public String timeZone;
        public UploadInfo favicon;
        public DomainState state;
    }
}
