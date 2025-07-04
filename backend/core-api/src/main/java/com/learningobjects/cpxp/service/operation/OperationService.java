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

import java.util.concurrent.Future;
import javax.ejb.Local;

import com.learningobjects.cpxp.util.Operation;
import com.learningobjects.cpxp.util.task.Priority;

/**
 * The operation service.
 */
@Local
public interface OperationService {
    /**
     * Perform an operation.
     *
     * @param operation
     *            the operation
     *
     * @return the operation result
     */
    public <T> T perform(Operation<T> operation);

    /**
     * Perform an operation in a new transaction.
     *
     * @param operation
     *            the operation
     *
     * @return the operation result
     */
    public <T> T transact(Operation<T> operation);

    /**
     * Defer the performance of an operation.
     *
     * @param operation
     *            the operation
     * @param join
     *            the number of milliseconds to synchronously wait for the
     * operation to complete, or -1
     */
    public <T> Future<T> defer(Operation<T> operation, Priority priority, String identifier, long join);

    public void logStuff();
}
