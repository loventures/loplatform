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

package loi.cp.datatype;

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.registry.SchemaRegistry;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.validation.DefaultHttpRequestGroupAssignmentStrategy;
import com.learningobjects.de.web.util.ModelUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;

@Service
public class SchemaService {

    public SchemaRegistry.Registration findSchema(final Method httpMethod, final Type targetType) {

        final String schemaName = ModelUtils.getSchemaName(targetType, Collections.<TypeVariable<?>, Type>emptyMap());

        return findSchema(httpMethod, schemaName);
    }

    public SchemaRegistry.Registration findSchema(final Method httpMethod, final String schemaName) {

        final SchemaRegistry.Registration registration;
        if (StringUtils.isNotEmpty(schemaName)) {
            final Class<?> schemaGroup = DefaultHttpRequestGroupAssignmentStrategy.getGroup(httpMethod.name());
            registration = ComponentSupport.lookupResource(Schema.class, SchemaRegistry.Registration.class,
                    schemaName, schemaGroup);
        } else {
            registration = null;
        }

        return registration;

    }

}
