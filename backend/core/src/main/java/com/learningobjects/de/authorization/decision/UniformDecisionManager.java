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

package com.learningobjects.de.authorization.decision;

import com.learningobjects.de.authorization.SecurityContext;

public enum UniformDecisionManager implements AccessDecisionManager{

    ALL_ALLOWED_INSTANCE(true),
    ALL_DENIED_INSTANCE(false);

    private final boolean decision;

    private UniformDecisionManager(final boolean decision) {
        this.decision = decision;
    }

    @Override
    public boolean decide(final SecurityContext securityContext) {
        return decision;
    }
}
