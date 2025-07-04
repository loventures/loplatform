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

package loi.cp.bootstrap;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.login.LoginWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.bootstrap.timeoffsets.BootstrapOffset;
import loi.cp.role.RoleService;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class UserBootstrap extends AbstractComponent {
    private static final Logger logger = Logger.getLogger(UserBootstrap.class.getName());

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    @Inject
    private LoginWebService _loginWebService;

    @Inject
    private RoleService _roleService;

    @Inject
    private UserWebService _userWebService;

    @Bootstrap("core.user.create")
    public UserFacade createUser(JsonUser json) throws Exception {
        logger.log(Level.INFO, "Create user {0}", json.userName);

        return internalCreateUser(json);
    }

    @Bootstrap("core.user.getOrCreate")
    public UserFacade getOrCreateUser(JsonUser json) throws Exception {
        final UserFacade user = _userWebService.getUserByUserName(json.userName);
        if (user != null) {
            return user;
        } else {
            return internalCreateUser(json);
        }
    }

    private UserFacade internalCreateUser(JsonUser json) throws Exception {
        Optional.ofNullable(json.offset)
                .ifPresent(o -> o.asTimeManipulator().commit());
        Long folder = _userWebService.getUserFolder();

        UserFacade user = _userWebService.addUser(folder);

        user.setUserName(json.userName);
        user.setGivenName(json.givenName);
        user.setMiddleName(json.middleName);
        user.setFamilyName(json.familyName);
        user.setEmailAddress(json.emailAddress);
        user.updateFullName();

        if (StringUtils.isNotBlank(json.externalId)) {
            user.setUserExternalId(Optional.of(json.externalId.trim()));
        }

        Long userId = user.getId();

        if (StringUtils.isNotEmpty(json.password)) {
            _loginWebService.setPassword(userId, json.password);
        }

        if (json.roles != null) {
            Set<String> roles = new HashSet<>(json.roles);
            List<Long> roleIds = new ArrayList<>();
            for (RoleFacade role : _roleService.getDomainRoles()) {
                if (roles.remove(role.getRoleId())) {
                    roleIds.add(role.getId());
                }
            }
            if (!roles.isEmpty()) {
                throw new Exception("Unknown role: " + roles.iterator().next());
            }
            _enrollmentWebService.setEnrollment(Current.getDomain(), roleIds, userId);
        }

        Current.clearCache();
        EntityContext.flushClearAndCommit();

        return _userWebService.getUser(userId);
    }

    public static class JsonUser {
        public String userName;
        public String givenName;
        public String middleName;
        public String familyName;
        public String emailAddress;
        public String password;
        public String externalId;
        public List<String> roles;
        public BootstrapOffset offset;
    }
}
