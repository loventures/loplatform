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

package com.learningobjects.cpxp.service.bootstrap;

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.CpxpClasspath;
import com.learningobjects.cpxp.component.ComponentManager;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.operation.DomainOperation;
import com.learningobjects.cpxp.operation.Executor;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.schedule.Scheduler;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.startup.StartupTaskService;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.cache.PressureValve;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The bootstrap service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class BootstrapServiceBean extends BasicServiceBean implements
        BootstrapService {
    private static final Logger logger = Logger.getLogger(BootstrapServiceBean.class.getName());

    @Inject
    private DomainWebService _domainWebService;

    @Override
    public void scheduleTasks() {
        logger.info("Schedule tasks");

        // upgrade and timer tasks may be taken on by any node
        CpxpClasspath.classGraph().getClassesWithMethodAnnotation(Scheduled.class).forEach(ci ->
          ci.getMethodInfo().forEach(mi -> {
              if (mi.hasAnnotation(Scheduled.class) && ci.hasAnnotation(Stateless.class)) {
                  var method = mi.loadClassAndGetMethod();
                  Scheduled scheduled = method.getAnnotation(Scheduled.class);
                  Class<?> service = method.getDeclaringClass().getInterfaces()[0];
                  String taskName = service.getName() + "." + method.getName();
                  ScheduledServiceMethod task = new ScheduledServiceMethod(ManagedUtils.getService(service), method, scheduled.singleton());
                  Scheduler.getScheduler().schedule(task, taskName, scheduled.value());
              }
          })
        );
    }

    private static class ScheduledServiceMethod implements Runnable {
        private final Object _service;
        private final Method _method;
        private final boolean _singleton;

        private ScheduledServiceMethod(Object service, Method method, boolean singleton) {
            _service = service;
            _method = method;
            _singleton = singleton;
        }

        public void run() {
            if (_singleton && !BaseServiceMeta.getServiceMeta().isDas()) {
                return;
            }
            try {
                ManagedUtils.perform(new VoidOperation() {
                    @Override
                    public void execute() throws Exception {
                        _method.invoke(_service);
                    }
                });
            } catch (Exception ex) {
                throw Throwables.propagate(ex);
            }
        }
    }

    public void startup() {

        Scheduler.startup();

        Executor.startup();

        PressureValve.startup();

        startupComponentFramework();
    }

    private void startupComponentFramework() {
        logger.log(Level.INFO, "Starting component framework");

        ComponentManager.startup();

        // ideally this should happen /before/ component postload/... is called, or some
        // other way to suppress background stuff cluster-wide
        if (BaseServiceMeta.getServiceMeta().isDas()) {
            /* StartupTaskService and friends have their implementation in overlörde,
             * which is off in all domains other than the Överlord one. */
            Long overlordDomain = _domainWebService.findDomainByType(DomainConstants.DOMAIN_TYPE_OVERLORD);
            new DomainOperation<>(overlordDomain, new VoidOperation() {
                @Override
                public void execute() {
                    ComponentSupport.lookupService(StartupTaskService.class).startup();
                }
            }).perform();
        }
    }

    public void shutdown() {

        Scheduler.shutdown();

        Executor.shutdown();

        PressureValve.shutdown();

    }

    @Scheduled("1 hour")
    public void heartbeat() {
        logger.info("Heartbeat.");
    }
}
