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

package com.learningobjects.cpxp.service;

import org.hibernate.HibernateException;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.PooledOptimizer;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

// See https://hibernate.onjira.com/browse/HHH-3608, the stock pooled
// optimizer is not cluster safe and double pulls from the sequence all
// the time..

/**
 * Customized pooled optimizer which fixes a few bugs in Hibernate's
 * implementation.
 */
public class CpxpOptimizer extends PooledOptimizer {
    private final Logger logger = Logger.getLogger(CpxpOptimizer.class.getName());

    private IntegralDataTypeHolder _value;

    private IntegralDataTypeHolder _hiValue;

    public CpxpOptimizer(Class<?> returnClass, int incrementSize) {
        super(returnClass, incrementSize);
        if (incrementSize < 1) {
            throw new HibernateException("Increment size cannot be less than 1");
        }
    }

    @Override
    public synchronized Serializable generate(AccessCallback callback) {
        if ((_hiValue == null) || !_value.lt(_hiValue)) {
            _value = callback.getNextValue();
            _hiValue = _value.copy().add(super.incrementSize);
            logger.log(Level.FINE, "Generate allocate, {0}, {1}, {2}", new Object[]{_value, _hiValue, hashCode()});
        }
        return _value.makeValueThenIncrement();
    }

    /** A common means to access the last value obtained from the underlying source.
     * This is intended for testing purposes, since accessing the underlying database
     * source directly is much more difficult. */
    @Override
    public IntegralDataTypeHolder getLastSourceValue() {
        return _hiValue; // - super.incrementSize;
    }

    /** Are increments to be applied to the values stored in the underlying value source? */
    @Override
    public boolean applyIncrementSizeToSourceValues() {
        return true;
    }
}
