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

package loi.cp.imports;

import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.ExactRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark service components that support creating & validating a certain type of entity pojo.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Binding(property = "value", registry = ExactRegistry.class)
public @interface ImportBinding {
    /**
     * The type which this component should be able to import
     * @return
     */
    Class<? extends ImportItem> value();

	/**
	 * The i8ln key which will be used as a label, i.e. "Terms," "Users," "Courses," etc.
	 * @return
	 */
	String label();
}
