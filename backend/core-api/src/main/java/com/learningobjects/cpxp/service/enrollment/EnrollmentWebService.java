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

package com.learningobjects.cpxp.service.enrollment;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.service.group.GroupFacade;
import com.learningobjects.cpxp.service.query.Projection;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.UserFacade;
import scaloi.GetOrCreate;

import javax.ejb.Local;
import java.util.*;


@Local
public interface EnrollmentWebService {
    // these are global ids, not names OR roleIds
    // they are used as supported-role id-strings
    String ROLE_STUDENT_NAME = "role-student";
    String ROLE_STAFF_NAME = "role-staff";
    String ROLE_GUEST_NAME = "role-guest";
    String ROLE_HOSTING_ADMIN_NAME = "role-hostingAdmin";
    String ROLE_HOSTING_STAFF_NAME = "role-hostingStaff";
    String ROLE_HOSTING_SUPPORT_NAME = "role-hostingSupport";
    String ROLE_ADMINISTRATOR_NAME = "role-administrator";
    String ROLE_INSTRUCTOR_NAME = "role-instructor";
    String ROLE_ADVISOR_NAME = "role-advisor";
    String ROLE_TRIAL_LEARNER_NAME = "role-trial-learner";
    String ROLE_FACULTY_NAME = "role-faculty";
    String ROLE_PROVISIONING_ADMIN_NAME = "role-provisioningAdmin";

    // the roleId values. Role items are basically just these role-id strings, and are
    // basically useless on their own. Roles become not useless when a group/domain
    // supports the role, by having a child supported-role item. A user can be enrolled
    // into any supported role of the group/domain. A supported role has rights.
    String ADMINISTRATOR_ROLE_ID = "administrator";
    String TRIAL_LEARNER_ROLE_ID = "trialLearner";
    String STAFF_ROLE_ID = "staff";
    String GUEST_ROLE_ID = "guest";
    String STUDENT_ROLE_ID = "student";
    String ADVISOR_ROLE_ID = "advisor";
    String INSTRUCTOR_ROLE_ID = "instructor";
    String FACULTY_ROLE_ID = "faculty";
    String HOSTING_ADMIN_ROLE_ID = "hostingAdmin";
    String HOSTING_STAFF_ROLE_ID = "hostingStaff";
    String HOSTING_SUPPORT_ROLE_ID = "hostingSupport";
    String PROVISIONING_ADMIN_ROLE_ID = "provisioningAdmin";
    String MANAGER_ROLE_ID = "manager";
    String MEMBER_ROLE_ID = "member";
    String LEADER_ROLE_ID = "leader";
    String TEACHING_ASSISTANT_ROLE_ID = "teachingAssistant";
    String INSTRUCTIONAL_DESIGNER_ROLE_ID = "instructionalDesigner";
    String USER_ROLE_ID = "user";
    String COURSE_CREATOR_ROLE_ID = "courseCreator";
    String COLLEAGUE_ROLE_ID = "colleague";
    String GRADER_ROLE_ID = "grader";
    String TUTOR_ROLE_ID = "tutor";
    String DEPARTMENTAL_CHAIR_ROLE_ID = "departmentalChair";
    String FRIEND_ROLE_ID = "friend";
    String PROFESSOR_ROLE_ID = "professor";
    String TEACHER_ROLE_ID = "teacher";
    String AUDITOR_ROLE_ID = "auditor";
    String ASSISTANT_ROLE_ID = "assistant";
    String COURSE_BUILDER_ROLE_ID = "courseBuilder";
    String ORGANIZATIONAL_BUILDER_ROLE_ID = "organizationBuilder";

    List<String> ROLE_IDS = Arrays.asList(ADMINISTRATOR_ROLE_ID, TRIAL_LEARNER_ROLE_ID,
      STAFF_ROLE_ID, GUEST_ROLE_ID, STUDENT_ROLE_ID, ADVISOR_ROLE_ID, INSTRUCTOR_ROLE_ID, FACULTY_ROLE_ID,
      HOSTING_ADMIN_ROLE_ID, HOSTING_STAFF_ROLE_ID, HOSTING_SUPPORT_ROLE_ID, PROVISIONING_ADMIN_ROLE_ID,
      MANAGER_ROLE_ID, LEADER_ROLE_ID, TEACHING_ASSISTANT_ROLE_ID, INSTRUCTIONAL_DESIGNER_ROLE_ID, USER_ROLE_ID,
      MEMBER_ROLE_ID, COURSE_CREATOR_ROLE_ID, COLLEAGUE_ROLE_ID, GRADER_ROLE_ID, TUTOR_ROLE_ID,
      DEPARTMENTAL_CHAIR_ROLE_ID, FRIEND_ROLE_ID, PROFESSOR_ROLE_ID, TEACHER_ROLE_ID, AUDITOR_ROLE_ID,
      ASSISTANT_ROLE_ID, COURSE_BUILDER_ROLE_ID, ORGANIZATIONAL_BUILDER_ROLE_ID);


