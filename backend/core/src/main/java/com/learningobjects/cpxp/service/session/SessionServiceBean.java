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

package com.learningobjects.cpxp.service.session;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.tx.TransactionCompletion;
import jakarta.persistence.Query;
import org.apache.commons.lang3.tuple.MutablePair;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Session service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SessionServiceBean extends BasicServiceBean implements SessionService {
    private static final Logger logger = Logger.getLogger(SessionServiceBean.class.getName());

    @Inject
    private FacadeService _facadeService;

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    // TODO: With a WebContext I could look up the ip address
    // directly without having to pass it in...
    public SessionFacade lookupSession(String id, String ipAddress) {
        return lookupSession(id, ipAddress, true);
    }

    public SessionFacade lookupSession(String id, String ipAddress, boolean extendSession) {

        logger.log(Level.FINE, "Searching for session, {0}", id);
        SessionFacade session = findSession(id);

        Object[] params = new Object[]{(session != null) ? session.getState() : "not found"};
        logger.log(Level.FINE, "Session state", params);

        if ((session != null) && SessionState.Okay.equals(session.getState())) {
            if (session.getUser() == null) {
                session.setState(SessionState.Expired); // The user was deleted
            } else {
                long now = new Date().getTime();
                long sessionAge = now - getLastAccess(session).getTime();
                long sessionTimeout = getSessionTimeout(session.getParent());
                if (sessionAge >= sessionTimeout) {
                    boolean remember = Boolean.TRUE.equals(session.getRemember());
                    if (now >= session.getExpires().getTime()) { // expired
                        // If the session has expired, I never resurrect it since
                        // that would lead to inconsistent behaviour depending
                        // on when the session purger ran.
                        // If it has exceeded the session timeout but was remembered
                        // then I need to verify that resurrecting it would not
                        // throw the domain overlimit
                        logger.log(Level.FINE, "Session has timed out, {0}", id);
                        if (!remember) {
                            logger.log(Level.FINE, "Forgetting the session, {0}", id);
                            session = null;
                        } else if (!checkSessionLimit(session.getUser().getId(), session.getParent())) {
                            logger.log(Level.FINE, "Resurrecting the session would exceed the session limit, {0}", id);
                            session.setState(SessionState.Limited);
                        } else {
                            logger.log(Level.FINE, "Resurrecting the session, {0}", id);
                        }
                    }
                }

                // If I'm at all in danger of being purged then don't
                // rely on the asynchronous access update flush and
                // just immediately update the session.
                if (extendSession && (session != null) && SessionState.Okay.equals(session.getState()) && (sessionAge >= sessionTimeout - FLUSH_INTERVAL * 2)) {
                    logger.log(Level.FINE, "Recording immediate session access to prevent unintended purging, {0}", id);
                    // TODO: New TX???
                    recordAccess(session, ipAddress);
                }
            }
        }

        return session;
    }

    public SessionFacade openSession(Long user, boolean remember, String ipAddress) {

        logger.log(Level.FINE, "Opening a new session, {0}, {1}", new Object[]{user, ipAddress});

        // Domain admins bypass the session limit and the session eviction.
        // ACDBean doesn't support evaluating someone else's ACL right now
        // and this is called before current is set to the target user so I
        // have to do a crude role check, grmbl.

         // if current (log in as) or target user is an admin..
        boolean domainAdmin = false;
        Long adminRole = getId(findDomainItemById(EnrollmentWebService.ROLE_ADMINISTRATOR_NAME));
        for (RoleFacade role : _enrollmentWebService.getActiveUserRoles(getCurrentUser().getId(), getCurrentDomain().getId())) {
            domainAdmin |= role.getId().equals(adminRole);
        }
        for (RoleFacade role : _enrollmentWebService.getActiveUserRoles(user, getCurrentDomain().getId())) {
            domainAdmin |= role.getId().equals(adminRole);
        }

        SessionDomainFacade domain = _facadeService.getFacade(getCurrentDomain(), SessionDomainFacade.class);
        if (!domainAdmin && !checkSessionLimit(user, domain)) {
            return null;
        }

        SessionFacade session = domain.addSession();
        session.setSessionId(SessionSupport.getPersistentId());
        session.setCreated(getCurrentTime());
        session.setUser(user);
        session.setRemember(remember);
        session.setState(SessionState.Okay);
        session.setProperties("");

        recordAccess(session, ipAddress);

        if (!domainAdmin && (Long.MAX_VALUE != getSessionLimit(domain))) {
            for (SessionFacade existing : domain.findSessionsByUser(user)) {
                if (!existing.equals(session)) {
                    existing.setState(SessionState.Evicted);
                }
            }
        }

        return session;
    }

    @Override
    public void invalidateUserSessions(Long user, Optional<Long> exclude) {
        QueryBuilder qb = queryBuilder();
        qb.setCacheQuery(true); // force caching
        qb.setCacheNothing(false); // don't cache nothing
        qb.setItemType(SessionConstants.ITEM_TYPE_SESSION);
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_USER, "eq", user);
        exclude.ifPresent(id -> qb.addCondition(DataTypes.META_DATA_TYPE_ID, "ne", id));
        for (SessionFacade session : qb.getFacadeList(SessionFacade.class)) {
            logger.log(Level.INFO, "Evicting session {0}", session.getSessionId());
            session.setState(SessionState.Evicted);
        }
    }

    public void closeSession(String id) {

        logger.log(Level.FINE, "Closing session, {0}", id);

        SessionFacade session = findSession(id);
        if (session != null) {
            session.setLastAccess(Current.getTime());
            session.setState(SessionState.Closed);
        }

    }

    public void recordAccess(SessionFacade session, String ipAddress) {

        logger.log(Level.FINE, "Recording session access, {0}, {1}", new Object[]{session.getId(), ipAddress});
        updateSession(session, getCurrentTime(), ipAddress, BaseServiceMeta.getServiceMeta().getLocalHost());

    }

    private void updateSession(SessionFacade session, Date when, String ipAddress, String nodeName) {
        session.setLastAccess(when);
        long timeout = Boolean.TRUE.equals(session.getRemember()) ? getRememberTimeout(session.getParent()) : getSessionTimeout(session.getParent());
        session.setExpires(new Date(when.getTime() + timeout));
        session.setIpAddress(ipAddress);
        session.setNodeName(nodeName);
    }

    public long getSessionValidity(String sessionId) {
        SessionFacade session = findSession(sessionId);
        long now = System.currentTimeMillis();
        long expires = (session != null) ? session.getExpires().getTime() - now : -1L;
        return ((session == null) || (session.getUser() == null) || !SessionState.Okay.equals(session.getState())) ? -1L : expires;
    }

    // I use last access for these computations rather than expires because
    // if there's an old keepalive session I'll happily expire it to let
    // someone new in.

    public long getActiveSessionCount(Long domainId) {

        Item domainItem = _itemService.get(domainId);
        SessionDomainFacade domain = _facadeService.getFacade(domainItem, SessionDomainFacade.class);

        QueryBuilder qb = queryRoot(domainItem,SessionConstants.ITEM_TYPE_SESSION);
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_LAST_ACCESS, "gt", new Date(getCurrentTime().getTime() - getSessionTimeout(domain)));
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_STATE, "eq", SessionState.Okay);
        long count = NumberUtils.longValue(qb.getAggregateResult(Function.COUNT));

        logger.log(Level.FINE, "Active sessions, {0}", count);

        return count;
    }

    public long getActiveSessionCount() {

        QueryBuilder qb = querySystem(SessionConstants.ITEM_TYPE_SESSION);
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_LAST_ACCESS, "gt", new Date(System.currentTimeMillis() - SessionConstants.DEFAULT_SESSION_TIMEOUT));
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_STATE, "eq", SessionState.Okay);
        long count = NumberUtils.longValue(qb.getAggregateResult(Function.COUNT));

        logger.log(Level.FINE, "Active sessions, {0}", count);

        return count;
    }

    private boolean checkSessionLimit(Long user, SessionDomainFacade domain) {
        return true;/*

        // TODO: This query could be done as a facade query...
        QueryBuilder qb = queryRoot(_itemService.get(domain.getId()),SessionConstants.ITEM_TYPE_SESSION);
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_USER, "ne", user);
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_STATE, "eq", SessionState.Okay);
        qb.addCondition(SessionConstants.DATA_TYPE_SESSION_LAST_ACCESS, "gt", new Date(getCurrentTime().getTime() - getSessionTimeout(domain)));
        long count = NumberUtils.longValue(qb.getAggregateResult(Function.COUNT));

        return count < getSessionLimit(domain);*/
    }

    private static final String PROPERTIES_ENCODING = "8859_1";

    public void setProperties(String id, Properties properties) {

        SessionFacade session = findSession(id);
        if (session != null) {
            Properties p = new Properties();
            try {
                if (session.getProperties() != null) {
                    ByteArrayInputStream in = new ByteArrayInputStream(session.getProperties().getBytes(PROPERTIES_ENCODING));
                    p.load(in);
                }
                p.putAll(properties);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                p.store(out, null);
                session.setProperties(out.toString(PROPERTIES_ENCODING));
            } catch (IOException ex) {
                throw new RuntimeException("Properties error", ex);
            }
        }

    }

    public Properties getProperties(String id) {

        Properties p = null;
        SessionFacade session = findSession(id);
        if ((session != null) && (session.getProperties() != null)) {
            p = new Properties();
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(session.getProperties().getBytes(PROPERTIES_ENCODING));
                p.load(in);
            } catch (IOException ex) {
                throw new RuntimeException("Properties error", ex);
            }
        }

        return p;
    }

    public SessionFacade findSession(String id) {

        SessionFacade facade = null;
        if ((id != null) && SessionSupport.isPersistentId(id)) {
            QueryBuilder qb = queryBuilder();
            qb.setCacheQuery(true); // force caching
            qb.setCacheNothing(false); // don't cache nothing
            qb.setItemType(SessionConstants.ITEM_TYPE_SESSION);
            qb.addCondition(SessionConstants.DATA_TYPE_SESSION_ID, "eq", id);
            Item item = (Item) qb.getResult();
            facade = _facadeService.getFacade(item, SessionFacade.class);
        }

        return facade;
    }

    private long getSessionTimeout(SessionDomainFacade domain) {
        return domain.getSessionTimeout(SessionConstants.DEFAULT_SESSION_TIMEOUT).longValue();
    }

    private long getRememberTimeout(SessionDomainFacade domain) {
        return domain.getRememberTimeout(SessionConstants.DEFAULT_REMEMBER_TIMEOUT).longValue();
    }

    private long getSessionLimit(SessionDomainFacade domain) {
        return domain.getSessionLimit(Long.MAX_VALUE).longValue();
    }

    private static Map<String, AccessInfo> __accesses = new HashMap<String, AccessInfo>();

    // I check for a queued access here because the following can
    // occur .. Wait 29 minutes 59 seconds, view a page, I see
    // it, a ping is scheduled, click on another page and the
    // session has now expired...
    private Date getLastAccess(SessionFacade session) {
        AccessInfo info;
        synchronized (__accesses) {
            info = __accesses.get(session.getSessionId());
        }
        Date lastAccess = session.getLastAccess();
        if ((info != null) && lastAccess.before(info._lastAccess)) {
            lastAccess = info._lastAccess;
        }
        return lastAccess;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void pingSession(String id, String ipAddress) {

        logger.log(Level.FINE, "Pinging session, {0}, {1}", new Object[]{id, ipAddress});

        synchronized (__accesses) {
            // I don't think I have Current time yet
            __accesses.put(id, new AccessInfo(new Date(), ipAddress));
        }

    }

    // This is used above; it should match the flush interval
    private static final long FLUSH_INTERVAL = DateUtils.Unit.minute.getValue(5);

    @Scheduled("5 minutes")
    public void flushSessionAccesses() {

        Map<String, AccessInfo> tmp;
        synchronized (__accesses) {
            tmp = new HashMap<String, AccessInfo>(__accesses);
            __accesses.clear(); // optimistically clear the static map
        }
        final Map<String, AccessInfo> accesses = tmp; // grumble
        logger.log(Level.FINE, "Flushing sessions, {0}", accesses);
        String nodeName = BaseServiceMeta.getServiceMeta().getLocalHost();
        for (Map.Entry<String, AccessInfo> entry : accesses.entrySet()) {
            SessionFacade session = findSession(entry.getKey());
            if (session != null) {
                AccessInfo info = entry.getValue();
                if (session.getLastAccess().before(info._lastAccess)) {
                    updateSession(session, info._lastAccess, info._ipAddress, nodeName);
                }
            }
        }

        EntityContext.onCompletion(new TransactionCompletion() {
                @Override
                public void onRollback() {
                    // Upon tx rollback, put each entry back unless it has been
                    // replaced by a more recent one
                    synchronized (__accesses) {
                        for (Map.Entry<String, AccessInfo> entry : accesses.entrySet()) {
                            if (!__accesses.containsKey(entry.getKey())) {
                                __accesses.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            });

    }

    @Scheduled(value = "5 minutes", singleton = true)
    public void purgeExpiredSessions() {
        // Pull information about sessions eligible for the purge
        final Query query = createQuery("SELECT id, root.id, created, lastAccess, remember FROM SessionFinder WHERE expires <= :now");
        query.setParameter("now", new Date());
        final List results = query.getResultList();
        if (results.isEmpty()) {
            return;
        }

        // Aggregate the durations of all the sessions being purged
        final List<Long> ids = new ArrayList<>();
        final Map<Long, MutablePair<Integer, Long>> statistics = new HashMap<>(); // map from domain to (count, duration)
        for (final Object o : results) {
            final Object[] a = (Object[]) o;
            final Long id = (Long) a[0];
            final Long domain = (Long) a[1];
            final Date created = (Date) a[2];
            final Date lastAccess = (Date) a[3];
            final Boolean remember = (Boolean) a[4]; // remembered sessions would pollute statistics
            ids.add(id);
            if ((created != null) && (lastAccess != null) && !Boolean.TRUE.equals(remember)) {
                final MutablePair<Integer, Long> statistic = statistics.computeIfAbsent(domain, (d) -> MutablePair.of(0, 0L));
                statistic.setLeft(statistic.getLeft() + 1);
                statistic.setRight(statistic.getRight() + lastAccess.getTime() - created.getTime());
            }
        }

        // Purge the sessions
        final Query delete = createQuery("DELETE FROM SessionFinder WHERE id in :ids");
        delete.setParameter("ids", ids);
        final int count = delete.executeUpdate();
        logger.log(Level.FINE, "Purged sessions, {0}", count);

        // Store the statistics under current month's aggregate
        final String date = SessionConstants.SESSION_STATISTICS_DATE_FORMAT.format(new Date());
        for (Map.Entry<Long, MutablePair<Integer, Long>> entry : statistics.entrySet()) {
            ManagedUtils.commit(); // commit before each domain lock to minimize chances of contention
            final SessionDomainFacade domain = _facadeService.getFacade(entry.getKey(), SessionDomainFacade.class);
            final SessionStatisticsFacade sessionStatistics = domain.getOrCreateSessionStatisticsByDate(date);
            sessionStatistics.setCount(entry.getValue().getLeft() + NumberUtils.longValue(sessionStatistics.getCount()));
            sessionStatistics.setDuration(entry.getValue().getRight() + NumberUtils.longValue(sessionStatistics.getDuration()));
        }
    }

    public void closeDomainSessions(Long domainId) {
        Query query = createQuery("UPDATE SessionFinder SET state = :state WHERE root.id = :domainId");
        query.setParameter("state", SessionState.Closed.toString());
        query.setParameter("domainId", domainId);
        int count = query.executeUpdate();
        logger.log(Level.FINE, "Closed sessions for domain {0}, {1}", new Object[] { domainId, count });
    }

    static class AccessInfo {
        Date _lastAccess;
        String _ipAddress;

        AccessInfo(Date lastAccess, String ipAddress) {
            _lastAccess = lastAccess;
            _ipAddress = ipAddress;
        }

        public String toString() {
            return "AccessInfo[" + _lastAccess + ", " + _ipAddress + "]";
        }
    }
}
