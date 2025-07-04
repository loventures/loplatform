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

package loi.cp.user;

import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Condition;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.user.*;
import com.learningobjects.cpxp.util.GuidUtil;
import scaloi.GetOrCreate;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * TODO: KILLME: This is a junk service to create users until we have a
 * workable base user component.
 */
@Service
public class OldUserService {

    @Inject
    private FacadeService _facadeService;

    public UserFacade createUser(String username) { // returns null on duplicate
        UserFolderFacade users = _facadeService.getFacade(UserConstants.ID_FOLDER_USERS, UserFolderFacade.class);
        GetOrCreate<UserFacade> goc = users.getOrCreateUserByUsername(username);
        if (goc.isGotten()) {
            return null;
        }

        UserFacade user = goc.result();
        initUser(user);
        user.bindUrl(GuidUtil.guid());

        return user;
    }

    public Optional<UserComponent> findUserById(long id) {
        return findFirstIn(
          BaseCondition.getInstance(DataTypes.META_DATA_TYPE_ID, Comparison.eq, id)
        );
    }

    public Optional<UserComponent> findUserByExternalId(String externalId) {
        return findFirstIn(
          BaseCondition.getInstance(UserConstants.DATA_TYPE_EXTERNAL_ID, Comparison.eq, externalId)
        );
    }

    public Optional<UserComponent> findUserByUsername(String username) {
        return findFirstIn(
          BaseCondition.getInstance(UserConstants.DATA_TYPE_USER_NAME, Comparison.eq, username)
        );
    }

    private Optional<UserComponent> findFirstIn(Condition... c) {
        QueryBuilder qb = _facadeService
          .getFacade(UserConstants.ID_FOLDER_USERS, UserFolderFacade.class)
          .queryUsers();

        Arrays.stream(c).forEach(qb::addCondition);

        List<UserComponent> users = qb.getComponentList(UserComponent.class);

        return users.stream().findFirst();
    }

    public void initUser(UserFacade user) {
        user.setUserType(UserType.Standard);
        user.setUserState(UserState.Active);
        user.setDisabled(false);
        user.setInDirectory(false);
        user.setCreateTime(Current.getTime());
    }

}
