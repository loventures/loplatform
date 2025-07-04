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

package loi.cp.accesscode;

import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.cpxp.service.domain.DomainConstants;

import java.util.List;

@FacadeItem(DomainConstants.ITEM_TYPE_DOMAIN)
public interface RedemptionRootFacade extends Facade {
    @FacadeComponent
    @FacadeQuery(domain = true, cache = false)
    public List<RedemptionComponent> findRedemptionsByAccessCode(
      @FacadeCondition(AccessCodeConstants.DATA_TYPE_REDEMPTION_ACCESS_CODE)
      AccessCodeComponent code
    );
}
