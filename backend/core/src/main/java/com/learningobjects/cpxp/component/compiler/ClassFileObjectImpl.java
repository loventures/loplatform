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

package com.learningobjects.cpxp.component.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class ClassFileObjectImpl extends SimpleJavaFileObject {
    private final URL _url;
    private final String _name;

    public ClassFileObjectImpl(URI uri, URL url, String name) {
        super(uri, Kind.CLASS);
        _url = url;
        _name = name;
    }

   @Override
   public InputStream openInputStream() throws IOException {
       return _url.openStream();
   }

    /** The class name */
    @Override
    public String getName() {
        return _name;
    }
}
