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

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.util.PropertyIgnoreCaseComparator;
import loi.cp.right.RightService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

@Service
public class AdminSiteService {

    @Inject
    private RightService _rightService;

    public Map<String, Collection<ComponentInstance>> getAdminPages() {
        TreeMultimap<String, ComponentInstance> pages = TreeMultimap.create(Ordering.natural(), new PropertyIgnoreCaseComparator<>("name", true));
        for (ComponentInstance page : ComponentSupport.getComponents(AdminPageComponent.class, null)) {
            AdminPageBinding binding = page.getComponent().getDelegate().getBinding(AdminPageComponent.class);
            if (binding != null && _rightService.getUserHasRight(binding.secured().value()[0], binding.secured().match())) {
                pages.put(binding.group(), page);
            }
        }
        return pages.asMap();
    }

}
