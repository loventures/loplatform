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

package com.learningobjects.cpxp.service.session;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;

/**
 * A facade for sessions statistics.
 */
@FacadeItem(SessionConstants.ITEM_TYPE_SESSION_STATISTICS)
public interface SessionStatisticsFacade extends Facade {
    @FacadeData(SessionConstants.DATA_TYPE_SESSION_STATISTICS_DATE)
    String getDate();
    void setDate(String date);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_STATISTICS_COUNT)
    Long getCount();
    void setCount(Long count);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_STATISTICS_DURATION)
    Long getDuration();
    void setDuration(Long duration);
}
