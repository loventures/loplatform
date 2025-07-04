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

package com.learningobjects.cpxp.schedule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to flag a method for execution at a regular schedule by
 * the timer service.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
public @interface Scheduled {
    /**
     * How often to schedule the task. This can be an interval
     * such as "5 minutes" or "3 hours" or a time of day such
     * as "03:23" (UTC). The value "Never" indicates that the
     * task should not run by default. Intervals are fuzzed
     * somewhat to avoid stampedes.
     *
     * This can be overridden on appserver with a context.xml
     * environment variable such as:
     *
     * <Environment name="Scheduler/com.elsevier.evolve.EvolveService/EvolveService.cacheRefresh" value="03:00" type="java.lang.String" override="true" />
     *
     * @see com.learningobjects.cpxp.util.DateUtils#parseDuration
     */
    public String value();

    /**
     * Whether this should be a singleton and only executed on a single
     * node.
     */
    public boolean singleton() default false;

    /**
     * Whether this should be executed once per domain. Currently only in components.
     */
    public boolean domain() default false;
}
