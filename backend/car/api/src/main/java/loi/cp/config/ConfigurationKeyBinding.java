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

package loi.cp.config;

import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.ExactRegistry;
import com.learningobjects.de.authorization.Secured;
import loi.cp.admin.right.AdminRight;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Binding for configuration types.
  * Causes the configuration relationship to be stored in the component registry,
  * where it can be reflectively accessed for purposes unknown and nefarious. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Binding(component = false, registry = ExactRegistry.class, property = "value")
public @interface ConfigurationKeyBinding {
    /** Schema name for this configuration. */
    String value() default "";

    /** Security configuration to read this key. */
    Secured read() default @Secured(value = AdminRight.class, byOwner = true);

    /** Security configuration to write to this key. */
    Secured write() default @Secured(value = AdminRight.class, byOwner = true);
}
