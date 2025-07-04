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

import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.authorization.voter.AccessDecisionVoter;
import com.learningobjects.de.authorization.voter.RightsVoter;
import com.learningobjects.de.authorization.voter.UserVoter;
import loi.cp.right.Right;

import java.lang.reflect.Method;
import java.util.*;

public class AccessDecisionManagerFactory {
    public static AccessDecisionManager getAccessDecisionManager(final Secured secured) {
        final List<AccessDecisionVoter> voters = new ArrayList<>();

        for (final Class<? extends Right> right : secured.value()) {
            voters.add(new RightsVoter(right, secured.match()));
        }

        if (secured.byOwner()) {
            voters.add(UserVoter.INSTANCE);
        }

        final AccessDecisionManager adm;

        if (voters.isEmpty()) {
            adm = UniformDecisionManager.ALL_ALLOWED_INSTANCE;
        } else if (secured.conjunction()) {
            adm = new ConjunctionDecisionManager(voters);
        } else {
            adm = new DisjunctionDecisionManager(voters);
        }

        final AccessDecisionManager decoratedDecider;
        if (!secured.allowAnonymous()) {
            decoratedDecider = new AnonymousRejector(adm);
        } else {
            decoratedDecider = adm;
        }

        return decoratedDecider;
    }
}
