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

package loi.cp.integration;

import com.learningobjects.cpxp.service.login.LoginComponent;

/**
 * A connector that supports some form of user login.
 */
public interface LoginSystemComponent<A extends SystemComponent<A>, B extends LoginComponent> extends SystemComponent<A> {
    /** Get the serializable representation of this login system for use by a user interface. */
    B getLoginComponent();

    /** If the implementation returns a redirect URL it is responsible for actually logging the user out of CP. */
    String logout();
}
