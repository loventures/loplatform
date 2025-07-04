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

package loi.cp.user;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.site.ItemSiteBinding;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.web.HtmlResponse;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.service.user.UserConstants;

@Component
@ItemSiteBinding(type = UserConstants.ITEM_TYPE_USER, action = "view")
public class UserProfile extends AbstractComponent implements ItemSiteComponent {
    @Instance
    private UserComponent _self;

    @Override
    public WebResponse renderSite(String view) {
        return HtmlResponse.apply(this, "userProfile.html");
    }
}
