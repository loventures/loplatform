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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.component.web.converter.StringConverterComponent;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.util.NumberUtils;

import javax.inject.Inject;

@Component
public class ComponentInterfaceStringConverter extends AbstractComponent implements StringConverterComponent<ComponentInterface> {
    @Inject
    FacadeService _facadeService;

    @Override
    public scala.Option<ComponentInterface> apply(StringConverter.Raw<ComponentInterface> input) {

        // should be assured type is sub of ComponentInterface
        final Class<? extends ComponentInterface> type = input.tpe().getRawType().asSubclass(ComponentInterface.class);

        ComponentInterface component = null;

        try {
            if (NumberUtils.isNumber(input.value()))  {
                component = _facadeService.getComponent(Long.valueOf(input.value()), type);
            }
        }
        catch(Exception e) {
            return scala.Option.empty();
        }

        if (component != null) {
            return scala.Some.apply(component);
        } else {
            throw new ResourceNotFoundException("component " + input.value() + " not found");
        }
    }
}
