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

package loi.cp.role;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.util.FormattingUtils;

@Component
public class Role extends AbstractComponent implements RoleComponent {
    @Instance
    private RoleFacade _instance;

    @Override
    public Long getId() {
        return _instance.getId();
    }

    @Override
    public String getRoleId() {
        return _instance.getRoleId();
    }

    @Override
    public String getName() {
        return FormattingUtils.roleStr(_instance);
    }
}
