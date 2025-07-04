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

import java.util.concurrent.Future;

import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.operation.OperationService;
import com.learningobjects.cpxp.util.Operation;
import com.learningobjects.cpxp.util.task.Priority;

public class Operations {
    private Operations() {
    }

    public static <T> T perform(Operation<T> operation) {
        return operation.perform();
    }

    public static <T> T transact(Operation<T> operation) {
        return new TransactedOperation<T>(operation).perform();
    }

    /**
     * Performs the operation asynchronously with no transaction.
     * The operation will not have access to the caller's JNDI.
     */
    public static <T> Future<T> defer(Operation<T> operation, Priority priority, String identifier) {
        return defer(operation, priority, identifier, -1L);
    }

    public static <T> Future<T> defer(Operation<T> operation, Priority priority, String identifier, long join) {

        OperationService operationService = ServiceContext.getContext().getService(OperationService.class);
        return operationService.defer(operation, priority, identifier, join);
    }

    public static <T> Future<T> deferTransact(Operation<T> operation, Priority priority, String identifier) {
        return defer(new TransactedOperation<T>(operation), priority, identifier);
    }

    public static <T> T asDomain(Long domain, Operation<T> operation) {
        return new DomainOperation<T>(domain, operation).perform();
    }

    public static <T> T asRoot(Operation<T> operation) {
        return new PrivilegedOperation<T>(operation).perform();
    }

    public static <T> Operation<T> asNRTransaction(Operation<T> operation) {
        return new DispatcherOperation<>(operation);
    }
}

