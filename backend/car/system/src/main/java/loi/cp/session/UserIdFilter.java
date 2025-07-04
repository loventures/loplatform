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

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.AbstractComponentFilter;
import com.learningobjects.cpxp.component.web.FilterBinding;
import com.learningobjects.cpxp.component.web.FilterInvocation;
import com.learningobjects.cpxp.filter.CurrentFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.NumberUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Writer;

@Component
@FilterBinding(
        priority = 10,
        system = false
)
public class UserIdFilter extends AbstractComponentFilter {
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response, FilterInvocation invocation) throws Exception {
        Long userId = safeParseLong(request.getHeader(CurrentFilter.HTTP_HEADER_X_USER_ID));
        if ((userId != null) && !userId.equals(Current.getUser())
                && !(Current.isAnonymous() && request.getSession().isNew()) ) {
            String message;
            if (Current.isAnonymous()) {
                message = "Your session has expired or you logged out.";
            } else {
                message = "You logged in as someone else.";
            }
            JsonMap map = JsonMap.of("error", "session").add("message", message);
            response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            try (Writer writer = HttpUtils.getWriter(request, response)) {
                ComponentUtils.toJson(map, writer);
            }
            return false;
        }
        return true;
    }

    private Long safeParseLong(String value) {
        return "NaN".equals(value) ? null : NumberUtils.parseLong(value);
    }
}
