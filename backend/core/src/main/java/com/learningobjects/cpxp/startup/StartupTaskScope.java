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

package com.learningobjects.cpxp.startup;

/**
 * Classifies the scope of a startup task, whether it applies to the entire
 * database, just the overlord domain, or just individual normal domains
 */
public enum StartupTaskScope {
    /**
     * This task affects the entire system and should be run before
     * all per-domain startup tasks.
     */
    System,

    /**
     * This task should only be run on the overlord domain.
     */
    Overlord,

    /**
     * This task should be run on normal domains.
     */
    Domain,

    /**
     * This task should be run on normal and overlord domains.
     */
    AnyDomain;

    /**
     * Test whether a scope matches this scope.
     * @param scope the scope to test against
     * @return if this scope equals the test scope or this scope matches any
     * domain and the test scope is the overlord or domain scope
     */
    public boolean matches(StartupTaskScope scope) {
        return (this == scope) || ((this == AnyDomain) && (scope != System));
    }
}
