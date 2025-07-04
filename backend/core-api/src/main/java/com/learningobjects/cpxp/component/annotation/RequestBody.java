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
 * Declares a method parameter that receives the body of an HTTP request. The HTTP
 * request is converted to the type of the annotated parameter by finding a suitable
 * HttpMessageConverter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {

    String SINGLE_PART_NAME = "";

    /**
     * When the {@link RequestBody} target type is UploadInfo, use this value to
     * indicate the request part if the HTTP request is a multi-part request.
     *
     * <p>This is not a way to inject parts of a multi-part request. SRS does not
     * support multipart requests at this time.</p>
     */
    String part() default SINGLE_PART_NAME;

    /**
     * Whether to log this request body, if json, during parsing.
     */
    boolean log() default true;
}
