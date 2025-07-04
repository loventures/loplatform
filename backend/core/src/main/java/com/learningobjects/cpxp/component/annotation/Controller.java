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

package com.learningobjects.cpxp.component.annotation;

import com.fasterxml.jackson.annotation.JsonValue;
import com.learningobjects.cpxp.component.registry.Binding;
import com.learningobjects.cpxp.component.registry.RootControllerRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class whose behavior is exposed by ApiDispatcherServlet. Required on classes
 * that have @RequestMappings.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Binding(registry= RootControllerRegistry.class)
public @interface Controller {

    /**
     * A name, unique in all {@link Controller}s loaded in the component environment, that
     * describes the behavior of the {@link Controller}, for use in the documentation site
     */
    String value() default "";

    /**
     * Set {@code root} to true if this controller can operate without being bound to an
     * Item (i.e. @Instance injects nothing). A method on a root {@link Controller} is
     * always the first method in the handler chain for a request.
     */
    boolean root() default false;

    /**
     * The controller's category in the documentation site.
     */
    Category category() default Category.UNCATEGORIZED;

    enum Category {

        CORE("core"),
        CONTEXTS("contexts"),
        ASSETS("assets"),
        ASSET_MANAGEMENT("asset-management"),
        ASSESSMENTS("assessments"),
        API_SUPPORT("api-support"),
        COLLABORATION("collaboration"),
        LICENSING("licensing"),
        USERS("users"),
        TAXONOMIES("taxonomies"),
        UNCATEGORIZED("uncategorized"),
        MASTERY("mastery"),
        ANALYTICS("analytics");

        private final String name;

        Category(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

    }

}
