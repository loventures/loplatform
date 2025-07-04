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

package com.learningobjects.cpxp.service.domain;

/**
 * Enumeration of the known domain states.
 */
public enum DomainState {
    /* NOTE WELL:
     *
     * The order of this enum is important. Semantically, the lower the state (i.e. the lower
     * the ordinal number), the more important the domain is and thus the earlier that it will
     * be processed in processes such as startup task execution.
     */

    /** A domain in normal, active state. */
    Normal,
    /** A domain in read-only state. This is not currently supported. */
    ReadOnly,
    /** A domain in maintenance mode. It will return to normal state once maintenance finishes. */
    Maintenance,
    /** A domain that has been suspended. It may subsequently be reinstated. */
    Suspended,
    /** A domain currently being created. If creation fails it will remain in this state. */
    Init,
    /** A domain that has been deleted and cannot be reinstated. */
    Deleted;
}
