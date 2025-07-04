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

package com.learningobjects.cpxp.service.replay;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import jakarta.persistence.Query;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.GuidUtil;

/**
 * Replay service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ReplayServiceBean extends BasicServiceBean implements ReplayService {
    private static final Logger logger = Logger.getLogger(ReplayServiceBean.class.getName());
    // TODO: configurable
    public static final long MAX_REPLAY_DELTA = 10 * 60 * 1000L; // 10m

    public static final long CLEANUP_INTERVAL = 5 * 60 * 1000L; // 5m

    public static final long MAX_SAME_IP_DELTA = 30 * 1000L; // 30s

    private static final String GENERATED_NONCE_PREFIX = "cpxp-";

    public void accept(Long systemId, Date time, String nonce, String ip) throws ReplayException {

        long now = getCurrentTime().getTime();
        long delta = now - time.getTime();
        if (delta > MAX_REPLAY_DELTA) {
            logger.log(Level.FINE, "Replay time too old, {0}", delta);
            throw new ReplayException("Clock skew: " + DateUtils.getDelta(time, getCurrentTime()), true);
        } else if (delta < -MAX_REPLAY_DELTA) {
            logger.log(Level.FINE, "Replay time too far in the future, {0}", delta);
            throw new ReplayException("Clock skew: " + DateUtils.getDelta(time, getCurrentTime()), true);
        }

        Query query = createQuery("SELECT r FROM Replay r WHERE r.system = :system AND r.nonce = :nonce");
        query.setParameter("system", systemId);
        query.setParameter("nonce", nonce);
        List replays = query.getResultList();
        Replay replay = !replays.isEmpty() ? (Replay) replays.get(0) : null;
        boolean failed = false;
        if (ip == null) {
            failed = (replay != null);
        } else if (replay != null) {
            long delta2 = now - replay.getReceived().getTime();
            failed = /* TODO: FIXME: !ip.equals(replay.getIp()) ||*/ (delta2 > MAX_SAME_IP_DELTA);
        }
        if (failed) {
            logger.log(Level.FINE, "Replay nonce found in database, {0}, {1}", new Object[]{systemId, nonce});
            throw new ReplayException("Duplicate nonce: " + nonce, false);
        }

        if (replay == null) {
            replay = new Replay();
            replay.setSystem(systemId);
            replay.setTime(time);
            replay.setNonce(nonce);
            if (ip != null) {
                replay.setIp(ip);
                replay.setReceived(getCurrentTime());
            }
            getEntityManager().persist(replay);
        }

    }

    @Override
    public String generateNonce() {
        String nonce = GuidUtil.longGuid();

        Replay replay = new Replay();
        replay.setTime(new Date());
        replay.setNonce(GENERATED_NONCE_PREFIX + nonce);
        getEntityManager().persist(replay);

        return nonce;
    }

    @Override
    public Boolean validateNonce(String nonce) {
        Query query = createQuery("SELECT r FROM Replay r WHERE r.nonce = :nonce");
        query.setParameter("nonce", GENERATED_NONCE_PREFIX + nonce);
        List replays = query.getResultList();

        Replay replay = !replays.isEmpty() ? (Replay) replays.get(0) : null;

        return replay == null ? false : true;
    }

    @Scheduled(value = "60 seconds", singleton = true)
    public void purgeReplayTokens() {

        Date time = new Date(System.currentTimeMillis() - MAX_REPLAY_DELTA);
        logger.log(Level.FINE, "Purging replay tokens, {0}", time);
        Query query = createQuery("DELETE FROM Replay r WHERE r.time <= :time");
        query.setParameter("time", time);
        int replays = query.executeUpdate();
        logger.log(Level.FINE, "Purged replay tokens, {0}", replays);

    }
}
