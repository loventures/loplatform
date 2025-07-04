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

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeJson;

import java.util.Date;
import scala.collection.immutable.List;

// I am sadly Java because of scalac issues with constants defined in Java
// in the same source tree but used in an annotation in scala. But I
// was scala, hence some scala collection types.

@FacadeItem(UserConstants.ITEM_TYPE_USER_HISTORY)
public interface UserHistoryFacade extends Facade {
    @FacadeJson
    Date getPasswordLastChanged();
    void setPasswordLastChanged(Date date);

    @FacadeJson
    List<LoginRecord> getLogins();
    void setLogins(List<LoginRecord> logins);

    @FacadeJson
    List<String> getPasswords();
    void setPasswords(List<String> passwords);

    @FacadeData(UserConstants.DATA_TYPE_ACCESS_TIME)
    Date getAccessTime();
    void setAccessTime(Date date);

    @FacadeData(UserConstants.DATA_TYPE_LOGIN_TIME)
    Date getLoginTime();
    void setLoginTime(Date date);

    @FacadeData(UserConstants.DATA_TYPE_LOGIN_AUTH_TIME)
    Date getExternalAuthTime();
    void setExternalAuthTime(Date date);

    @FacadeData(UserConstants.DATA_TYPE_LOGIN_COUNT)
    Long getLoginCount();
    void setLoginCount(long count);

    @FacadeJson
    List<Long> getHiddenAnnouncements();
    void setHiddenAnnouncements(List<Long> ids);
}
