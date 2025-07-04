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

package com.learningobjects.cpxp.service.upgrade;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataWebService;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainFacade;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.dump.DumpService;
import com.learningobjects.cpxp.util.*;
import com.typesafe.config.Config;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class UpgradeServiceBean extends BasicServiceBean implements UpgradeService {
    private static final Logger logger = Logger.getLogger(UpgradeServiceBean.class.getName());

    public static final Long SYSTEM_INFO_ID = 0L;

    @Inject
    public DomainWebService _domainWebService;

    @Inject
    private DumpService _dumpService;

    @Inject
    private DataWebService _dataWebService;

    @Inject
    private Config config;

    public void initDomains() {
        if (!BaseServiceMeta.getServiceMeta().isDas()) {
            return;
        }

        logger.info("Init domains");

        final String serviceMetaVersion = BaseServiceMeta.getServiceMeta().getVersion();
        SystemInfo systemInfo = ClusterSupport.clusterSystemInfo();
        if (serviceMetaVersion.equals(systemInfo.getVersion())) {
            return;
        }

        systemInfo.setVersion(serviceMetaVersion);

        logger.log(Level.INFO, "Performing system upgrade");

        if (_domainWebService.findDomainByType(DomainConstants.DOMAIN_TYPE_STOCK) == null) {
            prepareDomain(DomainConstants.DOMAIN_TYPE_STOCK, "/zips/dump-domain.zip", "Dump", null);
        }

        if (_domainWebService.findDomainByType(DomainConstants.DOMAIN_TYPE_OVERLORD) == null) {
            logger.log(Level.INFO, "Creating overlord domain");
            var bootMap = OverlordBootstrap.overlordProps(config);
            prepareDomain(DomainConstants.DOMAIN_TYPE_OVERLORD, "/zips/dump-overlord.zip", "Overlord", bootMap);
        }
    }

    private void prepareDomain(String type, String zipFile, String messages, Map<String, String> bootMap) {
        Current.setTime(new Date());

        // find existing domain
        Long domain = _domainWebService.findDomainByType(type);

        if (domain != null) {
            // flag it for destruction
            _dataWebService.createString(domain, "type", "Delete");
            _dataWebService.createString(domain, "name", "Delete");
        }

        // create a new domain from dump.xml
        DomainFacade facade = _domainWebService.addDomain();
        facade.setUrl("/");
        domain = facade.getId();
        Locale locale = ClassUtils.parseLocale("en_US");
        FileHandle domainDump = ClassUtils.getLocalizedZipResourceAsTempFile(zipFile, locale, messages, bootMap);
        try {
            _dumpService.restoreReplace(domain, domainDump.getFile());
        } catch (Exception ex) {
            throw new RuntimeException("Domain creation error", ex);
        } finally {
            domainDump.deref();
        }
        _domainWebService.setDomainType(domain, type);
    }

    @Override
    public String acquireCentralHost(String myself) { // coupled with ServiceMeta
        Date now = new Date();
        var query = getEntityManager().createQuery("FROM SystemInfo WHERE id = :singleton AND (centralHost IS NULL OR centralHost = :myself OR centralHostTime < :then)", SystemInfo.class)
          .setParameter("singleton", ClusterSupport.singletonId())
          .setParameter("myself", myself)
          .setParameter("then", DateUtils.delta(-DateUtils.Unit.minute.getValue(5))) // 5 minutes ago
          .setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var systemInfo = query.getResultStream().findFirst().orElse(null);
        if (systemInfo != null) {
            // I have obtained a locked row to acquire or retain central host status
            systemInfo.setCentralHost(myself);
            systemInfo.setCentralHostTime(now);
        } else {
            // Find the existing row, if any
            systemInfo = getEntityManager().find(SystemInfo.class, ClusterSupport.singletonId());
            if (systemInfo == null) {
                // There was no initial row
                systemInfo = new SystemInfo();
                systemInfo.setId(ClusterSupport.singletonId());
                systemInfo.setCentralHost(myself);
                systemInfo.setCentralHostTime(now);
                // This is bad and all racy and crap. The initial row should be created during database setup.
                // If multiple servers stand up simultaneously, some will explode at this point.
                getEntityManager().persist(systemInfo);
                getEntityManager().flush();
                ManagedUtils.commit();
            }
        }
        return systemInfo.getCentralHost();
    }

    @Override
    public List<SystemInfo> findRecentHosts() {
        var query = getEntityManager().createQuery("FROM SystemInfo WHERE id != :singleton AND centralHostTime > :then AND clusterId = :clusterId", SystemInfo.class);
        query.setParameter("singleton", ClusterSupport.singletonId());
        query.setParameter("then", DateUtils.delta(-DateUtils.Unit.minute.getValue(5))); // 5 minutes ago
        query.setParameter("clusterId", ClusterSupport.clusterId());
        return query.getResultList();
    }

    @Override
    public void heartbeat(String myself, String name) {
        var query = getEntityManager().createQuery("FROM SystemInfo WHERE id != :singleton AND centralHost = :myself", SystemInfo.class)
          .setParameter("singleton", ClusterSupport.singletonId())
          .setParameter("myself", myself);
        var systemInfo = query.getResultStream().findFirst().orElse(null); // because they hate us
        if (systemInfo != null) {
            systemInfo.setCentralHostTime(new Date());
        } else {
            systemInfo = new SystemInfo();
            systemInfo.setId(EntityContext.generateId());
            systemInfo.setCentralHost(myself);
            systemInfo.setCentralHostTime(new Date());
            systemInfo.setNodeName(name);
            systemInfo.setClusterId(ClusterSupport.clusterId());
            getEntityManager().persist(systemInfo);
        }
    }

    @Override
    public void releaseCentralHost(String myself) { // coupled with ServiceMeta
        // If I set central host to null then another host will come along
        // immediately and become DAS. This means that during a system upgrade
        // the DAS will rotate through a bunch of servers, and a *booting*
        // server will *never* acquire DAS, so *bootstrap* things will
        // not run. So I leave myself as DAS and update the time so that I have
        // five minutes to restart and take back DAS.
        Query query = createQuery("UPDATE SystemInfo SET centralHostTime = :now WHERE centralHost = :myself");
        query.setParameter("myself", myself);
        query.setParameter("now", new Date());
        query.executeUpdate();
    }
}
