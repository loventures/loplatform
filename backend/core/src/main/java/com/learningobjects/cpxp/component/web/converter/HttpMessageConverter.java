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

package com.learningobjects.cpxp.component.web.converter;

import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.Stateless;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Strategy interface for converters that convert HTTP entities to and from our own
 * objects.
 *
 * <p>
 * Making an HTTP message converter for some specific Java type is discouraged, unless
 * the Java type is domain-agnostic. Good examples of an HTTP message converter include
 * a Jackson converter than can convert any Jackson-annotated Java type, or a zip
 * converter can convert a zip payload to a FileLookup. Further translation to
 * domain-specific types (like taking a FileLookup, grabbing one of its files and
 * converting it into an Asset resource) should be done in the @RequestMapping method.
 * Using the HTTP message converter layer for domain-specific translation is a feature of
 * last resort.
 * </p>
 */
@Stateless
public interface HttpMessageConverter<T> extends ComponentInterface {

    List<MediaType> getSupportedMediaTypes();

    boolean canRead(final Type type, final MediaType mediaType);

    boolean canWrite(final Object value, final MediaType mediaType);

    /**
     * Read an {@link HttpServletRequest} into a new {@code target} instance
     *
     * @param request read source
     * @param target target type, a new object will be created of this type
     * @return the {@link HttpServletRequest} converted to a {@code target}
     */
    T read(final RequestBody requestBody, final WebRequest request, final Type target);

    /**
     * Write source to the {@link HttpServletResponse}
     *
     * @param source source
     * @param options conversion options
     * @param request request
     * @param response sink
     */
    void write(final T source, final ConvertOptions options,
            final HttpServletRequest request, final HttpServletResponse response);

}
