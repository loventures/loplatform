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

package loi.cp.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.web.Method;
import loi.cp.user.UserComponent;

import java.util.Optional;

@Schema("session")
public interface SessionComponent extends ComponentInterface {
    @JsonProperty("user_id")
    public Long getUserId();

    @RequestMapping(path = "user", method = Method.GET)
    public UserComponent getUser();

    @JsonProperty
    public Boolean isSudoed();

    // LMS integration

    @JsonProperty
    public Long getSessionPk();

    @JsonProperty
    public boolean isIntegrated();

    /**
     * A redirect URL invoked at the end of the session. Can be triggered by logout, or other events like completing an activity.
     */
    @JsonProperty
    public Optional<String> getReturnUrl();

    /**
     * A redirect URL invoked specifically by logout. More specific than {@link #getReturnUrl()}, so takes higher precedence when specified.
     */
    @JsonProperty
    public Optional<String> getLogoutReturnUrl();

    @JsonProperty
    public Optional<DocumentTarget> getDocumentTarget();

    @JsonProperty
    public Optional<String> getCustomTitle();

    public static enum DocumentTarget {
        frame, iframe, window;
    }
}
