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

package loi.cp.admin;

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractHtmlServlet;
import com.learningobjects.cpxp.component.web.ErrorResponse;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.right.Right;
import loi.cp.right.RightService;

import javax.inject.Inject;

@Component(
    name = "$$name=Admin Site Servlet",
    description = "$$description=This servlet renders the LTI admin page",
    version = "0.7"
)
@ServletBinding(
    path = "/sys/admin"
)
public class AdminPageServlet extends AbstractHtmlServlet {
    @Inject
    private RightService _rightService;

    @Override
    public WebResponse render(String path) {
        String name = StringUtils.substringBefore(path.substring(1), "!");
        final ComponentInstance component = ComponentSupport.getComponent(name, null);
        if (component == null) {
            return ErrorResponse.notFound();
        }
        component.getComponent().getDelegate().checkAccess(component);
        AdminPageComponent site = component.getInstance(AdminPageComponent.class);
        AdminPageBinding binding = ComponentSupport.getBinding(site, AdminPageComponent.class);
        boolean hasRight = binding.secured().value().length == 0;
        for (Class<? extends Right> right : binding.secured().value()) {
            hasRight |= _rightService.getUserHasRight(right, binding.secured().match());
        }
        if (!hasRight) {
            return ErrorResponse.forbidden();
        }
        return site.renderAdminPage();
    }
}
