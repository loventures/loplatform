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

package loi.cp.siteconfiguration;

import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;
import loi.cp.admin.right.ExternalLinkAdminRight;

/**
 * Endpoint to track domain-specific configuration settings.
 */
@Service
@Controller(value = "siteconfiguration", root = true)
public interface SiteConfigurationApiComponent extends ApiRootComponent {

    /**
     * Create a list of footer links to be displayed on each page.
     *
     * @param configuration
     * @return
     */
    @RequestMapping(path = "siteconfiguration/footer", method = Method.POST)
    @Secured(ExternalLinkAdminRight.class)
    public DomainLinkConfiguration createFooter(@RequestBody DomainLinkConfiguration configuration);

    /**
     * Get the list of links that have been configured for the domain's footer.
     *
     * NOTE: This needs to be unsecured in order for any person to be able to see the footer contents.
     * @return All configured footer links.
     */
    @RequestMapping(path = "siteconfiguration/footer", method = Method.GET)
    @Secured(allowAnonymous = true)
    public DomainLinkConfiguration getFooter();

    @RequestMapping(path = "siteconfiguration/header", method = Method.POST)
    @Secured(ExternalLinkAdminRight.class)
    DomainLinkConfiguration createHeader(@RequestBody DomainLinkConfiguration configuration);

    @RequestMapping(path = "siteconfiguration/header", method = Method.GET)
    @Secured(allowAnonymous = true)
    DomainLinkConfiguration getHeader();
}
