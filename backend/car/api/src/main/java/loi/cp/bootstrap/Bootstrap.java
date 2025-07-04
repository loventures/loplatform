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

package loi.cp.bootstrap;

import com.learningobjects.cpxp.component.function.Function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as a domain bootstrap phase.
 *
 * The annotated method shall take either one parameter of
 * an object type that is deserializable by Jackson, or
 * two parameters:
 * - a {@code Long}, representing an item ID,
 *   or a facade/component backed by an item,
 * - an object type deserializable by Jackson.
 *
 * During bootstrapping of a domain, if the "bootstrap" object
 * contains a phase with the name specified in this annotation,
 * then this method will be invoked with the deserialized object(s)
 * in the phase's config:
 *
 * <pre>
 *     "bootstrap": [
 *          ...,
 *          {
 *              "phase": "{phase_name}",
 *              "config": { ... }
 *          }
 *      ]
 * </pre>
 *
 * If the phase is used in another phase's "setup" list, and the
 * annotated method takes two parameters, then the first argument
 * will be passed as the return value of the containing phase:
 *
 * <pre>
 *     "bootstrap": [
 *          ...,
 *          {
 *              "phase": "{outer_phase}",
 *              "config": { ... },
 *              "setup": [
 *                  {
 *                      "phase": "{inner_phase}",
 *                      ...
 *                  }
 *              ]
 *          }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
@Function(global = "value")
public @interface Bootstrap {
    /** The name of the bootstrap phase this method defines */
    String value();
}
