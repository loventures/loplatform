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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.util.FormattingUtils;
import loi.cp.right.Right;
import loi.cp.right.RightService;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fronts {@link RoleService} to work with {@link SupportedRole} types.
 */
@Service
public class SupportedRoleService {

    @Inject
    private RightService rightService;

    @Inject
    private RoleService roleService;

    /**
     * Get the roles for the given context.
     *
     * @param context the source of the role configurations
     * @return the roles for the given context
     */
    public List<SupportedRole> getRoles(final Id context) {

        final List<SupportedRole> supportedRoles = new ArrayList<>();

        for (final SupportedRoleFacade supportedRole : roleService.getSupportedRoles(context)) {

            final RoleFacade role = supportedRole.getRole();
            if(role != null) {
                final RoleType roleType = new RoleType(role.getId(), FormattingUtils.contentName(role), role.getRoleId());
                final Set<Class<? extends Right>> rights = rightService.getRoleRights(context, role);

                final List<String> rightNames =
                  rights.stream().map(Class::getName).collect(Collectors.toList());

                supportedRoles.add(SupportedRole.apply(supportedRole.getId(), roleType, rightNames));
            }
        }

        return supportedRoles;
    }

    public List<SupportedRole> getAllRoles(Id context) {
        List<SupportedRole> list = new ArrayList<>();
        list.addAll(getRoles(context));
        Optional<SupportedRoleFacade> guestRoleFacade =
          roleService
            .getSupportedRoles(context)
            .stream()
            .filter(sr -> sr.getRole() == null)
            .findFirst();
        guestRoleFacade.ifPresent(supportedRoleFacade -> {
            final Set<Class<? extends Right>> rights = rightService.getRoleRights(context, null);
            final List<String> rightNames =
              rights.stream().map(Class::getName).collect(Collectors.toList());
            SupportedRole guest = SupportedRole.apply(
              supportedRoleFacade.getId(),
              new RoleType(-1L, "guest", "guest"),
              rightNames
            );
            list.add(guest);
        });
        return list;
    }

    public boolean supports(Id context, RoleComponent role) {
        /* I suppose null roles match? maybe? */
        return getRoles(context)
          .stream()
          .anyMatch(sr -> Objects.equals(sr.roleType().roleId(), role.getRoleId()));
    }

}
