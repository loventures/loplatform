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

package loi.cp.asset.api.converters.type;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.component.web.converter.StringConverterComponent;
import com.learningobjects.cpxp.service.item.ItemService;

import javax.inject.Inject;

@Component
public class IdStringConverter extends AbstractComponent implements StringConverterComponent<Id> {

    @Inject
    private ItemService _itemService;

    @Override
    public scala.Option<Id> apply(StringConverter.Raw<Id> input) {
        final Long longVal;
        try {
            longVal = Long.valueOf(input.value());
        } catch (NumberFormatException e) {
            return scala.Option.empty();
        }

        // can't have negative IDs, right...?
        // wrong! (ish) --hh
        if (longVal.longValue() < 0) {
            return scala.Option.empty();
        }
        else {

            return scala.Option.apply(_itemService.get(longVal));
        }
    }
}
