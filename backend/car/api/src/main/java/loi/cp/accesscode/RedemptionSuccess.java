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

package loi.cp.accesscode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.learningobjects.cpxp.component.ComponentSupport;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property = "_type")
public abstract class RedemptionSuccess {
    private AccessCodeComponent accessCode;

    private String schema;
    private String status;

    public RedemptionSuccess() {
        this("", "ok");
    }
    @JsonCreator
    public RedemptionSuccess(@JsonProperty("schema")String schema, @JsonProperty("status") String status) {
        this.schema = schema;
        this.status = status;
    }

    public RedemptionSuccess(AccessCodeComponent accessCode) {
        this(ComponentSupport.getSchemaName(accessCode.getBatch()), "ok");
        this.accessCode = accessCode;
    }

    @JsonProperty
    public String getStatus() {
        return status;
    }

    @JsonProperty
    public String getSchema() {
        return schema;
    }
}
