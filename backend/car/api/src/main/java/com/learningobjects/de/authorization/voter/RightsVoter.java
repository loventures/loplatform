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

package com.learningobjects.de.authorization.voter;

import com.learningobjects.de.authorization.CollectedRights;
import com.learningobjects.de.authorization.SecurityContext;
import loi.cp.right.Right;
import loi.cp.right.RightMatch;

/**
 * Votes yes if the {@link com.learningobjects.de.authorization.SecurityContext} has the voter's right.
 */
public class RightsVoter implements AccessDecisionVoter {
    // TODO: should this expand the right hierarchy?
    private final Class<? extends Right> right;
    private final RightMatch match;

    public RightsVoter(final Class<? extends Right> right, final RightMatch match) {
        this.right = right;
        this.match = match;
    }

    @Override
    public boolean vote(final SecurityContext securityContext) {
        final CollectedRights collectedRights = securityContext.get(CollectedRights.class);
        return collectedRights != null && collectedRights.contains(right, match);
    }
}
