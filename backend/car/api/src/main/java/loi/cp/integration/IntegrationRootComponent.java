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

package loi.cp.integration;

import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.PathVariable;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.web.Deletable;
import loi.cp.admin.right.AdminRight;
import loi.cp.admin.right.IntegrationAdminRight;
import loi.cp.right.RightMatch;

import java.util.Optional;

@Controller(value = "integrations", category = Controller.Category.CONTEXTS)
@RequestMapping(path = "integrations")
@Secured(IntegrationAdminRight.class)
public interface IntegrationRootComponent extends ApiRootComponent, ComponentDecorator {
    @RequestMapping(method = Method.GET)
    @Secured(value = AdminRight.class, overrides = true, match = RightMatch.ANY)
    ApiQueryResults<IntegrationComponent> getIntegrations(ApiQuery query);

    @RequestMapping(path = "{id}", method = Method.GET)
    @Deletable
    Optional<IntegrationComponent> getIntegration(@PathVariable("id") Long id);

    Optional<IntegrationComponent> getIntegrationBySystemId(Long systemId);

    @RequestMapping(method = Method.POST)
    IntegrationComponent addIntegration(@RequestBody IntegrationComponent integration);

    IntegrationComponent addIntegration(IntegrationComponent.Init integration);
}
