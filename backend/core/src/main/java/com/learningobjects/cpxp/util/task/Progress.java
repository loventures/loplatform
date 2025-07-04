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

package com.learningobjects.cpxp.util.task;

import com.learningobjects.cpxp.util.NumberUtils;

public class Progress {
    private boolean _running;
    private int _count, _current;
    private String _message;
    private Throwable _error;

    public void start() {
        _running = true;
    }

    public void init(int count) {
        _count = count;
        _current = 0;
    }

    public int increment() {
        return ++ _current;
    }

    public int getPercent() {
        return !_running ? -2 : (_count == 0) ? -1 : NumberUtils.percent(Math.min(_current, _count), _count);
    }

    public int getCount() {
        return _count;
    }

    public void setError(Throwable th) {
        _error = th;
    }

    public Throwable getError() {
        return _error;
    }
}
