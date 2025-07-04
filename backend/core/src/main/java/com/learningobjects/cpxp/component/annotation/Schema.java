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

package com.learningobjects.cpxp.component.annotation;

import com.learningobjects.cpxp.component.registry.ResourceBinding;
import com.learningobjects.cpxp.component.registry.SchemaRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a JSON schema for the @JsonProperty annotated properties of this interface.
 * The concrete class implementing an @Schema-annotated interface should put it
 * at the beginning of its implements-list.
 *
 * <p>Schema is generated at build time by the schemaGen task. By default, this task
 * is not part of any build.</p>
 *
 * <p>For example, at the time of writing, component/schemaGen will
 * produce schemas in {code}%componentOutputDir/classes/schema/json{code}. The schemas
 * are hosted at api/v2/schemaNames (see SchemaController) </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ResourceBinding(registry = SchemaRegistry.class)
public @interface Schema {

    String value();
}

