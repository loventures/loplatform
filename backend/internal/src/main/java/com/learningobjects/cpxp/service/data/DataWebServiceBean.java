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

package com.learningobjects.cpxp.service.data;

import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.item.Item;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

/**
 * The data Web service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DataWebServiceBean extends BasicServiceBean implements DataWebService {

    /** The data service. */
    @Inject
    private DataService _dataService;

    // TODO: Kill me
    public String getString(Long itemId, String dataTypeName) {

        Item item = _itemService.get(itemId);
        String dataType = dataTypeName;
        String string = DataTransfer.getStringData(item, dataType);

        return string;
    }

    public void createString(Long itemId, String dataTypeName, String data) {
        Item item = _itemService.get(itemId);
        String dataType = dataTypeName;
        _dataService.createString(item, dataType, data);
    }
}
