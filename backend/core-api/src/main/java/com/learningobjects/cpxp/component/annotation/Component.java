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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation currently serving two not entirely equal roles.
 * 1. Used to represent a business component backed by a facade and can be created by ComponentSupport.getInstance()
 * 2. Used on managed components that can be turned on or off through "Managed Components" in the admin portal.
 * Fundamentally, @Component will put an entry in a component registry (which is used by such things as the managed
 * components admin portal and the item type mapping).
 *
 * Although things annotated with @Component will have members dependency injected into them, this annotation
 * should not be used solely to get dependency injection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface Component {
    /**
     * A human readable name for the component,
     * which will help Administrators identify it on the "Manage Components" screen
     */
    @MessageValue
    String name() default "";
    /** The component version. */
    String version() default "";
    /** The component description. */
    @MessageValue
    String description() default "";
    /** The component icon. */
    String icon() default "";
    /** An old alias for the component.  Unless a business component's @ItemMapping uses 'singleton = true' or
     * 'schemaMapped = true', its full class name will be stored in the database and retrieval will
     * break if you change the class name.
     * Use this annotation to list old class names that might still be in the database. */
    String[] alias() default {};
    /** A context testing method. */
    String context() default "*";
    /** Indicates default enabled */
    boolean enabled() default true;
    /** Other components that must be bootstrapped first. */
    Class<?>[] dependencies() default {};
    /** Other components that this suppresses. */
    Class<?>[] suppresses() default {};
    /** Has json i18n files. */
    boolean i18n() default false;

    /** Not the API you're looking for.      */
    String implementation() default "";
}
