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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import loi.cp.user.UserComponent;

import java.util.Optional;

@Component
public class Session extends AbstractComponent implements SessionComponent {
    @Infer
    private HttpServletRequest _request;

    @Override
    public Long getUserId() {
        return Current.getUser();
    }

    @Override
    public UserComponent getUser() {
        return ComponentSupport.get(getUserId(), UserComponent.class);
    }

    @Override
    public Boolean isSudoed() {
        return _request.getSession().getAttribute(SessionUtils.SESSION_ATTRIBUTE_SUDOER) != null;
    }

    @Override
    public Long getSessionPk() {
        return Current.getSessionPk();
    }

    @Override
    public boolean isIntegrated() {
        return "true".equals(_request.getSession().getAttribute("ltiLaunch"));
    }

    @Override
    public Optional<String> getReturnUrl() {
        return getStringAttribute(SessionUtils.SESSION_ATTRIBUTE_RETURN_URL);
    }

    @Override
    public Optional<String> getLogoutReturnUrl() {
        return getStringAttribute(SessionUtils.SESSION_ATTRIBUTE_LOGOUT_RETURN_URL);
    }

    private Optional<String> getStringAttribute(final String name) {
        return Optional.ofNullable((String) _request.getSession().getAttribute(name));
    }

    @Override
    public Optional<SessionComponent.DocumentTarget> getDocumentTarget() {
        String target = (String) _request.getSession().getAttribute("ltiTarget");
        try {
            return Optional.ofNullable((target == null) ? null : DocumentTarget.valueOf(target));
        } catch (Exception ex) {
            return Optional.empty(); // ignore garbage
        }
    }

    @Override
    public Optional<String> getCustomTitle() {
        return getStringAttribute(SessionUtils.SESSION_ATTRIBUTE_CUSTOM_TITLE);
    }
}
