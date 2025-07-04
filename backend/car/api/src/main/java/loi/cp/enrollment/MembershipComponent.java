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

import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.MaxLimit;
import com.learningobjects.cpxp.component.annotation.PathVariable;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.Method;

import java.util.Optional;

@Controller(value = "membership", category = Controller.Category.USERS)
public interface MembershipComponent extends ComponentDecorator {
    @RequestMapping(path = "enrollments", method = Method.GET)
    // TODO: @Secured
    ApiQueryResults<EnrollmentComponent> getEnrollments(@MaxLimit(256) ApiQuery query);

    @RequestMapping(path = "enrollments/{id}", method = Method.GET)
    // TODO: @Secured
    Optional<EnrollmentComponent> getEnrollment(@PathVariable("id") Long id);
}
