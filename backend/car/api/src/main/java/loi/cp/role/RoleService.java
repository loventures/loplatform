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

package loi.cp.role;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.relationship.RelationshipWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.util.Ids;
import loi.cp.bootstrap.Bootstrap;
import loi.cp.right.Right;
import loi.cp.role.impl.SupportedRoleParentFacade;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.*;

@Service
public class RoleService {
    private static final Logger logger = Logger.getLogger(RoleService.class.getName());

    private final FacadeService _facadeService;

    private final EnrollmentWebService _enrollmentWebService;

    private final RelationshipWebService _relationshipWebService;

    public RoleService(final FacadeService facadeService, final EnrollmentWebService enrollmentWebService, final RelationshipWebService relationshipWebService) {
        _facadeService = facadeService;
        _enrollmentWebService = enrollmentWebService;
        _relationshipWebService = relationshipWebService;
    }

    /**
     * Get the roles of the given user in the given context. If the given context is a group and has a logical parent
     * id, the roles of the given user in the context's parent (and so on) are also returned.
     */
    public Collection<RoleFacade> getRolesForUser(Id context, Id user) {
        return _enrollmentWebService.getActiveUserRoles(user.getId(), context.getId());
    }

    /**
     * Returns the rights infos for the given role in the given context. The returned set is mutable and modifications to it do not affect
     * the role's persisted rights.
     */
    public Set<String> getRightInfoForRole(Id context, @Nullable Id role) {
        Multimap<Long, String> rightsByRole = getRightInfoByRole(context);
        Set<String> rightNames = new HashSet<>(rightsByRole.get(Ids.get(role))); // defensive copy
        return rightNames;
    }

    /**
     * Creates a map of the supported roles to their rights for the given context
     */
    private Multimap<Long, String> getRightInfoByRole(Id context) { // cacheable
        Multimap<Long, String> rightsByRole = HashMultimap.create();
        for (SupportedRoleFacade supported : getSupportedRoles(context)) {
            SupportedRoleFacade.RightsList rights = supported.getRights();
            rightsByRole.putAll(supported.getRoleId(), rights);
        }
        return rightsByRole;
    }

    private SupportedRoleParentFacade getSupportedRoleParent(Id id) {
        return _facadeService.getFacade(id.getId(), SupportedRoleParentFacade.class);
    }

    /**
     * @return the supported roles of the domain
     */
    public List<SupportedRoleFacade> getSupportedRoles() {
        return getSupportedRoles(Current.getDomainDTO());
    }

    /**
     * The designated getSupportedRoles method.
     *
     * @return the supported roles of the given context
     */
    public List<SupportedRoleFacade> getSupportedRoles(Id context) {
        return getSupportedRoleParent(context).getSupportedRoles();
    }

    /**
     * @return the role types of the domain
     */
    public List<RoleFacade> getDomainRoles() {
        return getRoles(Current.getDomainDTO());
    }

    /**
     * The designated getRoles method.
     *
     * @return the role types of the given context
     */
    public List<RoleFacade> getRoles(Id context) {
        return getSupportedRoleParent(context).findRoles();
    }

    /**
     * Find a supported role by role id.
     *
     * @return the role, if found
     */
    public Optional<RoleFacade> findRoleByRoleId(Id context, String roleId) {
        return getSupportedRoleParent(context).findRoles().stream().filter(r -> roleId.equalsIgnoreCase(r.getRoleId())).findFirst();
    }

    /**
     * Add an empty supported role to the domain.
     *
     * @return the creatd supported role
     */
    public SupportedRoleFacade addSupportedRole() {
        return addSupportedRole(Current.getDomainDTO());
    }

    /**
     * Add an empty supported role to the given context.
     *
     * @return the created supported role
     */
    public SupportedRoleFacade addSupportedRole(Id context) {
        return getSupportedRoleParent(context).addSupportedRole();
    }

    /**
     * Add a supported role to the given context
     *
     * @param context the context to configure
     * @param idStr   the id of the supported role's role type. Should be like {@link EnrollmentWebService#ROLE_STUDENT_NAME}
     * @param rights  the rights of the supported role.
     */
    @SafeVarargs // we just iterate over it, tis safe
    public final SupportedRoleFacade addSupportedRole(Id context, String idStr, Class<? extends Right>... rights) {
        RoleFacade role = _facadeService.getFacade(idStr, RoleFacade.class);
        if (role == null) {
            throw new IllegalArgumentException("Unknown role: " + idStr + " in domain " + Current.getDomain());
        }
        return addSupportedRole(context, role, rights);
    }

    /**
     * Add a supported role to the given context
     *
     * @param context the context to configure
     * @param role    the role
     * @param rights  the rights of the supported role.
     */
    @SafeVarargs // we just iterate over it, tis safe
    public final SupportedRoleFacade addSupportedRole(Id context, Id role, Class<? extends Right>... rights) {
        SupportedRoleFacade supported = addSupportedRole(context);
        supported.setRole(role);
        SupportedRoleFacade.RightsList list = new SupportedRoleFacade.RightsList();
        for (Class<? extends Right> right : rights) {
            list.add(right.getName());
        }
        supported.setRights(list);
        return supported;
    }

