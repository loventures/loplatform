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

package loi.cp.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.web.Queryable;
import loi.cp.context.ContextComponent;
import loi.cp.right.RightDTO;
import loi.cp.role.RoleComponent;

import javax.validation.groups.Default;
import java.util.Date;
import java.util.List;

/* Technically this is a context... but the context stuff is for group data types.
 * A decorator for now so I don't have to fight generic domain. */
@Schema("domain")
public interface DomainComponent extends ContextComponent {
    @JsonView(Default.class)
    @Queryable(dataType = DomainConstants.DATA_TYPE_DOMAIN_ID)
    String getDomainId();

    @JsonView(Default.class)
    @Queryable(dataType = DataTypes.DATA_TYPE_NAME)
    String getName();

    @JsonView(Default.class)
    @Queryable(dataType = DomainConstants.DATA_TYPE_DOMAIN_SHORT_NAME)
    String getShortName();

    /**
     * This is get-only roles, which is safe to expose to anyone,
     * vs supported roles which is admin only. But how to distinguish
     * from the *real* supported roles. Maybe rights is a protected
     * embed...
     */
    @RequestMapping(path = "supportedRoles", method = Method.GET)
    List<RoleComponent> getSupportedRoles();

    @RequestMapping(path = "knownRoles", method = Method.GET)
    ApiQueryResults<RoleComponent> getKnownRoles(ApiQuery query);

    @RequestMapping(path = "rights", method = Method.GET)
    List<RightDTO> getRights();

    @RequestMapping(path = "time", method = Method.GET)
    @Secured(allowAnonymous = true)
    TimeInfo getTime();

    class TimeInfo {
        private final String timeZone;
        private final Date currentTime;

        @JsonCreator
        public TimeInfo(@JsonProperty("timeZone") final String timeZone,
                        @JsonProperty("currentTime") final Date currentTime) {
            this.timeZone = timeZone;
            this.currentTime = currentTime;
        }

        public String getTimeZone() {
            return timeZone;
        }

        public Date getCurrentTime() {
            return currentTime;
        }
    }
}