    /**
     * Used to optionally filter enrollments based on whether they're disabled
     * or not.
     */
    enum EnrollmentType {
        /** All enrollments including disabled ones. */
        ALL,
        /** Does not include disabled enrollments. */
        ACTIVE_ONLY;
    }

    /**
     * @param groupId The group
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return The enrollments for specified group
     */
    List<EnrollmentFacade> getGroupEnrollments(Long groupId, EnrollmentType enrollmentType);

    /**
     * @param userId The user
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return The enrollments for specified user
     */
    List<EnrollmentFacade> getUserEnrollments(Long userId, EnrollmentType enrollmentType);

    /**
     * @param userId The user
     * @param groupId The group
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return The enrollments for specified user and group
     */
    List<EnrollmentFacade> getUserEnrollments(Long userId, Long groupId, EnrollmentType enrollmentType);

    default List<EnrollmentFacade> getUserEnrollments(long userId, long groupId) {
        return getUserEnrollments(userId, groupId, EnrollmentType.ACTIVE_ONLY);
    }

    EnrollmentFacade addEnrollment(Long userId, Long groupId);

    /**
     * Adds an enrollment if there is not an existing enrollment with the same role as the one that exists.
     * @param userId the id of the user which will get this enrollment
     * @param groupId the id of the group which this enrollment is for
     * @param roleId the id of the role for the new enrollment
     * @return Empty if an existing enrollment exists in the group for the user with the given role & it is disabled.
     */
    Optional<EnrollmentFacade> addEnrollmentIfNotDisabled(Long userId, Long groupId, Long roleId);

    Long createEnrollment(Long groupId, Long roleId, Long userId, String dataSource);

    Long createEnrollment(Long groupId, Long roleId, Long userId, String dataSource, Boolean disabled);

    void removeEnrollment(Long enrollment);

    void addEnrollments(Long groupId, Collection<Long> roleIds, Long userId, String dataSource, Date startTime, Date endTime, Boolean disabled);

    void setEnrollments(Long groupId, Collection<Long> roleIds, Long userId, String dataSource, Date startTime, Date endTime, Boolean disabled);

    SetEnrollmentDto setEnrollment(Long groupId, Long roleId, Long userId, String dataSource);

    SetEnrollmentDto setEnrollment(Long groupId, Long roleId, Long userId, String dataSource, Date startTime, Date endTime);

    void removeEnrollment(Long groupId, Long roleId, Long userId, String dataSource);

    List<Long> removeGroupEnrollmentsFromUser(Long groupId, Long userId);

    /**
     * Remove all enrollments owned by (the item with ID) [[ownerId]].
     *
     * `ownerId` is usually a group, but not always (e.g., domain).
     */
    void removeAllEnrollments(Long ownerId);

    void invalidateEnrollment(EnrollmentFacade enrollment);

    void invalidateUserMembership(Long userId);

    void invalidateGroupMembership(Long groupId);

    // This removes any enrollments for a role not in the collection, from another data source
    // data source, and adds new enrollments under the system data source. This
    // accepts existing enrollments even if disabled or invalid.
    void setEnrollment(Long groupId, Collection<Long> roleIds, Long userId);

    Long setSingleEnrollment(Long groupId, Long roleId, Long userId, String dataSource);

    long countEnrollmentsInAllGroups();

    long countEnrollmentsInAllGroups(Long domainId);

    /* Preview as X */

    void previewGroupAsRoles(Long groupId, List<Long> roleIds);

    void exitPreview(Long groupId);

    List<Long> getPreviewRoles(Long groupId);

    /* Old methods - to be massaged */

    /**
     * @param groupId The group
     * @param roleId The role. If null, all roles included.
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return The enrollments for specified group and role.
     */
    List<UserFacade> getGroupUsersByRole(Long groupId, Long roleId, EnrollmentType enrollmentType);

    /**
     * @param groupId The group
     * @param roleId The role. If null, all roles included.
     * @return The user ids for users by the specified group/role who have
     * <b>non-disabled</b> enrollments.
     */
    List<Long> getGroupActiveUserIdsByRole(Long groupId, Long roleId);

    /**
     * @param groupId The group
     * @param roleName The role. If null, all roles included.
     * @return The user ids for users by the specified group/role who have
     * <b>non-disabled</b> enrollments.
     */
    List<Long> getGroupActiveUserIdsByRoleName(Long groupId, String roleName);


    /**
     * Returns a query builder over the enrollments in the group.
     * @param groupId The group
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return A query builder of enrollments for specified group.
     */
    QueryBuilder getGroupEnrollmentsQuery(Long groupId, EnrollmentType enrollmentType);

