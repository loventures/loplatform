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

package loi.cp.admin.right;

import com.learningobjects.cpxp.component.acl.AccessControl;
import loi.cp.admin.right.impl.AdminRightEnforcer;
import loi.cp.right.RightMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces that the annotated RPC class or method requires
 * the current user to have the given domain right.
 *
 * @see AccessControl
 * @see AdminRightEnforcer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.METHOD })
@AccessControl(AdminRightEnforcer.class)
public @interface EnforceAdminRight {
    /** The right that the user must have on the domain */
    Class<? extends HostingAdminRight> value();

    /**
     * Whether to match only the specified right
     * or any/all of its descendant rights, as well.
     *
     *  @see RightMatch
     */
    RightMatch match() default RightMatch.EXACT;
}
