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

package loi.cp.analytics;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * formerly known as AnalyticsConfig
 */
public interface AnalyticsConstants {

    /**
     * Actions
     */
    enum EventActionType {
        // admin CRUD (e.g. user/course creation)
        CREATE("create"),
        UPDATE("update"),
        PUT("put"), // create or update
        DELETE("delete"),

        // sessions
        START("start"),
        END("end"),

        // navigation/front-end events
        ENTER("enter"),

        // discussion boards
        POST("post"),

        // assignments/grading
        SUBMIT("submit"),
        AWARD("award"), // for grades. 2017-02-22 will probably change...
        NAVIGATE("navigate"),
        SAVE("save"),
        VIEW("view"),

        // mastery
        MASTER("master"),
        GENERATE("generate"),

        BECAME_IDLE("became_idle"),
        BECAME_ACTIVE("became_active"),
        SNAPSHOT("snapshot"),
        TIME_SPENT("time_spent"),

        LAUNCHED("launched")
        ;

        private String actionType;

        EventActionType(String actionType) {
            this.actionType = actionType;
        }

        @JsonValue
        public String toString() {
            return this.actionType;
        }
    }

    enum AssignmentType {
        ASSESSMENT("assessment"),
        SUBMISSION("submission"),
        OTHER("other") // placeholder, unused as of 2/22/2017
        ;

        private String assignmentType;
        AssignmentType(String assignmentType) {
            this.assignmentType = assignmentType;
        }

        @JsonValue
        public String toString() {
            return this.assignmentType;
        }
    }

    enum SessionAuthenticationMethod {
        DIRECT("direct"),
        LTI("lti"),
        LDAP("ldap");

        private String authMethod;

        SessionAuthenticationMethod(String authMethod) {
            this.authMethod = authMethod;
        }

        @JsonValue
        public String toString() {
            return this.authMethod;
        }
    }

    String SESSION_KEY = "sessionId";
}
