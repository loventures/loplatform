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

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A JavaFileObject which contains either the source text or the compiler
 * generated class. This class is used in two cases.
 * <ol>
 * <li>This instance uses it to store the source which is passed to the
 * compiler. This uses the
 * {@link JavaFileObjectImpl#JavaFileObjectImpl(String, CharSequence)}
 * constructor.
 * <li>The Java compiler also creates instances (indirectly through the
 * FileManagerImplFileManager) when it wants to create a JavaFileObject for the
 * .class output. This uses the
 * {@link JavaFileObjectImpl#JavaFileObjectImpl(String, JavaFileObject.Kind)}
 * constructor.
 * </ol>
 * This class does not attempt to reuse instances (there does not seem to be a
 * need, as it would require adding a Map for the purpose, and this would also
 * prevent garbage collection of class byte code.)
 */
public final class JavaFileObjectImpl extends SimpleJavaFileObject {
   // If kind == CLASS, this stores byte code from openOutputStream
   private ByteArrayOutputStream byteCode;
   private final String origin;
   private final String className;
   // if kind == SOURCE, this contains the source text
   private final CharSequence source;

   /**
    * Construct a new instance which stores source
    *
    * @param baseName
    *           the base name
    * @param source
    *           the source code
    */
    JavaFileObjectImpl(final String origin, final String className, final String baseName, final CharSequence source) {
      super(CharSequenceCompiler.toURI(baseName + CharSequenceCompiler.JAVA_EXTENSION),
            Kind.SOURCE);
      this.origin = origin;
      this.className = className;
      this.source = source;
   }

   /**
    * Construct a new instance
    *
    * @param name
    *           the file name
    * @param kind
    *           the kind of file
    */
   JavaFileObjectImpl(final String name, final Kind kind) {
      super(CharSequenceCompiler.toURI(name), kind);
      this.origin = "";
      this.className = null;
      this.source = null;
   }

    public String getClassName() {
        return className;
    }

   /**
    * Return the source code content
    *
    * @see javax.tools.SimpleJavaFileObject#getCharContent(boolean)
    */
   @Override
   public CharSequence getCharContent(final boolean ignoreEncodingErrors)
         throws UnsupportedOperationException {
      if (source == null)
         throw new UnsupportedOperationException("getCharContent()");
      return source;
   }

   /**
    * Return an input stream for reading the byte code
    *
    * @see javax.tools.SimpleJavaFileObject#openInputStream()
    */
   @Override
   public InputStream openInputStream() {
      return new ByteArrayInputStream(byteCode.toByteArray());
   }

   /**
    * Return an output stream for writing the bytecode
    *
    * @see javax.tools.SimpleJavaFileObject#openOutputStream()
    */
   @Override
   public OutputStream openOutputStream() {
      byteCode = new ByteArrayOutputStream();
      return byteCode;
   }

    public String toString() {
        return origin + "|" + toUri();
    }
}
