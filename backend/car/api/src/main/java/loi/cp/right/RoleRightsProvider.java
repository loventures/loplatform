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
import com.learningobjects.cpxp.component.ComponentInterface;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * This allows a context (specifically, courses) to provide additional logic against the rights returned for that
 * context. RightsService will call out to providers before returning any user rights.
 *
 * i.e., a course could implement this to define it's own logic for how Rights are given following the passing of that
 * course's end date
 *
 * If a course/context does not implement this, then {@Link DefaultRoleRightsProvider} will be used
 */
public interface RoleRightsProvider extends ComponentInterface {

    /**
     * Return the desired Rights for this role in this context
     *
     * @param context The context to express rights within (generally domain / course)
     * @param role    The role to check rights for (i.e. SupportedRole - @role-student)
     * @return the full list of rights available to that role
     */
    Set<Class<? extends Right>> getRoleRights(Id context, @Nullable Id role);
}
