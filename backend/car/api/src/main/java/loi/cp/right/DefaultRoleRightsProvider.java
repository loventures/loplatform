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
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import loi.cp.role.RoleService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Set;

/**
 * This is the default implementation of rights, and is the logic used in most cases.
 *
 * This queries for a SupportedRole matching 'role' within a context (i.e. domains/courses), grabs the rights assigned
 * to that role, and expands them using the Rights hierarchy.
 *
 *
 * i.e. Within a course, 'role' might bind to @role-trial-learner and have
 * [
 *  "loi.cp.course.right.LearnCourseRight",
 *  "-loi.cp.course.right.FullContentRight"
 * ]
 * This code will expand LearnCourseRight but restrict FullContentRight, giving
 * [
 *  "loi.cp.course.right.TrialContentRight"
 *  "loi.cp.course.right.ReadCourseRight"
 *  "loi.cp.course.right.CourseParticipationRight"
 *  "loi.cp.course.right.InteractCourseRight"
 *  "loi.cp.course.right.LearnCourseRight"
 * ]
 * as the final set of rights provided for the user.
 */
@Component
public class DefaultRoleRightsProvider extends AbstractComponent implements RoleRightsProvider {

    @Inject
    RightService _rightService;

    @Inject
    RoleService _roleService;

    public Set<Class<? extends Right>> getRoleRights(Id context, @Nullable Id role) {
        return _rightService.expandRightIds(_roleService.getRightInfoForRole(context, role));
    }
}
