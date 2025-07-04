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

package loi.cp.enrollment;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.web.Method;
import loi.cp.role.RoleComponent;
import loi.cp.user.UserComponent;

import javax.validation.groups.Default;
import java.util.List;

@Schema("enrolledUser")
public interface EnrolledUserComponent extends ComponentDecorator {
    String PROPERTY_ROLES = "roles";

    @JsonUnwrapped
    @JsonView(Default.class)
    public UserComponent getUser();

    @RequestMapping(path = PROPERTY_ROLES, method = Method.GET)
    public List<RoleComponent> getRoles();

    @RequestMapping(path = "enrollments", method = Method.GET)
    public List<EnrollmentComponent> getEnrollments();

}
