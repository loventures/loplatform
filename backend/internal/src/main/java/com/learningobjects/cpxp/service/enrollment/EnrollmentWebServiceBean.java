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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.domain.DomainLimitException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.group.GroupFacade;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.item.ItemWebService;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.service.relationship.RelationshipWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.BooleanUtils;
import scala.Tuple2;
import scala.jdk.javaapi.CollectionConverters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Provider;
import jakarta.persistence.Query;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EnrollmentWebServiceBean extends BasicServiceBean implements EnrollmentWebService {
    private static final Logger logger = Logger.getLogger(EnrollmentWebServiceBean.class.getName());

    /**
     * The facade service.
     */
    @Inject
    private FacadeService _facadeService;

    /**
     * The item service.
     */
    @Inject
    private ItemService _itemService;

    /**
     * The item web service.
     */
    @Inject
    private ItemWebService _itemWebService;

    /**
     * The relationship web service.
     */
    @Inject
    private RelationshipWebService _relationshipWebService;

    @Inject
    private Provider<HttpServletRequest> _request;

    @Inject
    private RoleCache _roleCache;

    @Override
    public void invalidateEnrollment(EnrollmentFacade enrollment) {
        invalidateGroupMembership(enrollment.getGroupId());
        invalidateUserMembership(enrollment.getParentId());
    }

    public EnrollmentFacade addEnrollment(Long userId, Long groupId) {
        EnrollmentFacade enrollment = _facadeService.addFacade(userId, EnrollmentFacade.class);
        enrollment.setDisabled(false);
        enrollment.setStartTime(DataSupport.MIN_TIME);
        enrollment.setStopTime(DataSupport.MAX_TIME);
        enrollment.setGroupId(groupId);
        enrollment.setCreatedOn(Current.getTime().toInstant());
        return enrollment;
    }

    public Optional<EnrollmentFacade> addEnrollmentIfNotDisabled(Long userId, Long groupId, Long roleId) {
        Optional<EnrollmentFacade> existingDisabledEnrollment = findDisabledEnrollment(groupId, roleId, userId);
        if (existingDisabledEnrollment.isPresent()) {
            return Optional.empty();
        } else {
            EnrollmentFacade enrollment = addEnrollment(userId, groupId);
            enrollment.setRoleId(roleId);
            return Optional.of(enrollment);
        }
    }

    private Item getEnrollmentItem(Long id) {
        return _itemService.get(id, EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
    }

    public void removeEnrollment(Long enrollmentId) {
        removeEnrollment(getEnrollmentItem(enrollmentId));
    }

    private void removeEnrollment(Item enrollment) {
        Item group = DataTransfer.getItemData(enrollment, EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP);
        if (group != null) {
            // checkPermission(group, PERMISSION_MANAGE);
            invalidateGroupMembership(group.getId());
        }
        invalidateUserMembership(enrollment.getParent().getId());
        _itemService.delete(enrollment);
    }

    @Override
    public List<Long> removeGroupEnrollmentsFromUser(Long groupId, Long userId) {
        List<Item> enrollmentItems = findEnrollmentItems(groupId, null, userId, null, null, null, EnrollmentType.ALL);
        for (Item enrollment : enrollmentItems) {
            removeEnrollment(enrollment);
        }
        return enrollmentItems.stream().map(Item::getId).collect(Collectors.toList());
    }

    @Override
    public void removeAllEnrollments(Long ownerId) {
        Item owner = _itemService.get(ownerId);

        Query query = createQuery("UPDATE EnrollmentFinder SET del = :del WHERE group = :owner");
        query.setParameter("del", Current.deleteGuid());
        query.setParameter("owner", owner);
        query.executeUpdate();
    }

    /**
     * Integration. All fields must be set
     */
    public void addEnrollments(Long groupId, Collection<Long> roleIds, Long userId, String dataSource, Date startTime, Date endTime, Boolean disabled) {
        updateEnrollments(groupId, roleIds, userId, dataSource, startTime, endTime, disabled, false);
    }

    /**
     * Integration. All fields must be set.
     */
    public void setEnrollments(Long groupId, Collection<Long> roleIds, Long userId, String dataSource, Date startTime, Date endTime, Boolean disabled) {

        updateEnrollments(groupId, roleIds, userId, dataSource, startTime, endTime, disabled, true);

    }

    private void updateEnrollments(Long groupId, Collection<Long> roleIds, Long userId, String dataSource, Date startTime, Date endTime, Boolean disabled, boolean isSet) {
        List<EnrollmentFacade> enrollments = findEnrollments(groupId, null, userId, null, null, null, EnrollmentType.ALL);
        Set<Long> addRoles = new HashSet<>(roleIds);
        List<Long> removeIds = new ArrayList<>();

        if (isSet) { // if isSet is true, this is an update operation.  otherwise, treat everything as additive
            for (EnrollmentFacade enrollment : enrollments) {
                Long role = enrollment.getRoleId();
                if (addRoles.remove(role)) {
                    enrollment.setStartTime(DataSupport.defaultToMinimal(startTime));
                    enrollment.setStopTime(DataSupport.defaultToMaximal(endTime));
                    enrollment.setDisabled(BooleanUtils.isTrue(disabled));
                } else if (!groupId.equals(_itemWebService.getParentId(role))) { // local role == team membership => leave it alone
                    removeIds.add(enrollment.getId());
                }
            }
        }

        int delta = addRoles.size() - removeIds.size();
        if (delta > 0) {
            checkEnrollmentLimit(groupId, delta);
        }

        for (Long roleId : addRoles) {
            createEnrollment(groupId, roleId, userId, dataSource, startTime, endTime, disabled);
        }
        removeEnrollmentIds(removeIds);

        invalidateGroupAndUserMembership(groupId, userId);
    }

    /**
     * Integration. Fields roleId, userId and datasource may be null.
     */
    public void removeEnrollment(Long groupId, Long roleId, Long userId, String dataSource) {

        List<Item> enrollments = findEnrollmentItems(groupId, roleId, userId, dataSource, null, null, EnrollmentType.ALL);
        removeEnrollments(enrollments);

        invalidateGroupAndUserMembership(groupId, userId);

    }

    @Override
    public void invalidateUserMembership(@Nullable Long userId) {
        if (userId != null) {
            invalidateQuery(EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + userId);
        }
    }

    @Override
    public void invalidateGroupMembership(@Nullable Long groupId) {
        if (groupId != null) {
            invalidateQuery(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + groupId);
        }
    }

    private void invalidateGroupAndUserMembership(@Nullable Long groupId, @Nullable Long userId) {
        invalidateGroupMembership(groupId);
        invalidateUserMembership(userId);
    }

    private Optional<EnrollmentFacade> findDisabledEnrollment(Long groupId, Long roleId, Long userId) {
        List<EnrollmentFacade> existingEnrollments = findEnrollments(groupId, roleId, userId, null, null, null, EnrollmentType.ALL);
        return existingEnrollments
          .stream()
          .filter(EnrollmentFacade::getDisabled)
          .findFirst();
    }

    /**
     * Integration. Field datasource may be null.
     */
    public SetEnrollmentDto setEnrollment(Long groupId, Long roleId, Long userId, String dataSource) {
        return setEnrollment(groupId, roleId, userId, dataSource, null, null);
    }

    public SetEnrollmentDto setEnrollment(Long groupId, Long roleId, Long userId, String dataSource, Date startTime, Date endTime) {

        List<Item> enrollments = findEnrollmentItems(groupId, null, userId, dataSource, null, null, EnrollmentType.ALL);
        removeEnrollments(enrollments);

        // invalidate done by create
        Long createdId = createEnrollment(groupId, roleId, userId, dataSource, startTime, endTime, false);

        List<Long> removeIds = enrollments.stream().map(Item::getId).collect(Collectors.toList());
        return SetEnrollmentDto$.MODULE$.javaApply(removeIds, createdId);
    }

    private void removeEnrollments(List<Item> enrollments) {
        if (!enrollments.isEmpty()) {
            // lock(getFirst(enrollments).getParent());
            for (Item enrollment : enrollments) {
                removeEnrollment(enrollment);
            }
        }
    }

    /**
     * UI.
     */
    public void setEnrollment(Long groupId, Collection<Long> roleIds, Long userId) {
        List<Long> removeIds = new ArrayList<>();
        Set<Long> targetRoles = (roleIds != null) ? new HashSet<>(roleIds) : new HashSet<>();
        Set<Long> addRoles = new HashSet<>(targetRoles);
        List<EnrollmentFacade> enrollments = findEnrollments(groupId, null, userId, null, null, null, EnrollmentType.ALL);
        for (EnrollmentFacade enrollment : enrollments) {
            if (targetRoles.contains(enrollment.getRoleId())) {
                addRoles.remove(enrollment.getRoleId());
            } else {
                removeIds.add(enrollment.getId());
            }
        }

        int delta = addRoles.size() - removeIds.size();
        if (delta > 0) {
            checkEnrollmentLimit(groupId, delta);
        }

        removeEnrollmentIds(removeIds);
        for (Long roleId : addRoles) {
            createEnrollment(groupId, roleId, userId, IntegrationConstants.DATA_SOURCE_SYSTEM);
        }

        invalidateGroupAndUserMembership(groupId, userId);

    }

    @Override
    public Long setSingleEnrollment(Long groupId, Long roleId, Long userId, String dataSource) {
        var enrolments = findEnrollments(groupId, null, userId, null, null, null, EnrollmentType.ALL);
        var enrolment = enrolments
          .stream()
          .findFirst()
          .map(e -> {
              e.setDisabled(false);
              e.setRoleId(roleId);
              e.setStartTime(DataSupport.MIN_TIME);
              e.setStopTime(DataSupport.MAX_TIME);
              return e.getId();
          })
          .orElseGet(() -> createEnrollment(groupId, roleId, userId, dataSource));
        enrolments.stream().skip(1).forEach(Facade::delete);
        invalidateGroupMembership(groupId);
        invalidateUserMembership(userId);
        return enrolment;
    }


    private void checkEnrollmentLimit(Long groupId, int delta) {
        long maxEnrollments = NumberUtils.longValue(Current.getDomainDTO().getEnrollmentLimit());
        if (maxEnrollments > 0) {
            long enrollmentsInAllGroups = countEnrollmentsInAllGroups();
            if (enrollmentsInAllGroups + delta > maxEnrollments) {
                throw new DomainLimitException(DomainLimitException.Limit.enrollmentLimit, enrollmentsInAllGroups + delta, maxEnrollments);
            }
        }
        long maxEnrollsInGroup = NumberUtils.longValue(Current.getDomainDTO().getMembershipLimit());
        if (!groupId.equals(Current.getDomain()) && (maxEnrollsInGroup > 0)) {
            long groupEnrollments = countGroupEnrollments(groupId);
            if (groupEnrollments + delta > maxEnrollsInGroup) {
                throw new DomainLimitException(DomainLimitException.Limit.memberLimit, groupEnrollments + delta, maxEnrollsInGroup);
            }
        }
    }

    public Long createEnrollment(Long groupId, Long roleId, Long userId, String dataSource) {
        return createEnrollment(groupId, roleId, userId, dataSource, Boolean.FALSE);
    }

    public Long createEnrollment(Long groupId, Long roleId, Long userId, String dataSource, Boolean disabled) {
        return createEnrollment(groupId, roleId, userId, dataSource, null, null, disabled);
    }

    private Long createEnrollment(Long groupId, Long roleId, Long userId, String dataSource, Date startTime, Date endTime, Boolean disabled) {
        EnrollmentFacade enrollment = addEnrollment(userId, groupId);
        enrollment.setRoleId(roleId);
        enrollment.setDataSource(dataSource);
        enrollment.setStartTime(DataSupport.defaultToMinimal(startTime));
        enrollment.setStopTime(DataSupport.defaultToMaximal(endTime));
        enrollment.setDisabled(BooleanUtils.isTrue(disabled));
        invalidateEnrollment(enrollment);
        return enrollment.getId();
    }

    private void removeEnrollmentIds(List<Long> enrollments) {
        List<Item> enrollmentItems = new ArrayList<>();
        for (Long enrollmentId : enrollments) {
            Item enrollment = getEnrollmentItem(enrollmentId);
            enrollmentItems.add(enrollment);
        }
        if (!enrollmentItems.isEmpty()) {
            removeEnrollments(enrollmentItems);
        }
    }

    public long countEnrollmentsInAllGroups() {
        return countEnrollmentsInAllGroups(Current.getDomain());
    }

    public long countEnrollmentsInAllGroups(Long domainId) {
        Item domain = _itemService.get(domainId);
        QueryBuilder enrollments = queryRoot(domain, EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
        Long result = enrollments.getAggregateResult(Function.COUNT);
        return NumberUtils.longValue(result);
    }

    public List<EnrollmentFacade> getGroupEnrollments(Long groupId, EnrollmentType enrollmentType) {
        return findEnrollments(groupId, null, null, null, null, null, enrollmentType);
    }


    public List<EnrollmentFacade> getUserEnrollments(Long userId, EnrollmentType enrollmentType) {
        return findEnrollments(null, null, userId, null, null, null, enrollmentType);
    }


    public List<EnrollmentFacade> getUserEnrollments(Long userId, Long groupId, EnrollmentType enrollmentType) {
        return findEnrollments(groupId, null, userId, null, null, null, enrollmentType);
    }

    private long countGroupEnrollments(Long groupId) {

        Item group = _itemService.get(groupId);

        // TODO: cache this one way or another
        QueryBuilder enrollments = queryBuilder();
        enrollments.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + groupId);
        enrollments.setImplicitRoot(group.getRoot());
        enrollments.setItemType(EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
        enrollments.addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, "eq", groupId);
        Long result = enrollments.getAggregateResult(Function.COUNT);

        return NumberUtils.longValue(result);
    }

    private List<EnrollmentFacade> findEnrollments(Long groupId, Long roleId, Long userId, String dataSource, Integer limit, Integer offset, EnrollmentType enrollmentType) {
        return queryEnrollments(groupId, roleId, userId, dataSource, limit, offset, enrollmentType)
          .getFacadeList(EnrollmentFacade.class);
    }

    private List<Item> findEnrollmentItems(Long groupId, Long roleId, Long userId, String dataSource, Integer limit, Integer offset, EnrollmentType enrollmentType) {
        return queryEnrollments(groupId, roleId, userId, dataSource, limit, offset, enrollmentType).getItems();
    }

    @Override
    public QueryBuilder getEnrollmentsQuery(EnrollmentType enrollmentType) {
        QueryBuilder qb = querySystem(EnrollmentConstants.ITEM_TYPE_ENROLLMENT); // seems dangerous; not scoped to a domain.
        if (enrollmentType == EnrollmentType.ACTIVE_ONLY) {
            includeActiveEnrollmentsOnly(qb);
        }
        return qb;
    }

    private QueryBuilder queryEnrollments(Long groupId, Long roleId, Long userId, String dataSource, Integer limit, Integer offset, EnrollmentType enrollmentType) {
        QueryBuilder qb;
        if (userId != null) {
            Item user = _itemService.get(userId);
            qb = queryParent(user, EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
            qb.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + userId);
        } else {
            qb = querySystem(EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
            if ((groupId == null) && (roleId == null)) {
                qb.setRoot(getCurrentDomain());
            } else {
                qb.setImplicitRoot(getCurrentDomain());
            }
        }
        if (groupId != null) {
            qb.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + groupId);
            qb.addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, "eq", groupId);
        }
        if (userId == null && groupId == null) {
            qb.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + getCurrentDomain().getId());
        }
        if (roleId != null) {
            qb.addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, "eq", roleId);
        }
        if (dataSource != null) {
            qb.addCondition(IntegrationConstants.DATA_TYPE_DATA_SOURCE, "eq", dataSource);
        }
        if (enrollmentType == EnrollmentType.ACTIVE_ONLY) {
            includeActiveEnrollmentsOnly(qb);
        }

        if (limit != null && offset != null) {
            qb.setFirstResult(offset.intValue());
            qb.setLimit(limit.intValue());
        }
        return qb;
    }

    public QueryBuilder getDomainActiveUsersByRolesQuery(final List<Long> roleIds, final Projection projection) {
        final QueryBuilder qb = queryRoot(getCurrentDomain(), EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
        qb.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + getCurrentDomain().getId());
        qb.addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, "eq", getCurrentDomain().getId());
        qb.setDistinct(true);
        qb.setProjection(projection);
        includeActiveEnrollmentsOnly(qb);

        qb.addDisjunction0(roleIds.stream()
          .map(r -> BaseCondition.getInstance(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, "eq", r))
          .collect(Collectors.toList()));

        return qb;
    }

    @Override
    public QueryBuilder getGroupEnrollmentsQuery(Long groupId, EnrollmentType enrollmentType) {
        return queryEnrollments(groupId, null, null, null, null, null, enrollmentType);
    }

    @Override
    public QueryBuilder getEnrollmentUsersQuery(QueryBuilder qb, EnrollmentType enrollmentType) {
        qb.setProjection(Projection.PARENT_ID);
        final QueryBuilder uqb = querySystem(UserConstants.ITEM_TYPE_USER);
        if (enrollmentType == EnrollmentType.ACTIVE_ONLY) {
            includeActiveEnrollmentsOnly(qb);
        }
        uqb.setImplicitRoot(getCurrentDomain());
        uqb.addInitialQuery(qb);
        return uqb;
    }

    @Override
    public QueryBuilder getGroupUsersQuery(@Nonnull Long groupId, Long roleId, EnrollmentType enrollmentType) {
        final QueryBuilder qb = queryEnrollments(groupId, roleId, null, null, null, null, enrollmentType);
        return getEnrollmentUsersQuery(qb, enrollmentType);
    }

    @Override
    public Map<Long, Integer> getGroupUserCounts(Set<Long> groupIds, Set<Long> roleIds, EnrollmentType enrollmentType) {
        if (groupIds == null || groupIds.isEmpty() || roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> res = new HashMap<>();
        getEnrollmentsQuery(enrollmentType)
          .addCondition(BaseCondition.inIterable(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, roleIds))
          .addCondition(BaseCondition.inIterable(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, groupIds))
          .setDataProjection(BaseDataProjection.ofAggregateData(DataTypes.META_DATA_TYPE_ID, Function.COUNT))
          .setGroup(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP)
          .<Object[]>getResultList()
          .forEach(arr -> res.put((Long) arr[1], ((Long) arr[0]).intValue()));

        Sets.difference(groupIds, res.keySet()).forEach(id -> res.put(id, 0));
        return res;
    }

    @Override
    public List<UserFacade> getGroupUsersByRole(Long groupId, Long roleId, EnrollmentType enrollmentType) {
        final QueryBuilder qb = getGroupUsersQuery(groupId, roleId, enrollmentType);
        return getUserFacadesFromQueryBuilder(qb);
    }

    @Override
    public List<Long> getGroupActiveUserIdsByRole(Long groupId, Long roleId) {
        final QueryBuilder qb = queryEnrollments(groupId, roleId, null, null, null, null, EnrollmentType.ACTIVE_ONLY);
        qb.setDistinct(true);
        qb.setProjection(Projection.PARENT_ID);
        return qb.getResultList();
    }

    @Override
    public List<Long> getGroupActiveUserIdsByRoleName(Long groupId, String roleName) {
        Long roleId = _itemWebService.findById(roleName);
        return getGroupActiveUserIdsByRole(groupId, roleId);
    }

    /**
     * Gets the results from the given QueryBuilder and returns them as a list of UserFacades
     *
     * @param qb The QueryBuilder
     * @return the List of UserFacades
     */
    private List<UserFacade> getUserFacadesFromQueryBuilder(QueryBuilder qb) {
        List<UserFacade> users = new ArrayList<>();
        for (Item user : qb.getItems()) {
            UserFacade facade = _facadeService.getFacade(user, UserFacade.class);
            if (facade != null) {
                users.add(facade);
            }
        }

        return users;
    }

    @Override
    public QueryBuilder getUserEnrollmentsQuery(Long userId, EnrollmentType enrollmentType) {
        return queryEnrollments(null, null, userId, null, null, null, enrollmentType);
    }

    public QueryBuilder getActiveUserGroupsQuery(Long userId) {

        QueryBuilder qb = getUserEnrollmentsQuery(userId, EnrollmentType.ACTIVE_ONLY);
        qb.setDistinct(true);
        qb.setDataProjection(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP);

        return qb;
    }

    @Override
    public Set<Long> searchGroupEnrollments(Set<Long> groupIds, long userId, String roleName) {
        return new HashSet<>(getActiveUserGroupsQuery(userId)
          .addCondition(
            EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE,
            Comparison.eq,
            _itemWebService.findById(roleName))
          .addCondition(
            EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP,
            Comparison.in,
            groupIds)
          .setDataProjection(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP)
          .<Long>getResultList());
    }

    private List<GroupFacade> processUserGroupsQuery(final QueryBuilder qb) {
        final List<Long> groupIds = qb.getResultList();

        final Long currentDomain = getCurrentDomain().getId();
        final List<GroupFacade> groups = new ArrayList<>();
        for (final Long groupId : groupIds) {
            if (!currentDomain.equals(groupId) && (groupId != null)) {
                GroupFacade group = _facadeService.getFacade(groupId, GroupFacade.class);
                if (group != null) {
                    groups.add(group);
                }
            }
        }
        return groups;
    }

    public List<GroupFacade> getActiveUserGroups(Long userId) {
        return getActiveUserGroups(userId, null);
    }

    public List<GroupFacade> getActiveUserGroups(Long userId, final ApiPage page) {
        QueryBuilder qb = getActiveUserGroupsQuery(userId);
        if (page != null) {
            qb.setFirstResult(page.getOffset());
            qb.setLimit(page.getLimit());
        }
        return processUserGroupsQuery(qb);
    }

    @Override
    public void preloadActiveUserRoles(Iterable<? extends Id> users, Long contextId) {
        QueryBuilder rqb = getGroupEnrollmentsQuery(contextId, EnrollmentType.ACTIVE_ONLY);
        rqb.addCondition(BaseCondition.inIterable(DataTypes.META_DATA_TYPE_PARENT_ID, users));
        rqb.setDataProjection(BaseDataProjection.ofData(
          DataTypes.META_DATA_TYPE_PARENT_ID,
          EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE
        ));
        HashMultimap<Long, Long> userRoles = HashMultimap.create();
        for (Object[] userRole : rqb.<Object[]>getResultList()) {
            userRoles.put((Long) userRole[0], (Long) userRole[1]);
        }
        for (Id user : users) {
            QueryBuilder qb = getActiveUserRolesQueryBuilder(user.getId(), contextId);
            Current.put(qb.getCacheKey(), new ArrayList<>(userRoles.get(user.getId())));
        }
    }

    @Override
    public List<RoleFacade> getActiveUserRoles(final Long userId, final Long contextId) {
        if (Current.getUser().equals(userId)) { // hmm... really..
            var previewRoles = getPreviewRoles(contextId);
            if (!previewRoles.isEmpty()) {
                return previewRoles.stream().map(_relationshipWebService::getRole).collect(Collectors.toList());
            }
        }

        // this is getOrCompute but scala cannot infer...
        var roleIds = _roleCache.get(Tuple2.apply(userId, contextId)).getOrElse(() -> {
            var entry = RoleEntry.apply(
              userId,
              contextId,
              CollectionConverters.asScala(getUserEnrollments(userId, contextId, EnrollmentType.ALL)).toList(),
              new Date()
            );
            _roleCache.put(entry);
            return entry.value();
        });

        return CollectionConverters.asJava(roleIds)
          .stream()
          .map(_relationshipWebService::getRole)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    @Override
    public RoleInfo getActiveUserRoleInfo(Long userId, Long contextId) {
        Collection<Long> previewRoles = getPreviewRoles(contextId);
        QueryBuilder qb = getActiveUserRolesQueryBuilder(userId, contextId);
        List<Long> realRoles = qb.getResultList();

        return new RoleInfo(userId, contextId,
          realRoles.stream().map(this::getRole).collect(Collectors.toList()),
          previewRoles.stream().map(this::getRole).collect(Collectors.toList()));
    }

    private RoleFacade getRole(Long id) {
        return _relationshipWebService.getRole(id);
    }

    private QueryBuilder getActiveUserRolesQueryBuilder(Long userId, Long contextId) {
        Item user = _itemService.get(userId);
        QueryBuilder qb = queryParent(user, EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
        qb.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + userId);
        qb.addCondition(BaseCondition.getInstance(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, Comparison.eq, contextId));
        includeActiveEnrollmentsOnly(qb);
        qb.setDistinct(true);
        qb.setDataProjection(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE);
        return qb;
    }

    private void includeActiveEnrollmentsOnly(QueryBuilder qb) {
        qb.addCondition(DataTypes.DATA_TYPE_START_TIME, "le", getApproximateTimeCeiling());
        qb.addCondition(DataTypes.DATA_TYPE_STOP_TIME, "gt", getApproximateTime());
        qb.addCondition(DataTypes.DATA_TYPE_DISABLED, "ne", Boolean.TRUE);
    }

    public boolean isMember(Long userId, Long itemId) {

        return isMember(userId, itemId, true);
    }

    public boolean isCurrentOrFormerMember(Long userId, Long itemId) {

        return isMember(userId, itemId, false);
    }

    private boolean isMember(Long userId, Long itemId, boolean active) {

        Item user = _itemService.get(userId);
        QueryBuilder qb = queryParent(user, EnrollmentConstants.ITEM_TYPE_ENROLLMENT);
        qb.addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + userId);
        qb.addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, "eq", itemId);
        if (active) {
            includeActiveEnrollmentsOnly(qb);
        }
        Long result = qb.getAggregateResult(Function.COUNT);

        return NumberUtils.longValue(result) > 0;
    }

    public void previewGroupAsRoles(Long groupId, List<Long> roleIds) {
        // TODO: look up the group, look up the role, verify that there is such a role in such a group
        ListMultimap<Long, Long> previewRolesMap = getPreviewRolesMap();

        if (previewRolesMap == null) {
            HttpSession session = _request.get().getSession(false);
            if (session == null) {
                return;
            }
            previewRolesMap = ArrayListMultimap.create();
            session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_PREVIEW, previewRolesMap);
        }
        previewRolesMap.putAll(groupId, roleIds);
    }

    public void exitPreview(Long groupId) {
        ListMultimap<Long, Long> previewRoles = getPreviewRolesMap();
        if (previewRoles != null) {
            previewRoles.removeAll(groupId);
        }
    }

    public List<Long> getPreviewRoles(Long groupId) {
        ListMultimap<Long, Long> previewRoles = getPreviewRolesMap();

        if (previewRoles != null) {
            List<Long> groupRoles = previewRoles.get(groupId);
            if (groupRoles != null) {
                return groupRoles;
            }
        }
        return Collections.emptyList();
    }

    private ListMultimap<Long, Long> getPreviewRolesMap() {
        HttpServletRequest request = _request.get();
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        return (ListMultimap<Long, Long>) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_PREVIEW);
    }

}
