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

package com.learningobjects.de.style;

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.controller.domain.DomainAppearance;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface StyleCompiler {
    Set<String> STYLE_EXTENSIONS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("css", "scss", "sass")));

    /**
     * This function compiles style sheets from a supported language to static, minified css
     * that can be served to a client.
     * @param request The contextual request that is requesting the compiled style file.
     * @param path The source file from which to derive the compiled file.
     * @param component The component which this style is associated with.
     * @param out An OutputStream which the result should be written to.
     * @param resultFileName The name of the file being requested
     * @throws IOException
     * @throws StyleCompileException
     */
    public void compileStyle(HttpServletRequest request, Path path, ComponentDescriptor component, OutputStream out, String resultFileName, DomainAppearance appearance) throws IOException, StyleCompileException;

}
