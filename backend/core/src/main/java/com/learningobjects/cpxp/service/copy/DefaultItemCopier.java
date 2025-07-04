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

package com.learningobjects.cpxp.service.copy;

import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.cpxp.service.item.Item;

/**
 * Optionally perform filtering of internal data.
 */
public class DefaultItemCopier extends AbstractItemCopier {

    // this could be done via a separate ItemCopier
    private boolean _copyInternalData;

    public DefaultItemCopier(Item source, ServiceContext serviceContext, boolean copyInternalData, boolean skipLeafDescendants) {
        super(source, serviceContext, skipLeafDescendants);
        _copyInternalData = copyInternalData;
    }

    public Object filter(Item item) {
        String type = item.getType();
        if (!_copyInternalData) {
            if (IntegrationConstants.ITEM_TYPE_INTEGRATION.equals(type)) {
                return null;
            }
        }
        return item;
    }

    public Data filter(Item owner, Data data) {
        String type = data.getType();
        if (!_copyInternalData) {
            if (DataTypes.DATA_TYPE_ID.equals(type)
                        || DataTypes.DATA_TYPE_CRITICAL.equals(type)
                        || IntegrationConstants.DATA_TYPE_LEGACY_ID.equals(type)) {
                return null;
            }
        }
        return data;
    }

}
