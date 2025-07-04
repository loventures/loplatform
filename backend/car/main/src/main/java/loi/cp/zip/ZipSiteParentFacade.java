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

package loi.cp.zip;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.folder.FolderConstants;
import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.site.SiteConstants;

import java.util.List;

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
public interface ZipSiteParentFacade extends Facade {
    @FacadeChild(value = SiteConstants.ITEM_TYPE_SITE, orderType = DataTypes.DATA_TYPE_URL, direction = Direction.ASC)
    List<ZipSiteFacade> getSites();
    QueryBuilder querySites();
    ZipSiteFacade getSite(Long id);
    ZipSiteFacade getSiteByPath(@FacadeCondition(DataTypes.DATA_TYPE_URL) String path);
    ZipSiteFacade addSite();
    void removeSite(Long id);
}
