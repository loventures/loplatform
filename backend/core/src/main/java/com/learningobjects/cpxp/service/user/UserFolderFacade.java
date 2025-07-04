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

package com.learningobjects.cpxp.service.user;

import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.folder.FolderConstants;
import com.learningobjects.cpxp.service.folder.FolderFacade;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import scala.Option;
import scaloi.GetOrCreate;

import java.util.List;

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
public interface UserFolderFacade extends FolderFacade, ComponentFacade {
    @FacadeChild(UserConstants.ITEM_TYPE_USER)
    public List<UserFacade> getUsers();
    public UserFacade findUserByUsername(
      @FacadeCondition(value = UserConstants.DATA_TYPE_USER_NAME, function = Function.LOWER)
      String userName
    );
    public UserFacade findUserByEmailAddress(
      @FacadeCondition(value = UserConstants.DATA_TYPE_EMAIL_ADDRESS, function = Function.LOWER)
      String userName
    );
    public UserFacade findUserByExternalId(
      @FacadeCondition(value = UserConstants.DATA_TYPE_EXTERNAL_ID, comparison = Comparison.in)
      String externalId
    );
    public List<UserFacade> findUsersBySubquery(
      @FacadeCondition(value = DataTypes.META_DATA_TYPE_ITEM, comparison = Comparison.in)
      QueryBuilder subQuery
    );
    public QueryBuilder queryUsers();
    public GetOrCreate<UserFacade> getOrCreateUserByUsername(
      @FacadeCondition(value = UserConstants.DATA_TYPE_USER_NAME, function = Function.LOWER)
        String userName
    );
    public Option<UserFacade> findUserBySubquery(
      @FacadeCondition(value = DataTypes.META_DATA_TYPE_ITEM, comparison = Comparison.in)
        QueryBuilder subQuery
    );
    public GetOrCreate<UserFacade> getOrCreateUserBySubquery(
      @FacadeCondition(value = DataTypes.META_DATA_TYPE_ITEM, comparison = Comparison.in)
      QueryBuilder subQuery
    );
    public void lock(boolean pessimistic);
}