    /**
     * Add rights to an existing supported role in a given context
     *
     * @param context the context to configure
     * @param roleId  the roleId of the supported role's role type. Should be like {@link EnrollmentWebService#ROLE_STUDENT_NAME}
     * @param rights  the rights to add to the supported role.
     */
    @SafeVarargs // we just iterate over it, tis safe
    public final void addRightsToRole(final Id context, final String roleId, final Class<? extends Right>... rights) {
        final SupportedRoleFacade supported = getSupportedRoles(context).stream()
          .filter(sr -> roleId.equals(sr.getRole().getRoleId()))
          .findFirst().orElse(null);
        if (supported == null) {
            throw new IllegalArgumentException("Unsupported role: " + roleId);
        }
        final SupportedRoleFacade.RightsList list = supported.getRights();
        for (Class<? extends Right> right : rights) {
            if (!list.contains(right.getName())) {
                list.add(right.getName());
            }
        }
        supported.setRights(list);
    }

    /**
     * Add a supported role to the given context.
     * Child rights are auto-selected when parent-rights are selected.
     * In order a deselect a child right it must be stored in the database as a negative right.
     * IE "-loi.cp.course.right.EditGradebookRight"
     * <p/>
     * NOTE: Negative != Remove. Negative rights are additional entires in the database.
     *
     * @param roleId         the roleId of the supported role's role type. Should be like {@link EnrollmentWebService#ROLE_STUDENT_NAME}
     * @param rights         the rights of the supported role that should be removed.
     * @param negativeRights children rights that should be deslected
     */
    public final void addSupportedRoleWithNegativeRights(Id context, String roleId, Class[] rights, Class[] negativeRights) {
        SupportedRoleFacade supported = addSupportedRole(context, roleId, rights);
        SupportedRoleFacade.RightsList list = supported.getRights();

        for (Class<? extends Right> negativeRight : negativeRights) {
            list.add("-" + negativeRight.getName());
        }
        supported.setRights(list);
    }

    public List<RoleFacade> getKnownRoles() {
        return new ArrayList<>(_relationshipWebService.getSystemRoles());
    }

    public RoleComponent getRole(Long id) {
        return ComponentSupport.get(_facadeService.getFacade(id, RoleFacade.class), RoleComponent.class);
    }

    public RoleComponent getRoleByItemId(String id) {
        return ComponentSupport.get(_facadeService.getFacade(id, RoleFacade.class), RoleComponent.class);
    }

    public RoleComponent getRoleByRoleId(String roleId) {
        return ComponentSupport.get(getRoleFacadeByRoleId(roleId), RoleComponent.class);
    }

    public RoleFacade getRoleFacadeByRoleId(String roleId) {
        return _relationshipWebService.getRoleByRoleId(_relationshipWebService.getRoleFolder(), roleId);
    }

    public boolean isHostingRole(Id role) {
        String idStr = (role == null)
          ? null
          : _facadeService.getFacade(role.getId(), RoleFacade.class).getIdStr();
        return ROLE_HOSTING_ADMIN_NAME.equals(idStr) ||
          ROLE_HOSTING_STAFF_NAME.equals(idStr) ||
          ROLE_HOSTING_SUPPORT_NAME.equals(idStr);
    }

    // TODO: The below should not be in car/api..
    /* ... but neither should this service! */

    @Bootstrap("core.role.create")
    public void createRole(JsonRole json) {
        logger.log(Level.INFO, "Create role, {0}", json.roleId);
        Long folder = _relationshipWebService.getRoleFolder();

        RoleFacade role = _relationshipWebService.getRoleByRoleId(folder, json.roleId);

        if (role == null) {
            role = _relationshipWebService.addRole(folder);
            role.setRoleId(json.roleId);
        }

        if (json.name != null) {
            role.setName(json.name);
        } else if (role.getName() == null) {
            role.setName(json.roleId); // meh
        }

        if (json.idStr != null) {
            role.setIdStr(json.idStr);
        }

        SupportedRoleFacade supported =
          getSupportedRoleParent(Current.getDomainDTO())
            .getSupportedRoles()
            .stream()
            .filter(sr -> json.roleId.equals(sr.getRole().getRoleId()))
            .findAny()
            .orElse(null);


        if (supported == null) {
            supported = addSupportedRole();
            supported.setRole(role);
        }

        supported.setRights(new SupportedRoleFacade.RightsList((json.rights == null) ? Collections.<String>emptyList() : json.rights));
    }

    public HashSet<String> getHostingAdminRoleNames() {
        return new HashSet<>(Arrays.asList(ROLE_HOSTING_ADMIN_NAME, ROLE_HOSTING_STAFF_NAME, ROLE_HOSTING_SUPPORT_NAME));
    }

    public HashSet<String> getHostingAdminRoleIds() {
        return new HashSet<>(Arrays.asList(HOSTING_ADMIN_ROLE_ID, HOSTING_STAFF_ROLE_ID, HOSTING_SUPPORT_ROLE_ID));
    }

    public static class JsonRole {
        public String idStr;
        public String roleId;
        public String name;
        public List<String> rights;
    }
}
