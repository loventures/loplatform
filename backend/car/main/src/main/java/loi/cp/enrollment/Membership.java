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
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.query.QueryBuilder;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class Membership extends AbstractComponent implements MembershipComponent {
    @Instance
    private Long _context;

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    // -- Enrollments

    @Override
    public Optional<EnrollmentComponent> getEnrollment(Long id) {
        return getEnrollments(ApiQuery.byId(id, EnrollmentComponent.class)).toOptional();
    }

    @Override
    public ApiQueryResults<EnrollmentComponent> getEnrollments(ApiQuery query) {
        return ApiQuerySupport.query(queryBuilder(), query, EnrollmentComponent.class);
    }

    private QueryBuilder queryBuilder() {
        return _enrollmentWebService.getUserEnrollmentsQuery(_context, EnrollmentWebService.EnrollmentType.ALL);
    }
}
