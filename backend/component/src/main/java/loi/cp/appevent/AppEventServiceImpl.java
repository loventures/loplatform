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

package loi.cp.appevent;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.appevent.AppEventConstants;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.util.Ids;
import loi.cp.appevent.facade.AppEventFacade;
import loi.cp.appevent.facade.AppEventListenerFacade;
import loi.cp.appevent.impl.AppEventScheduler;

@Service
public class AppEventServiceImpl implements AppEventService {
    private static final Logger logger = Logger.getLogger(AppEventServiceImpl.class.getName());

    private final QueryService _queryService;

    private final FacadeService _facadeService;

    public AppEventServiceImpl(final QueryService queryService, final FacadeService facadeService) {
        _queryService = queryService;
        _facadeService = facadeService;
    }

    @Override
    public void registerListener(Long listener, Class<? extends AppEvent> event, Long target) {
        String eventId = event.getName();
        if (!getEventListeners(listener, target, eventId).isEmpty()) {
            return;
        }
        AppEventListenerFacade appListener = _facadeService.addFacade(listener, AppEventListenerFacade.class);
        appListener.setEventId(eventId);
        appListener.setTarget(target);
    }

    @Override
    public void deregisterListener(Long listener, Class<? extends AppEvent> event, Long target) {
        String eventId = event.getName();
        for (AppEventListenerFacade facade : getEventListeners(listener, target, eventId)) {
            facade.delete();
        }
    }

    private List<AppEventListenerFacade> getEventListeners(Long listener, Long target, String eventId) {
        QueryBuilder qb = _queryService.queryParent(listener, AppEventConstants.ITEM_TYPE_APP_EVENT_LISTENER);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_EVENT_ID, "eq", eventId);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_TARGET, "eq", target);
        return qb.getFacadeList(AppEventListenerFacade.class);
    }

    private List<AppEventListenerFacade> getEventListeners(Long target, String eventId) {
        QueryBuilder qb = _queryService.queryRoot(AppEventConstants.ITEM_TYPE_APP_EVENT_LISTENER);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_TARGET, "eq", target);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_EVENT_ID, "eq", eventId);
        return qb.getFacadeList(AppEventListenerFacade.class);
    }

    @Override
    public void fireEvent(Id source, AppEvent event, Id... rels) {
        logger.info("Fire event (" + Ids.get(source) + ", " + event + ")");
        if (source == null) {
            return;
        }

        String eventId = event.getClass().getName();
        boolean hasListener = ComponentSupport.lookupComponent(OnEventComponent.class, event.getClass()) != null;
        if (!hasListener) {
            QueryBuilder qb = _queryService.queryAllDomains();
            qb.setItemType(AppEventConstants.ITEM_TYPE_APP_EVENT_LISTENER);
            qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_TARGET, "eq", source);
            qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_EVENT_ID, "eq", eventId);
            // TODO: Cache key etc
            hasListener = !qb.getResultList().isEmpty();
        }
        if (!hasListener) {
            logger.warning("Event " + event.getClass().getName() + " has no listener");
            return;
        }
        logger.info("Persisting event");
        AppEventFacade facade = addAppEventFacade(source, event, rels);
        facade.setDeadline(Current.getTime());
        facade.setFired(Current.getTime());
        facade.setHost(BaseServiceMeta.getServiceMeta().getLocalHost());
        AppEventScheduler.eventFired(facade.getId());
    }

    @Override
    public void scheduleEvent(Date when, Id source, Id target, AppEvent event, Id... rels) {
        logger.info("Schedule event (" + when + ", " + Ids.get(source) + ", " + event + ")");
        AppEventFacade facade = addAppEventFacade(source, event, rels);
        facade.setDeadline(when);
        facade.setTarget(target);
        facade.setFired(DataSupport.MIN_TIME); // I can't leave fired null because then my db queries get ugly
        String host = BaseServiceMeta.getServiceMeta().isDas() ? BaseServiceMeta.getServiceMeta().getLocalHost() : "";
        facade.setHost(host);
        AppEventScheduler.eventScheduled(when);
    }

    private AppEventFacade addAppEventFacade(Id source, AppEvent event, Id[] rels) {
        AppEventFacade facade = _facadeService.addFacade(source.getId(), AppEventFacade.class);
        facade.setEventId(event.getClass().getName());
        facade.setCreated(Current.getTime());
        facade.setPayload(event);
        if (rels.length > 0) {
            facade.setRel0(rels[0]);
            if (rels.length > 1) {
                facade.setRel1(rels[1]);
            }
        }
        return facade;
    }

    @Override
    public void deleteEvents(Id source, Id target, Class<? extends AppEvent> eventType) {
        String eventId = eventType.getName();
        QueryBuilder qb = _queryService.queryParent(source.getId(), AppEventConstants.ITEM_TYPE_APP_EVENT);
        qb.setCacheQuery(false); // just to be safe
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_EVENT_ID, "eq", eventId);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_TARGET, "eq", target);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_STATE, "eq", null);
        for (AppEventFacade event: qb.getFacadeList(AppEventFacade.class)) {
            event.delete();
        }
    }

    @Override
    public Date getNextEventTime(Id source, Id target, Class<? extends AppEvent> eventType) {
        String eventId = eventType.getName();
        QueryBuilder qb = _queryService.queryParent(source.getId(), AppEventConstants.ITEM_TYPE_APP_EVENT);
        qb.setCacheQuery(false); // just to be safe
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_EVENT_ID, "eq", eventId);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_TARGET, "eq", target);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_STATE, "eq", null);
        return qb.setDataProjection(BaseDataProjection.ofAggregateData(AppEventConstants.DATA_TYPE_APP_EVENT_DEADLINE, Function.MIN))
          .getResult();
    }

    /**
     * @return information about how quickly the app events are processed.  These stats are for
     * all the app events in the system, not just those that have affinity to this server.
     */
    @Override
    public QueueStats getQueueStats() {

        // Query for the number of overdue events and the due date of the most overdue event.
        QueryBuilder query = _queryService.queryAllDomains(AppEventConstants.ITEM_TYPE_APP_EVENT);
        query.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_DEADLINE, Comparison.le, new Date());
        query.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_STATE, Comparison.eq, null);
        DataProjection[] aggregates = {
          BaseDataProjection.ofAggregateData(DataTypes.META_DATA_TYPE_ID, Function.COUNT),
          BaseDataProjection.ofAggregateData(AppEventConstants.DATA_TYPE_APP_EVENT_DEADLINE, Function.MIN)
        };
        query.setDataProjection(aggregates);

        // Package it in a QueueStats object and explicitly set currentWaitTime to 0 if there are
        // no overdue events
        final List<Object[]> rows = query.getResultList();
        Object[] row = rows.get(0);
        QueueStats result = new QueueStats();
        result.numInQueue = ((Number)row[0]).longValue();
        if (result.numInQueue == 0) {
            result.currentWaitTimeMs = 0;
        } else {
            Date earliestDeadline = (Date)row[1];
            result.currentWaitTimeMs = System.currentTimeMillis() - earliestDeadline.getTime();
        }
        return result;

    }

}
