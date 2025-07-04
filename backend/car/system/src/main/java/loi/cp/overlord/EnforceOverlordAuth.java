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

package loi.cp.overlord;

import com.learningobjects.cpxp.component.acl.AccessControl;
import com.learningobjects.cpxp.component.annotation.EnforceOverlord;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires that the user making the RPC has authenticated as an Overlord.
 *
 * This will prompt the user for an Overlord username/password over
 * basic HTTP auth, and if successful will add the Overlord authentication
 * to their session. This does *not* change the user nor the domain.
 *
 * @see AccessControl
 * @see OverlordAuthEnforcer
 * @see EnforceOverlord
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
@AccessControl(OverlordAuthEnforcer.class)
public @interface EnforceOverlordAuth {
}
