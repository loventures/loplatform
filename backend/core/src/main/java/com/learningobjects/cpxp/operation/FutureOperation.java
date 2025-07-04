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
import com.learningobjects.cpxp.util.Operation;
import com.learningobjects.cpxp.util.task.Priority;
import com.learningobjects.cpxp.util.task.Task;
import com.learningobjects.cpxp.util.task.TaskQueue;

import java.util.Date;
import java.util.concurrent.*;

class FutureOperation<T> implements Future<T>, Task {
    private final Priority _priority;
    private final String _identifier;
    private final Operation<T> _operation;
    private final Date _created;
    private final Long _domainId;
    private final Long _userId;
    private TaskQueue _queue;
    private boolean _cancelled;
    private boolean _done;
    private Throwable _cause;
    private T _result;

    public FutureOperation(Operation<T> operation, Priority priority, String identifier) {
        _operation = operation;
        _priority = priority;
        _identifier = identifier;
        _created = new Date();
        _domainId = Current.getDomain();
        _userId = Current.getUser();
    }

    public Priority getPriority() {
        return _priority;
    }

    public Object getGroup() {
        return (_domainId == null) ? Long.valueOf(-1L) : _domainId;
    }

    public String getIdentifier() {
        return _identifier;
    }

    public Operation<T> getOperation() {
        return _operation;
    }

    public Date getCreated() {
        return _created;
    }

    public Long getDomainId() {
        return _domainId;
    }

    public Long getUserId() {
        return _userId;
    }

    public void offered(TaskQueue queue) {
        _queue = queue;
    }

    public synchronized boolean cancel(boolean mayInterrupt) {
        if (!_cancelled && !_done) {
            _cancelled = _queue.remove(this);
            notify();
        }
        return _cancelled;
    }

    public synchronized boolean isCancelled() {
        return _cancelled;
    }

    public synchronized boolean isDone() {
        return _cancelled || _done;
    }

    public synchronized T get() throws InterruptedException, ExecutionException, CancellationException {
        if (_queue == null) {
            throw new IllegalStateException("Operation not yet offered");
        }
        while (!_done) {
            if (_cancelled) {
                throw new CancellationException();
            }
            wait();
        }
        if (_cause != null) {
            throw new ExecutionException(_cause);
        }
        return _result;
    }

    public synchronized T get(long amount, TimeUnit unit) throws InterruptedException, ExecutionException, CancellationException, TimeoutException {
        if (_queue == null) {
            throw new IllegalStateException("Operation not yet offered");
        }
        if (!_cancelled && !_done) {
            wait(unit.toMillis(amount));
        }
        if (_cancelled) {
            throw new CancellationException();
        } else if (_cause != null) {
            throw new ExecutionException(_cause);
        } else if (!_done) {
            throw new TimeoutException();
        }
        return _result;
    }

    Object getTarget() {
        return (_operation instanceof ProxyOperation) ?
            ((ProxyOperation) _operation).getTarget() : _operation;
    }

    void perform() {
        try {
            _result = _operation.perform();
        } catch (Throwable th) {
            _cause = th;
        } finally {
            _done = true;
        }
        synchronized (this) {
            notify();
        }
    }
}
