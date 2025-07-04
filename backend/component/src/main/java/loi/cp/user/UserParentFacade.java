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

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeComponent;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.folder.FolderConstants;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.user.UserConstants;
import scaloi.GetOrCreate;

import java.util.Optional;

@FacadeItem("*")
public interface UserParentFacade extends Facade {
    @FacadeComponent
    public Optional<UserComponent> getUser(Long id);
    public Optional<UserComponent> findUserByUsername(
      @FacadeCondition(value = UserConstants.DATA_TYPE_USER_NAME, function = Function.LOWER)
      String userName
    );
    public Optional<UserComponent> findUserByExternalId(
      @FacadeCondition(value = UserConstants.DATA_TYPE_EXTERNAL_ID, function = Function.LOWER)
      String externalId);
    public GetOrCreate<UserComponent> getOrCreateUserByUsername(
      @FacadeCondition(value = UserConstants.DATA_TYPE_USER_NAME, function = Function.LOWER)
      String userName,
      UserComponent.Init init
    );
    public GetOrCreate<UserComponent> getOrCreateUserByExternalId(
      @FacadeCondition(value = UserConstants.DATA_TYPE_EXTERNAL_ID, function = Function.LOWER)
      String externalId,
      UserComponent.Init init
    );
    public QueryBuilder queryUsers();
    public UserComponent addUser(UserComponent.Init init);
    public boolean lock(boolean pessimistic);

    String ID_FOLDER_USERS = UserConstants.ID_FOLDER_USERS;
}
