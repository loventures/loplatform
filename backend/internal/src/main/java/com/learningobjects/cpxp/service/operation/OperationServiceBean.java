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

package com.learningobjects.cpxp.service.operation;

import com.learningobjects.cpxp.component.eval.CacheInjector;
import com.learningobjects.cpxp.operation.Executor;
import com.learningobjects.cpxp.operation.ProxyOperation;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.Operation;
import com.learningobjects.cpxp.util.task.Priority;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * The operation service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OperationServiceBean extends BasicServiceBean implements OperationService {
    private static final Logger logger = Logger.getLogger(OperationServiceBean.class.getName());

    public <T> T perform(Operation<T> operation) {

        Object target = (operation instanceof ProxyOperation) ?
            ((ProxyOperation) operation).getTarget() : operation;
        ManagedUtils.di(target, false);

        return operation.perform();
    }

    /**
     * This is a hack that allows us to
     * do nested transactions by using the container tx management
     * User transaction itself doesn't support nested tx.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <T> T transact(Operation<T> operation) {

        Object target = (operation instanceof ProxyOperation) ?
            ((ProxyOperation) operation).getTarget() : operation;
        ManagedUtils.di(target, false);

        return operation.perform();
    }

    /**
     * Defer the processing of an operation. The operation will be
     * performed asynchronously in a separate context at some point
     * in the future; usually within a few seconds.
     *
     * @param operation the operation
     * @param join the number of milliseconds to synchronously wait for the
     * operation to complete, or -1
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T> Future<T> defer(final Operation<T> operation, final Priority priority, final String identifier, final long join) {
        assert isSafeIdentifier(identifier) : "Operation identifier must enclose variables in parentheses: " + identifier;
        return Executor.getExecutor().submit(operation, priority, identifier, join);
    }

    // Test that the identifier doesn't look like it has a PK without parentheses.
    // The identifier, sans paranthetic, turns into a APM transaction name
    // which must be one of a bounded set of values or APM will shut down our
    // transaction reporting because we have too many transaction names.
    private static boolean isSafeIdentifier(String identifier) {
        return (identifier == null) || !identifier.matches("^[^(]*[0-9].*");
    }

    @Scheduled(value = "5 minutes")
    public void logStuff() {
        Executor.getExecutor().logStuff();
        CacheInjector.logStuff();
    }
}
