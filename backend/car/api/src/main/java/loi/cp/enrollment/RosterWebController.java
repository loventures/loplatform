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

import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.authorization.SecuredAdvice;
import com.learningobjects.de.web.Deletable;
import loi.cp.admin.right.CourseAdminRight;
import loi.cp.course.right.CourseRosterRight;
import loi.cp.course.right.ManageCoursesReadRight;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service // but shouldn't be... TODO: separate the Webbish and Servicey parts of this
@Controller(value = "roster", root = true, category = Controller.Category.CONTEXTS)
public interface RosterWebController extends ApiRootComponent {

    class RosterEnrollmentRequest {
        public Long userId;
        public Long contextId;
        public Long roleId;
        public Date startTime;
        public Date stopTime;
        public String dataSource = "Enrollment";
    }

    @RequestMapping(path = "contexts/{courseId}/roster/{userId}", method = Method.GET)
    @Secured({ CourseRosterRight.class, CourseAdminRight.class, ManageCoursesReadRight.class })
    Optional<EnrolledUserComponent> getEnrolledUser(
      @PathVariable("courseId") @SecuredAdvice Long courseId,
      @PathVariable("userId")                  Long userId
    );

    @RequestMapping(path = "contexts/{courseId}/roster", method = Method.GET)
    @Secured({ CourseRosterRight.class, CourseAdminRight.class, ManageCoursesReadRight.class })
    ApiQueryResults<EnrolledUserComponent> getEnrolledUsers(
      @PathVariable("courseId") @SecuredAdvice Long courseId,
      @MaxLimit(256)                           ApiQuery query
    );

    @RequestMapping(path = "contexts/{courseId}/roster", method = Method.DELETE)
    @Secured({ CourseRosterRight.class, CourseAdminRight.class })
    void dropEnrolledUsers(
            @PathVariable("courseId") @SecuredAdvice Long courseId,
            @QueryParam("userId") List<Long> userIds
    );

    ApiQueryResults<EnrolledUserComponent> getEnrolledUsers(
      Long courseId,
      List<String> roleNames,
      ApiQuery query
    );

    @RequestMapping(path = "contexts/{courseId}/roster/enrollments", method = Method.POST)
    @Secured(value = CourseAdminRight.class)
    EnrollmentComponent addEnrollment(
      @PathVariable("courseId") @SecuredAdvice Long courseId,
      @RequestBody                             EnrollmentComponent enrollment
    );

    void addEnrollment(RosterEnrollmentRequest enrollmentRequest);

    @RequestMapping(path = "contexts/{courseId}/roster/enrollments", method = Method.GET)
    @Secured({ CourseRosterRight.class, CourseAdminRight.class, ManageCoursesReadRight.class })
    ApiQueryResults<EnrollmentComponent> getEnrollments(
      @PathVariable("courseId") @SecuredAdvice Long courseId,
      @MaxLimit(256)                           ApiQuery query
    );

    // @Patchable
    @Deletable
    @RequestMapping(path = "contexts/{courseId}/roster/enrollments/{enrollmentId}", method = Method.GET)
    @Secured({ CourseRosterRight.class, CourseAdminRight.class, ManageCoursesReadRight.class })
    Optional<EnrollmentComponent> getEnrollment(
      @PathVariable("courseId") @SecuredAdvice Long courseId,
      @PathVariable("enrollmentId")            Long id
    );
}
