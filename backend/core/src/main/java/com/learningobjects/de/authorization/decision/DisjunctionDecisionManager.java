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
import com.learningobjects.de.authorization.voter.AccessDecisionVoter;

import java.util.List;

/**
 * Requires at least one voter votes yes to grant access.
 */
public class DisjunctionDecisionManager implements AccessDecisionManager {

    private final List<AccessDecisionVoter> voters;

    public DisjunctionDecisionManager(final List<AccessDecisionVoter> voters) {
        this.voters = List.copyOf(voters);
    }

    @Override
    public boolean decide(final SecurityContext securityContext) {
        for (final AccessDecisionVoter voter : voters) {
            if (voter.vote(securityContext)) {
                return true;
            }
        }
        return false;
    }

}