    /**
     * Returns a query builder over the users in a query of enrollment records.
     * @param qb A enrollment query builder
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return A query builder of users.
     */
    QueryBuilder getEnrollmentUsersQuery(QueryBuilder qb, EnrollmentType enrollmentType);

    /**
     * @param enrollmentType The type of enrollment to query.
     * @return a query builder for enrollments.
     */
    QueryBuilder getEnrollmentsQuery(EnrollmentType enrollmentType);

    /**
     * @param userId The user
     * @param enrollmentType Whether to include all enrollments or only active ones
     * @return The enrollments for specified user
     */
    QueryBuilder getUserEnrollmentsQuery(Long userId, EnrollmentType enrollmentType);

    /**
     * @param userId The user
     * @return A query builder for the groups in which the specified user has
     * <b>non-disabled</b> enrollments.
     */
    QueryBuilder getActiveUserGroupsQuery(Long userId);

    /**
     * @param groupIds The courses to search in
     * @param userId The user
     * @param roleName The user's role
     * @return The subset of the courseIds specified in which the specified user
     * has an an active <b>non-disabled</b> enrollment using the specified role.
     */
    Set<Long> searchGroupEnrollments(Set<Long> groupIds, long userId, String roleName);

    /**
     * @param userId The user
     * @return The list of groups for which the user has <b>non-disabled</b> enrollments.
     */
    List<GroupFacade> getActiveUserGroups(Long userId);

    /**
     * @param userId The user
     * @param page   The pagination data for the list.
     * @return A paginated list of groups for which the user has <b>non-disabled</b> enrollments.
     */
    List<GroupFacade> getActiveUserGroups(Long userId, final ApiPage page);

    /**
     * @param roleIds The roles to filter on, can be empty
     * @param projection The data to fetch from the query builder
     * @return A query builder for the active users in the current domain.
     */
    QueryBuilder getDomainActiveUsersByRolesQuery(final List<Long> roleIds, final Projection projection);

    /**
     * Get roles for a user within a domain or within a course for users with <b>non-disabled</b> enrollments
     * @param userId the user id
     * @param itemId the domain id or the course id
     * @return a list of roles
     */
    List<RoleFacade> getActiveUserRoles(Long userId, Long itemId);

    /**
     * Get roles for an <b>non-disabled</b> user within a domain or within a course.
     * Use this method instead of {@link #getActiveUserRoles(Long, Long)} when you need to distinguish
     * between roles that this user normally has, and roles which they have because of their preview state.
     * @param userId the user id
     * @param contextId the domain id or the course id
     * @return a RoleInfo object, which contains a user's real roles and preview roles.
     */
    RoleInfo getActiveUserRoleInfo(Long userId, Long contextId);

    void preloadActiveUserRoles(Iterable<? extends Id> users, Long contextId);

    boolean isMember(Long userId, Long itemId);

    boolean isCurrentOrFormerMember(Long userId, Long itemId);

    /**
     * Returns query builder of users with enrollments in a group.
     *
     * @param groupId        The group
     * @param roleId         The role. If null, return users with all roles.
     * @param enrollmentType Whether to query for all enrollments or only active ones.
     * @return A query builder of users enrolled in a group.
     */
    QueryBuilder getGroupUsersQuery(Long groupId, Long roleId, EnrollmentType enrollmentType);

    /**
     * Return the user counts for each group for the specified roles.
     *
     * <b>NOTE:</b> If the requirements for groupIds or roleId are not met, this
     * function will return an empty map.
     *
     * @param groupIds The groups, must be a non-empty set
     * @param roleIds The roles, must not be null
     * @param enrollmentType Whether to query for all enrollments or only active ones.
     * @return A map containing the user counts for the provided roles in the groups specified.
     */
    Map<Long, Integer> getGroupUserCounts(Set<Long> groupIds, Set<Long> roleIds, EnrollmentType enrollmentType);

    /**
     * An encapsulation of information about a user's enrollments in a context.
     */
    class RoleInfo {

        private Long userId;
        private Long contextId;
        private List<RoleFacade> roles;
        private List<RoleFacade> previewRoles;

        public RoleInfo(Long userId, Long contextId, List<RoleFacade> roles, List<RoleFacade> previewRoles) {
            this.userId = userId;
            this.contextId = contextId;
            this.roles = roles;
            this.previewRoles = previewRoles;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getContextId() {
            return contextId;
        }

        public void setContextId(Long contextId) {
            this.contextId = contextId;
        }

        public List<RoleFacade> getRoles() {
            return roles;
        }

        public void setRoles(List<RoleFacade> roles) {
            this.roles = roles;
        }

        public List<RoleFacade> getPreviewRoles() {
            return previewRoles;
        }

        public void setPreviewRoles(List<RoleFacade> previewRoles) {
            this.previewRoles = previewRoles;
        }
    }

}
