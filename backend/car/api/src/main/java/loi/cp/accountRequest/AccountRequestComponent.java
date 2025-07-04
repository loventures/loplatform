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

package loi.cp.accountRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.de.web.Queryable;
import loi.cp.user.UserComponent;

import javax.validation.groups.Default;
import java.util.Date;
import java.util.Map;

@Schema("accountRequest")
public interface AccountRequestComponent extends ComponentDecorator, Id {
    @JsonProperty
    @Queryable(name = "user", dataType = DataTypes.META_DATA_TYPE_ID, joinComponent = UserComponent.class)
    UserComponent getUser();

    @JsonProperty
    Map<String, Object> getAttributes();

    @JsonView(Default.class)
    @Queryable(name = "createTime", dataType = DataTypes.DATA_TYPE_CREATE_TIME)
    Date getCreateTime();

    /** Initialize this decorated account request with the specified request information. */
    void init(AccountRequestComponent init);
}
