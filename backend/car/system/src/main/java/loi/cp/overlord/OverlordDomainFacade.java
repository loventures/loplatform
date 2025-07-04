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

package loi.cp.overlord;

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.component.misc.OverlordDomainConstants;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainFacade;

import java.util.Set;

@FacadeItem(DomainConstants.ITEM_TYPE_DOMAIN)
public interface OverlordDomainFacade extends DomainFacade {

    @FacadeData(OverlordDomainConstants.DATA_TYPE_BANNED_IPS)
    public Set<String> getBannedIps();
    public void setBannedIps(Set<String> bannedIps);

}
