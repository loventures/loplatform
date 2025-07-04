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

package com.learningobjects.cpxp.service.site;

import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.portal.NameFacade;
import com.learningobjects.cpxp.service.script.ComponentFacade;

@FacadeItem("*")
public interface SiteFacade extends ComponentFacade {
    @FacadeParent
    public NameFacade getParent();

    @FacadeData(DataTypes.DATA_TYPE_JSON)
    public <T> T getJson(Class<T> clas);
    public void setJson(Object json);
}
