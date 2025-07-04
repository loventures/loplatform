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

package loi.cp.admin;

import com.learningobjects.cpxp.component.acl.AccessControl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces that the annotated RPC class or method requires
 * the current user to have admin privileges on the current domain.
 *
 * This used to use ACLs, but those have been non-existent since
 * early 2017, and this annotation duly moved to using rights. As
 * such, it's now equivalent to
 *
 * <code>
 *     &amp;EnforceAdminRight(
 *       value = AdminRight.class,
 *       match = RightMatch.ANY
 *     )
 * </code>
 *
 * but significantly terser.
 *
 * @see AccessControl
 * @see AdminEnforcer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
@AccessControl(AdminEnforcer.class)
public @interface EnforceAdmin {
}
