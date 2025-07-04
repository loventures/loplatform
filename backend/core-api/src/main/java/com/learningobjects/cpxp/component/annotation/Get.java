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
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.RpcMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes a function to be accessible via an RPC call as a GET request
 * (NOTE: This is part of an old paradigm and not encouraged for use in new code)
 *
 * This can be accessed by using the $$rpc function and passing in the name, for example:
 *     "$$rpc('appevents')"
 * Alternatively, and more commonly, this can be accessed by getting the base RPC object (from $$rpc('')) and adding on the method name
 * For example, to invoke 'appevents' on AppEventView.java:
 *     _rpcBase = "$$rpc('')"
 *     ...
 *     "sAjaxSource": _rpcBase + 'appevents',
 * Note that the $$rpc(...) class is implied by context, because appEventView.html was invoked from the 'render' call in AppEventView.java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Function
@RpcMethod(Method.GET)
public @interface Get {
}
