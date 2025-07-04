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
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.query.*;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType;
import com.learningobjects.cpxp.service.item.ItemWebService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.query.QueryService;
import loi.cp.course.CourseComponent;
import loi.cp.user.UserComponent;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RosterWebControllerImpl extends AbstractComponent implements RosterWebController {

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    @Inject
    private ItemWebService _itemWebService;

    @Inject
    private QueryService _queryService;

    // -- Enrollments

    @Override
    public Optional<EnrollmentComponent> getEnrollment(Long courseId, Long enrollmentId) {
        ApiQuery idQuery = ApiQuery.byId(enrollmentId, EnrollmentComponent.class);
        return getEnrollments(courseId, idQuery).toOptional();
    }

    @Override
    public ApiQueryResults<EnrollmentComponent> getEnrollments(Long courseId, ApiQuery query) {
        return ApiQuerySupport.query(queryBuilder(courseId), query, EnrollmentComponent.class);
    }

    @Override
    public EnrollmentComponent addEnrollment(Long courseId, EnrollmentComponent enrollment) {
        RosterEnrollmentRequest req = new RosterEnrollmentRequest();
        req.userId = enrollment.getUserId();
        req.contextId = courseId;
        req.roleId = enrollment.getRoleId();
        req.startTime = enrollment.getStartTime();
        req.stopTime = enrollment.getStopTime();
        addEnrollment(req);
        return enrollment;
    }

    @Override
    public void addEnrollment(RosterEnrollmentRequest enrollmentRequest) {
        _enrollmentWebService.addEnrollments(
          enrollmentRequest.contextId,
          Collections.singleton(enrollmentRequest.roleId),
          enrollmentRequest.userId,
          enrollmentRequest.dataSource,
          enrollmentRequest.startTime,
          enrollmentRequest.stopTime,
          false
        );
    }

    @Override
    public Optional<EnrolledUserComponent> getEnrolledUser(Long courseId, Long id) {
        ApiQuery idQuery = ApiQuery.byId(id);
        return getEnrolledUsers(courseId, idQuery).toOptional();
    }

    @Override
    public ApiQueryResults<EnrolledUserComponent> getEnrolledUsers(final Long courseId, ApiQuery query) {
        // In order to support role filtering on users in a context I need to
        // do some manipulation to transform the role conditions into prefilters on
        // the underlying entitlements. Blargh. I could make this less surprising
        // by making roles a mapping of user, but then the roles of a user would
        // vary depending on how you asked about the user, which could be surprising.
        // This only accepts these predicates as prefilters because the totalCount
        // would be otherwise incorrect.
        ApiQuery.Builder userApiQueryBuilder = new ApiQuery.Builder()
          .setFilterOp(query.getFilterOp())
          .addFilters(query.getFilters())
          .setPage(query.getPage())
          .addPropertyMappings(UserComponent.class)
          .addPropertyMapping("overallGrade", new OverallGradeHandler(courseId, _queryService));
        ApiQuery.Builder enrollmentApiQueryBuilder = new ApiQuery.Builder()
          .addPropertyMappings(EnrollmentComponent.class);
        EnrollmentType enrollmentType = EnrollmentType.ACTIVE_ONLY; // by default we should only show active users.
        for (ApiFilter filter : query.getPrefilters()) {
            if ("includeInactive".equals(filter.getProperty()) && (filter.getOperator() == null) && "".equals(filter.getValue())) {
                enrollmentType = EnrollmentType.ALL;
            } else if (filter.getProperty().startsWith("role")) { // role. and role_id
                enrollmentApiQueryBuilder.addFilter(filter);
            } else {
                userApiQueryBuilder.addPrefilter(filter);
            }
        }

        for (ApiOrder order : query.getOrders()) {
            userApiQueryBuilder.addOrder(order);
        }

        QueryBuilder eqb = _enrollmentWebService.getGroupEnrollmentsQuery(courseId, enrollmentType);
        eqb = ApiQuerySupport.getQueryBuilder(eqb, enrollmentApiQueryBuilder.build());
        QueryBuilder uqb = _enrollmentWebService.getEnrollmentUsersQuery(eqb, enrollmentType);
        ApiQueryResults<UserComponent> users = ApiQuerySupport.query(uqb, userApiQueryBuilder.build(), UserComponent.class);
        if (query.getEmbeds().contains(EnrolledUserComponent.PROPERTY_ROLES)) {
            _enrollmentWebService.preloadActiveUserRoles(users, courseId);
        }
        List<EnrolledUserComponent> enrolledUsers =
          users.stream()
            .map(c -> c.asComponent(EnrolledUserComponent.class, courseId))
            .collect(Collectors.toList());
        return new ApiQueryResults<>(enrolledUsers, users.getFilterCount(), users.getTotalCount());
    }

    @Override
    public void dropEnrolledUsers(Long courseId, List<Long> userIds) {
        userIds.forEach(id -> _enrollmentWebService.removeGroupEnrollmentsFromUser(courseId, id));
    }

    private CourseComponent getCourse() {
        return asComponent(CourseComponent.class);
    }

    @Override
    public ApiQueryResults<EnrolledUserComponent> getEnrolledUsers(Long courseId, List<String> roleNames, ApiQuery query) {
        ApiQuery.Builder filterQuery = ApiQuery.Builder.unmapped(query);
        List<Long> roleIds = roleNames.stream()
          .map(roleName -> _itemWebService.findById(roleName))
          .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            roleIds = Collections.singletonList(_itemWebService.findById(EnrollmentWebService.ROLE_STUDENT_NAME));
        }
        filterQuery.addPrefilter("role", PredicateOperator.IN, roleIds); //TODO: Should I be clearing any other filters on this field first?

        // ensure we don't get pagination collisions (for example, when two students share a first or last name)
        checkAndMaybeAddOrderById(query, filterQuery);

        return getEnrolledUsers(courseId, filterQuery.build());
    }

    private void checkAndMaybeAddOrderById(ApiQuery query, ApiQuery.Builder filterQuery) {
        // first check to see what the existing query uses
        //  - if it already sorts by id, don't do anything
        //  - count whether there's more ASC or DESC, then use the more popular one
        String idProperty = "id";
        List<ApiOrder> orderList = query.getOrders();
        boolean alreadyById = false;
        int ascCount = 0;
        for (ApiOrder order : orderList) {
            if (order.getDirection().equals(OrderDirection.ASC)) {
                ascCount++;
            }
            if (order.getProperty().equals(idProperty)) {
                alreadyById = true;
            }
        }

        OrderDirection idDirection = OrderDirection.DESC;
        // tie on ASC v. DESC goes to DESC
        if (ascCount > (orderList.size() - ascCount)) {
            idDirection = OrderDirection.ASC;
        }

        // add order by id if not present
        if (!alreadyById) {
            filterQuery.addOrder(idProperty, idDirection);
        }
    }

    private QueryBuilder queryBuilder(Long courseId) {
        return _enrollmentWebService
          .getGroupEnrollmentsQuery(courseId, EnrollmentType.ACTIVE_ONLY);
    }
}

