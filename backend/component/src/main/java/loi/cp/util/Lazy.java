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

package loi.cp.util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Supplier;

/**
 * Retrieves a value when it is needed and then keeps it in-memory. This implementation is not thread-safe.
 * @param <T> value type
 */
@NotThreadSafe
public class Lazy<T> {
    private T _instance = null;
    private boolean _isCached = false;
    private Supplier<T> _supplier;

    public Lazy(Supplier<T> supplier) {
        _supplier = supplier;
    }

    /**
     * Lazily gets the value.
     * @return the value, which can be null when the supplier returns null
     */
    @Nullable
    public T get() {
        if (!_isCached) {
            _instance = _supplier.get();
            _isCached = true;
        }
        return _instance;
    }
}
