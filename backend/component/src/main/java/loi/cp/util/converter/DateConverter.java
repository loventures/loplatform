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

package loi.cp.util.converter;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.component.web.converter.StringConverterComponent;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

@Component
public class DateConverter extends AbstractComponent implements StringConverterComponent<Date> {

    @Override
    public scala.Option<Date> apply(final StringConverter.Raw<Date> input) {

        final DateFormat dateFormat = JacksonUtils.getMapper().getSerializationConfig().getDateFormat();

        try {
            return scala.Some.apply(dateFormat.parse(input.value()));
        } catch (final ParseException pe) {
            return scala.Option.empty();
        }
    }
}
