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

package loi.cp.right;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants;
import com.learningobjects.cpxp.util.cache.Entry;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RightEntry extends Entry<Pair<Long, Long>, Set<Class<? extends Right>>> {

    RightEntry(Set<Class<? extends Right>> rights, Id user, Id context, List<? extends Id> supportedRoleIds) {
        super(
          Pair.of(user.getId(), context.getId()),
          rights,
          invalidationKeys(context.getId(), user.getId(), supportedRoleIds)
        );

    }

    private static Set<String> invalidationKeys( // gah, superconstructor call must be first statement...
      Long contextId,
      Long userId,
      List<? extends Id> supportedRoleIds
    ) {
        Set<String> keys = new HashSet<>(Arrays.asList(
          EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + contextId,
          EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + userId
        ));
        supportedRoleIds.forEach(id -> keys.add(id.getId().toString()));
        return keys;
    }
}
