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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Init;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.de.web.jackson.SerializeAsSubtype;
import loi.cp.role.RoleComponent;
import loi.cp.user.UserComponent;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EnrolledUser extends AbstractComponent implements EnrolledUserComponent {
    @Instance
    private UserComponent _self;

    @Init
    private Long _context;

    @Inject
    private EnrollmentWebService enrollmentWebService;

    @Override
    public UserComponent getUser() {
        return ClassUtils.mark(_self, SerializeAsSubtype.class);
    }

    @Override
    public List<RoleComponent> getRoles() {
        return enrollmentWebService.getActiveUserRoles(_self.getId(), _context).stream().map(ComponentSupport.toComponent(RoleComponent.class)).collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentComponent> getEnrollments() {
        List<EnrollmentFacade> facades = enrollmentWebService.getUserEnrollments(_self.getId(),
          EnrollmentWebService.EnrollmentType.ACTIVE_ONLY);
        return facades.stream().map(ComponentSupport.toComponent(EnrollmentComponent.class)).collect(Collectors.toList());
    }

}
