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

import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.MultiRegistry;
import com.learningobjects.de.authorization.Secured;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link AdminPageComponent} annotation which configures where the page link is displayed on the Administration Portal
 * and who can access it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
@Binding(property = "group", registry = MultiRegistry.class)
public @interface AdminPageBinding {
    /**
     * Identifier for the set of admin pages to which this page belongs. See {@code loi.cp.admin.AdminSite} for ID to label mapping.
     * Values include: domainCustomization, integrations, domainConfig, reporting, usersGroups, domainOperations (the last being overlord only).
     */
    String group();

    /** What security applies to this page. */
    Secured secured();
}
