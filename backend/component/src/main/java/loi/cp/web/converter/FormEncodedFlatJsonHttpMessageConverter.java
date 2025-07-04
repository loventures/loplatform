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

package loi.cp.web.converter;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.eval.AllEvaluator;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.converter.ConvertOptions;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverter;
import com.learningobjects.cpxp.component.web.exception.HttpMessageNotReadableException;
import com.learningobjects.de.web.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Converts form-encoded flattened json HTTP entities to and from our own types using
 * Jackson annotations.
 * <pre>
function flatten_json(data) {
    return function flatten(value, path, map) {
        if (Object(value) !== value) {
            map[path] = value;
        } else {
            $.each(value, function(k, v) {
                flatten(v, path ? path + "." + k : "" + k, map);
            });
        }
        return map;
    } (data, "", {});
}
flatten_json({ order: [ { property: 'foo', direction: 'ASC' } ], columns: [ 'userName' ] })
Object {order.0.property: "foo", order.0.direction: "ASC", columns.0: "userName"}
$.post('/api/v2/enrollments/self/10129200/context/gradebook/export',
       flatten_json({ order: [ { property: 'foo', direction: 'ASC' } ], columns: [ 'userName' ] }),
       function(r) { console.log(r); });
 * </pre>
 */
@Component
public class FormEncodedFlatJsonHttpMessageConverter extends AbstractComponent implements HttpMessageConverter<Object> {

    private static final List<MediaType> SUPPORTED_MEDIA_TYPES = List.of(MediaType.APPLICATION_FORM_URLENCODED);

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public boolean canRead(final Type type, final MediaType mediaType) {
        return SUPPORTED_MEDIA_TYPES.stream().anyMatch(MediaType.isCompatibleWithPredicate(mediaType));
    }

    @Override
    public boolean canWrite(final Object value, final MediaType mediaType) {
        return false;
    }

    @Override
    public Object read(final RequestBody requestBody, final WebRequest request, final Type targetType) {
        try {
            AllEvaluator evaluator = new AllEvaluator();
            evaluator.init(null, null, targetType, new Annotation[0]);
            return evaluator.decodeValue(null, null, request.getRawRequest());
        } catch (Exception ex) {
            throw new HttpMessageNotReadableException("Could not read flat JSON: " + ex.getMessage(),ex);
        }
    }

    @Override
    public void write(final Object source, final ConvertOptions options, HttpServletRequest request, final HttpServletResponse response) {
        throw new UnsupportedOperationException();
    }
}
