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

package loi.cp.appevent.impl;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.registry.ComponentRegistry;
import com.learningobjects.cpxp.component.registry.ComponentRegistryMaps;
import com.learningobjects.cpxp.component.registry.Registry;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.appevent.AppEventConstants;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.ThreadLog;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.cpxp.util.tx.AfterTransactionCompletionListener;
import de.tomcat.juli.LogMeta;
import loi.apm.Apm;
import loi.cp.appevent.AppEvent;
import loi.cp.appevent.OnEventComponent;
import loi.cp.appevent.facade.AppEventFacade;
import loi.cp.appevent.facade.AppEventListenerFacade;

import javax.annotation.Nullable;
import javax.inject.Inject;
import jakarta.persistence.Query;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AppEventExecution extends VoidOperation {
    private final static Logger logger = Logger.getLogger(AppEventExecution.class.getName());

    @Inject
    FacadeService _facadeService;

    @Inject
    QueryService _queryService;

    @Inject
    DomainWebService _domainWebService;

    private final Long _id;

    public AppEventExecution(Long id) {
        _id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute() {
        ManagedUtils.di(this, true);
        AppEventFacade facade = _facadeService.getFacade(_id, AppEventFacade.class);

        // clearing logmeta is left to the caller so subsequent transaction commit information
        // is still decorated with this metadata
        LogMeta.event(_id);
        if (facade == null) {
            logger.info(String.format("AppEvent %d was deleted and will not execute.", _id));
            return;
        }
        LogMeta.domain(facade.getRootId());
        if(facade.getState()!= null && facade.getState().equals(AppEventState.Complete)){
            logger.info(String.format("AppEvent %d has already been completed and will not execute again ", _id));
            return;
        }

        logger.log(Level.INFO, String.format("Executing app event (%d, %s)", _id, facade.getEventId()));

        // The next (most earliest date) the event should be rerun, if at all
        Date next = null;
        try {
            ThreadLog.begin("AppEventExecution:" + facade.getEventId());
            Apm.setTransactionName("appevent", facade.getEventId());
            Apm.addCustomParameter("eventId", facade.getId());
            Apm.addCustomParameter("domainId", facade.getRootId());
            Apm.ignoreApdex(); // do we need this?
            _domainWebService.setupContext(facade.getRootId());
            next = executeAppEventImpl(facade);
        } catch (Exception ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
            Apm.noticeError(ex);
        } finally {
            EntityContext.onCompletion(new AfterTransactionCompletionListener() {
                @Override
                public void onCommit() {
                    Current.clear();
                }

                @Override
                public void onRollback() {
                    Current.clear();
                }
            });
            ThreadLog.end();
        }
        // if an event listener returns a reschedule time, update event
        completeScheduledEvent(next);
        // a sql query updated the row, now reload the entity
        facade.refresh();
    }

    /** For testing, executes an app-event with fewer surprising side-effects. */
    public void executeAppEvent() throws Exception {
        ManagedUtils.di(this, true);
        AppEventFacade facade = _facadeService.getFacade(_id, AppEventFacade.class);
        Date next = executeAppEventImpl(facade);
        if (next != null) {
            facade.setDeadline(next);
            facade.setFired(DataSupport.MIN_TIME);
        } else {
            facade.setState(AppEventState.Complete);
            facade.setProcessingEnd(new Date());
        }
    }

    private Date executeAppEventImpl(AppEventFacade facade) throws Exception {
        Date next = null;
        String eventId = facade.getEventId();
        // SuppressWarnings: unchecked
        Class<? extends AppEvent> eventType = (Class<? extends AppEvent>) ComponentSupport.loadClass(eventId);
        AppEvent event = ComponentUtils.fromJson(facade.getPayload(), eventType);

        // Execute any global listeners
        Set<ComponentInstance> listeners = getGlobalEventListenersForEventType(eventType);
        for (ComponentInstance listener : listeners) {
            // Cast the typeless component as an event listener
            OnEventInstance listenerFn = listener.getFunctionInstance(OnEventInstance.class, eventType);

            if (listenerFn == null) {
                logger.warning("Somehow I got to a state where I have a stateless component (" + listener.getIdentifier() + ") that is recognized to handle " + eventType + " but does not have a function to do so.");
                throw new IllegalStateException("Cannot find event handler for " + eventType + " in stateless listener " + listener.getIdentifier() + " in environment");
            }

            // Execute the event listener function
            logger.log(Level.INFO, String.format("Executing global listener (%s)", listener.getIdentifier()));
            Date nextForListener = asDate(listenerFn.invoke(facade, event));
            next = mostRecent(next, nextForListener);
        }

        // Was a specific target specified
        ComponentFacade target = facade.getTarget();
        if (target != null) {
            logger.log(Level.INFO, String.format("Executing target listener (%s)", target));
            ComponentInstance component = ComponentSupport.getComponent(target, null);
            OnEventInstance onEvent = component.getFunctionInstance(OnEventInstance.class, eventType);

            if (onEvent == null) {
                logger.warning("Somehow I got to a state where I have a target component (" + component.getId() + "/" + component.getIdentifier() + ") that is recognized to handle " + eventType + " but does not have a function to do so.");
                throw new IllegalStateException("Cannot find event handler for " + eventType + " in target " + component.getId() + "/" + component.getIdentifier() + " in environment");
            }

            Date nextForListener = asDate(onEvent.invoke(facade, event));
            next = mostRecent(next, nextForListener);
        }

        Long source = facade.getParentId();
        facade.setProcessingStart(Date.from(Instant.now()));
        for (AppEventListenerFacade listener : getEventListeners(source, eventId)) {
            logger.log(Level.INFO, String.format("Executing source listener (%d)", listener.getParentId()));
            try {
                ComponentInstance component = ComponentSupport.getComponent(listener.getParent(), null);
                OnEventInstance onEvent = component.getFunctionInstance(OnEventInstance.class, eventType);

                if (onEvent == null) {
                    logger.warning("Somehow I got to a state where I have a source component (" + component.getId() + "/" + component.getIdentifier() + ") that is recognized to handle " + eventType + " but does not have a function to do so.");
                    throw new IllegalStateException("Cannot find event handler for " + eventType + " in source " + component.getId() + "/" + component.getIdentifier() + " in environment");
                }

                Date nextForListener = asDate(onEvent.invoke(facade, event));
                next = mostRecent(next, nextForListener);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Event notification error: " + listener, ex);
            }
        }

        return next;
    }

    private Date asDate(Object o) {
        if ((o != null) && OptionLike.isOptionLike(o.getClass())) {
            return asDate(OptionLike.getOrNull(o));
        } else if (o instanceof Instant) {
            return Date.from((Instant) o);
        } else {
            return (Date) o;
        }
    }

    /**
     * Returns the more recent, non-null of the two <code>Date</code>.  If both are null, then null is returned,
     * otherwise if one is null, then the other is returned.  If both are non-null, then the earliest date is returned.
     *
     * @param lhs the first value to check
     * @param rhs the second value to check
     * @return the earliest non-null of <code>lhs</code> and <code>rhs</code>; otherwise <code>null</code> if both are
     * null
     */
    @Nullable
    private Date mostRecent(@Nullable Date lhs, @Nullable Date rhs) {
        if (lhs == null && rhs == null) {
            return null;
        } else if (lhs == null || rhs == null) {
            return Optional.ofNullable(lhs).orElse(rhs);
        } else {
            return lhs.before(rhs) ? lhs : rhs;
        }
    }

    /**
     * Gets all event listeners for the given <code>eventType</code>.  Multiple listeners may exist for the given type.
     * The order they are taken is arbitrary.
     *
     * @param eventType the event type to provide listeners for
     * @return a <code>Set</code> of components that accept the given <code>eventType</code>
     */
    private Set<ComponentInstance> getGlobalEventListenersForEventType(Class<? extends AppEvent> eventType) {
        // We cannot use the standard calls to pull from the registry since we have a one-to-many mapping
        ComponentRegistry componentRegistry = ComponentSupport.getEnvironment().getRegistry();
        ComponentRegistryMaps registryMaps = (ComponentRegistryMaps) componentRegistry;

        Map<Class<?>, Registry<Annotation, DelegateDescriptor>> registryRegistry = registryMaps.getRegistryRegistry();
        Registry<Annotation, DelegateDescriptor> listenerRegistry = registryRegistry.get(OnEventComponent.class);

        // Get all the types that handle this event
        Collection<DelegateDescriptor> descriptors = Optional.ofNullable(listenerRegistry.toMap().get(eventType.toString()))
                                                             .orElse(Collections.emptyList());

        // Instantiate a stateless instance of each of these types
        return descriptors.stream()
                          .map(DelegateDescriptor::getComponent)
                          .map(descriptor -> descriptor.getInstance(null, null))
                          .collect(Collectors.toSet());
    }

    private List<AppEventListenerFacade> getEventListeners(Long target, String eventId) {
        QueryBuilder qb = _queryService.queryRoot(AppEventConstants.ITEM_TYPE_APP_EVENT_LISTENER);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_TARGET, "eq", target);
        qb.addCondition(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_EVENT_ID, "eq", eventId);
        return qb.getFacadeList(AppEventListenerFacade.class);
    }

    // If reschedule is non-null then reschedule the current event for then
    private void completeScheduledEvent(Date reschedule) {
        Query query;
        if (reschedule != null) {
            logger.log(Level.INFO, String.format("Rescheduling app event (%s)", reschedule));
            query = _queryService.createQuery(
                    "UPDATE AppEventFinder" +
                    " SET deadline = :deadline, fired = :unfired" +
                    " WHERE id = :id AND host = :host");
            query.setParameter("deadline", reschedule);
            query.setParameter("unfired", DataSupport.MIN_TIME);
        } else {
            query = _queryService.createQuery(
                    "UPDATE AppEventFinder" +
                    " SET state = :complete, finished = :finishTime" +
                    " WHERE id = :id" +
                    " AND host = :host");
            query.setParameter("complete", AppEventState.Complete.toString());
            query.setParameter("finishTime", Date.from(Instant.now()));
        }
        query.setParameter("id", _id);
        query.setParameter("host", BaseServiceMeta.getServiceMeta().getLocalHost());
        if (query.executeUpdate() != 1) {
            throw new RuntimeException("Concurrent AppEventSchedule execution error: " + _id);
        }
    }
}
