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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;

@FacadeItem(AccessCodeConstants.ITEM_TYPE_ACCESS_CODE)
public interface AccessCodeFacade extends Facade {
    @FacadeData
    public String getAccessCode();
    public void setAccessCode(String accessCode);

    @FacadeData
    public Long getRedemptionCount();
    public void setRedemptionCount(Long redemptionCount);

    @FacadeData
    public AccessCodeBatchComponent getBatch();
    public void setBatch(Id batch);

    @FacadeData
    public Long getBatchId();

    public void refresh(boolean lock);
}
