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

package com.learningobjects.cpxp.component.eval;

import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.service.item.Item;

@Evaluates(Instance.class)
public class InstanceEvaluator extends AbstractEvaluator {
    @Override
    protected Object getValue(ComponentInstance instance, Object object) {
        Long id = (instance == null) ? null : instance.getId();
        String type = (instance == null) ? null : instance.getItemType();

        // Optimization + Test Compat: If we already have a handle on the component's backing item, we can skip
        // a few lookups by passing the item directly into ComponentSupport.  This is particularly useful in
        // test-cases where there is not a full component environment.
        IdType item = instance.getItem();
        if (item instanceof Item && _itemClass.equals(ItemClass.ComponentType)) {
            return ComponentSupport.get(item, (Class<? extends ComponentInterface>) _subtype);
        }

        return super.getItem(id, type);
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
