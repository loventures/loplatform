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

import com.learningobjects.cpxp.util.Operation;
import loi.apm.Apm;
import loi.apm.Trace;

/**
 * A wrapper for an operation that will be noticed by APM.
 */
public class DispatcherOperation<T> implements Operation<T> {
    private final Operation<T> _wrapped;
    private final String _category;
    private final String _transaction;
    private final String _customParameterName;
    private final Number _customParameterValue;

    public DispatcherOperation(Operation<T> wrapped) {
        this(wrapped, null, null);
    }

    public DispatcherOperation(Operation<T> wrapped, String category, String transaction) {
        this(wrapped, category, transaction, null, null);
    }

    public DispatcherOperation(Operation<T> wrapped, String category, String transaction, String customParameterName, Number customParameterValue) {
        _wrapped = wrapped;
        _category = category;
        _transaction = transaction;
        _customParameterName = customParameterName;
        _customParameterValue = customParameterValue;
    }

    @Override
    public T perform() {
        return performImpl();
    }

    @Trace(dispatcher = true)
    private T performImpl() {
        if (_category != null) {
            Apm.setTransactionName(_category, _transaction);
            if (_customParameterName != null) {
                Apm.addCustomParameter(_customParameterName, _customParameterValue);
            }
        }
        return _wrapped.perform();
    }
}
