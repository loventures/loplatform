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

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.component.misc.SiteConfigurationConstants;

@FacadeItem(SiteConfigurationConstants.ITEM_TYPE_SITE_CONFIGURATION)
public interface SiteConfigurationFacade extends Facade {

    @FacadeData(SiteConfigurationConstants.DATA_TYPE_FOOTER_CONFIGURATION)
    public DomainLinkConfiguration getFooterConfiguration();
    public void setFooterConfiguration(DomainLinkConfiguration configuration);

    @FacadeData(SiteConfigurationConstants.DATA_TYPE_HEADER_CONFIGURATION)
    public DomainLinkConfiguration getHeaderConfiguration();
    public void setHeaderConfiguration(DomainLinkConfiguration configuration);
}
