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

package com.learningobjects.cpxp.startup;

import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.ClassRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
@Binding(registry = ClassRegistry.class, component = false)
public @interface StartupTaskBinding {
    /**
     * A version number for this startup task. If the version number does
     * not match the run log in the database then the startup task will
     * be executed again, even if it has already been run. Increment this
     * number when an existing upgrade task needs to be re-run because
     * related code has changed; for example, if an enumeration that is
     * persisted to the database has been extended. Be aware of the
     * consequences to total order of doing this.
     * @return the version number of this task
     */
    int version();

    /**
     * The scope of this startup task - whether it applies to the whole system
     * (e.g. a database schema upgrade), just the overlord domain or normal domains.
     *
     * @return the scope of this startup task
     */
    StartupTaskScope taskScope();

    /**
     * A list of other startup tasks that should be run before this task.
     * Use this to express dependency order within a component archive.
     * Dependency order among component archives is expressed by the module
     * dependencies at compile time.
     * @return the startup tasks that should be run before this task
     */
    Class<? extends StartupTask>[] runAfter() default {};
}
