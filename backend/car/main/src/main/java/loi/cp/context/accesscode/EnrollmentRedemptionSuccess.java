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

package loi.cp.context.accesscode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import loi.cp.accesscode.AccessCodeComponent;
import loi.cp.accesscode.RedemptionSuccess;

public class EnrollmentRedemptionSuccess extends RedemptionSuccess {
    private final Long course;
    private final Long role;

    @JsonCreator
    public EnrollmentRedemptionSuccess(@JsonProperty("schema") String schema,
                                       @JsonProperty("status") String status,
                                       @JsonProperty("course") Long course,
                                       @JsonProperty("role") Long role) {
        super(schema, status);
        this.course = course;
        this.role = role;
    }

    public EnrollmentRedemptionSuccess(AccessCodeComponent accessCode, Long course, Long role) {
        super(accessCode);
        this.course = course;
        this.role = role;
    }

    public Long getCourse() {
        return course;
    }

    public Long getRole() {
        return role;
    }
}
