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

class ScheduledTask<T extends Task> implements Comparable<ScheduledTask<T>> {
    final T task;
    long created;
    long started;

    ScheduledTask(T task) {
        this.task = task;
        this.created = System.currentTimeMillis();
    }

    @Override
        public int compareTo(ScheduledTask<T> other) {
        int cmp = task.getPriority().compareTo(other.task.getPriority());
        if (cmp == 0) {
            cmp = (int) (started - other.started);
            if (cmp == 0) {
                cmp = hashCode() - other.hashCode(); // meh, hack. relies in identity hash code.
            }
        }
        return cmp;
    }
}
