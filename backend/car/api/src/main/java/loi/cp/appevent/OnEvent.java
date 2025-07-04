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

package loi.cp.appevent;

import com.learningobjects.cpxp.component.function.Function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a component implementation method can accept app events
 * of a given type (the type of the first parameter). If the component is the recipient
 * of an app event of this type, then the annotated method will be invoked. The event
 * itself will be supplied as the first parameter. Other parameters will be injected
 * according to their {@link EventRel} and {@link EventSrc} annotations.
 *
 * A component will receive an app event of a given type if it is either the explicit
 * target of the event (e.g. a scheduled event), if it has explicitly registered in
 * the database to receive those events ({@link AppEventService#registerListener}), or
 * if it has registered as a global stateless recipient of the event type
 * ({@link OnEventBinding}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
@Function
public @interface OnEvent {
}
