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

import java.util.Date;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.appevent.AppEventConstants;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import loi.cp.appevent.AppEvent;
import loi.cp.appevent.impl.AppEventState;

@FacadeItem(AppEventConstants.ITEM_TYPE_APP_EVENT)
public interface AppEventFacade extends Facade {
    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_EVENT_ID)
    public String getEventId();
    public void setEventId(String eventId);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_CREATED)
    public Date getCreated();
    public void setCreated(Date created);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_TARGET)
    public ComponentFacade getTarget();
    public void setTarget(Id target);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_DEADLINE)
    public Date getDeadline();
    public void setDeadline(Date deadline);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_FIRED)
    public Date getFired();
    public void setFired(Date fired);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_STARTED)
    public Date getProcessingStart();
    public void setProcessingStart(Date startProcessing);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_FINISHED)
    public Date getProcessingEnd();
    public void setProcessingEnd(Date endProcessing);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_HOST)
    public String getHost();
    public void setHost(String host);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_STATE)
    public AppEventState getState();
    public void setState(AppEventState state);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_PAYLOAD)
    public String getPayload(); // I have to retrieve it as a string because the class is determined at runtime
    public void setPayload(AppEvent payload);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_REL_0)
    public Long getRel0();
    public void setRel0(Id rel0);

    @FacadeData(AppEventConstants.DATA_TYPE_APP_EVENT_REL_1)
    public Long getRel1();
    public void setRel1(Id rel1);

    public void refresh();
}
