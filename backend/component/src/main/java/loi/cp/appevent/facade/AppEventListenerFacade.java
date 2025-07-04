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

package loi.cp.appevent.facade;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeParent;
import com.learningobjects.cpxp.service.appevent.AppEventConstants;
import com.learningobjects.cpxp.service.script.ComponentFacade;

@FacadeItem(AppEventConstants.ITEM_TYPE_APP_EVENT_LISTENER)
public interface AppEventListenerFacade extends Facade {
    @FacadeParent
    public ComponentFacade getParent();

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_EVENT_ID)
    public String getEventId();
    public void setEventId(String eventId);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_LISTENER_TARGET)
    public Long getTarget();
    public void setTarget(Long target);
}
