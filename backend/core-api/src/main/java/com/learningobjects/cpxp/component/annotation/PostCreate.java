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

import com.learningobjects.cpxp.component.function.Function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called after an instance of a component is created and persisted to the database. Should be
 * used to initialize the database state of a newly created component instance. This is the closest approximation
 * we have to a proper constructor that is aware of the Component Environment.
 * <br>
 * Contrast with {@link javax.annotation.PostConstruct} which is invoked after a JVM instance is loaded into memory
 * and DI has occurred.
 * <br>
 * Parameters on a PostCreate method are inferred using the {@link com.learningobjects.cpxp.component.eval.InferEvaluator}
 * which looks in {@link com.learningobjects.cpxp.service.Current} for any objects with types matching the parameters.
 * There are a few ways to inject the params into a PostCreate method.
 * <br>
 * First, regular DI types such as services can be used as expected. In some cases, such as group creation
 * (see ManageGroup#submitGroup) we add to Current manually. Finally, if a Facade ADD method has parameters,
 * these parameters will be added to Current by {@link com.learningobjects.cpxp.service.facade.FacadeComponentHandler}
 * before triggering the PostCreate Lifecycle (see TaxonomyParentFacade#addTaxonomy). After the add, they are removed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Function
public @interface PostCreate {
}
