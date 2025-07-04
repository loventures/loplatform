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

package com.learningobjects.cpxp.operation;

import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.script.ScriptService;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.util.ManagedObject;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.ThreadLog;
import com.learningobjects.cpxp.util.task.Progress;
import de.tomcat.juli.LogMeta;
import loi.apm.Apm;
import loi.apm.Trace;

import javax.inject.Inject;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

class ExecutorTask extends ManagedObject implements Runnable {
    private static final Logger logger = Logger.getLogger(ExecutorTask.class.getName());

    @Inject
    private DomainWebService _domainWebService;

    @Inject
    private UserWebService _userWebService;

    @Inject
    private ScriptService _scriptService;

    private final Executor _executor;

    public ExecutorTask(Executor executor) {
        _executor = executor;
    }

    public void run() {

        logger.log(Level.FINE, "Operation thread ready");
        boolean done = false;
        do {
            try {
                final FutureOperation<?> operation = _executor.getQueue().take();
                ThreadLog.begin(operation.getIdentifier());
                long then = System.currentTimeMillis();
                try {
                    ManagedUtils.perform(new VoidOperation() {
                        @Override
                        public void execute() throws Exception {
                            logger.log(Level.INFO, "Operation: " + operation.getIdentifier());
                            logger.log(Level.FINE, "Performing deferred operation, {0}, {1}", new Object[]{operation, operation.getTarget()});
                            try {
                                Current.setTime(new Date());
                                Current.put(Progress.class, _executor.getOperationProgress(operation.getIdentifier()));

                                if (operation.getDomainId() != null) {
                                    LogMeta.domain(operation.getDomainId());
                                    Current.setDomainDTO(_domainWebService.getDomainDTO(operation.getDomainId()));
                                    Current.setUserDTO(_userWebService.getUserDTO(operation.getUserId()));
                                    _scriptService.initComponentEnvironment();
                                }

                                executeTask(operation);
                            } finally {
                                Current.clear();
                            }
                        }
                    });
                } catch (Throwable th) {
                    operation.cancel(true); // in case an error happened before it was performed...
                    _executor.getQueue().taskFailed(operation, th); // this is inelegant.
                    logger.log(Level.WARNING, "Deferred operation error", th);
                } finally {
                    ThreadLog.end();
                    _executor.getQueue().complete(operation);
                }
                long delta = System.currentTimeMillis() - then;
                logger.log(Level.INFO, "Operation " + operation.getIdentifier() + " processed in " + delta + "ms");
            } catch (InterruptedException ex) {
                done = true;
            } finally {
                LogMeta.clear();
            }
            done |= _executor.isStopped();
        } while (!done);

    }

    @Trace(dispatcher = true)
    private void executeTask(FutureOperation<?> operation) throws Exception {
        Apm.setTransactionName("Async", StringUtils.substringBefore(operation.getIdentifier(), "(")); // for operation ids of the form foo(params), strip the params. Should generalize this form.
        operation.perform();
        operation.get();
    }
}
