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

import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.component.ComponentArchive;
import com.learningobjects.cpxp.component.ComponentRing;
import com.learningobjects.cpxp.service.Current;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A JavaFileManager which manages Java source and classes. This FileManager
 * delegates to the JavaFileManager and the ComponentClassLoader provided in the
 * constructor. The sources are all in memory CharSequence instances and the
 * classes are all in memory byte arrays.
 */
public final class FileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {
    private ComponentArchive _archive;

   // the delegating class loader (passed to the constructor)
   private final ComponentClassLoader classLoader;

   // Internal map of filename URIs to JavaFileObjects.
   private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();

   /**
    * Construct a new FileManager which forwards to the <var>fileManager</var>
    * for source and to the <var>classLoader</var> for classes
    *
    * @param fileManager
    *           another FileManager that this instance delegates to for
    *           additional source.
    * @param classLoader
    *           a ClassLoader which contains dependent classes that the compiled
    *           classes will require when compiling them.
    */
   public FileManagerImpl(JavaFileManager fileManager, ComponentClassLoader classLoader, ComponentArchive archive) {
      super(fileManager);
      this.classLoader = classLoader;
      _archive = archive;
   }

   /**
    * @return the class loader which this file manager delegates to
    */
   public ClassLoader getClassLoader() {
      return (ClassLoader) classLoader;
   }

   /**
    * For a given file <var>location</var>, return a FileObject from which the
    * compiler can obtain source or byte code.
    *
    * @param location
    *           an abstract file location
    * @param packageName
    *           the package name for the file
    * @param relativeName
    *           the file's relative name
    * @return a FileObject from this or the delegated FileManager
    * @see javax.tools.ForwardingJavaFileManager#getFileForInput(javax.tools.JavaFileManager.Location,
    *      java.lang.String, java.lang.String)
    */
   @Override
   public FileObject getFileForInput(JavaFileManager.Location location, String packageName,
         String relativeName) throws IOException {
      FileObject o = fileObjects.get(uri(location, packageName, relativeName));
      System.out.println("GET: " + location + " / " + packageName + " / " + relativeName + " / " + (o != null));
      if (o != null)
         return o;
      return super.getFileForInput(location, packageName, relativeName);
   }

   /**
    * Store a file that may be retrieved later with
    * {@link #getFileForInput(javax.tools.JavaFileManager.Location, String, String)}
    *
    * @param location
    *           the file location
    * @param packageName
    *           the Java class' package name
    * @param relativeName
    *           the relative name
    * @param file
    *           the file object to store for later retrieval
    */
   public void putFileForInput(StandardLocation location, String packageName,
         String relativeName, JavaFileObject file) {
      fileObjects.put(uri(location, packageName, relativeName), file);
   }

   /**
    * Convert a location and class name to a URI
    */
   private URI uri(JavaFileManager.Location location, String packageName, String relativeName) {
      return CharSequenceCompiler.toURI(location.getName() + '/' + packageName + '/'
            + relativeName);
   }

   /**
    * Create a JavaFileImpl for an output class file and store it in the
    * classloader.
    *
    * @see javax.tools.ForwardingJavaFileManager#getJavaFileForOutput(javax.tools.JavaFileManager.Location,
    *      java.lang.String, javax.tools.JavaFileObject.Kind,
    *      javax.tools.FileObject)
    */
   @Override
   public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String qualifiedName,
         Kind kind, FileObject outputFile) {
      JavaFileObject file = new JavaFileObjectImpl(qualifiedName, kind);
      classLoader.addClass(qualifiedName, file);
      return file;
   }

   @Override
   public ClassLoader getClassLoader(JavaFileManager.Location location) {
      return (ClassLoader) classLoader;
   }

   @Override
   public String inferBinaryName(JavaFileManager.Location loc, JavaFileObject file) {
      String result;
      // For our JavaFileImpl instances, return the file's name, else
      // simply run the default implementation
      if ((file instanceof JavaFileObjectImpl) || (file instanceof ClassFileObjectImpl)) {
         result = file.getName();
      } else {
         result = super.inferBinaryName(loc, file);
      }
      return result;
   }

   @Override
   public Iterable<JavaFileObject> list(JavaFileManager.Location location, String packageName,
         Set<Kind> kinds, boolean recurse) throws IOException {
       if (recurse) {
           throw new RuntimeException("Recursive listing unsupported: " + location + "/" + packageName);
       }
      Iterable<JavaFileObject> result = super.list(location, packageName, kinds,
            recurse);
      ArrayList<JavaFileObject> files = new ArrayList<JavaFileObject>();
      if (location == StandardLocation.CLASS_PATH
            && kinds.contains(JavaFileObject.Kind.CLASS)) {
         for (JavaFileObject file : fileObjects.values()) {
             if (file.getKind() == Kind.CLASS && file.getName().startsWith(packageName)) {
               files.add(file);
             }
         }
         ComponentRing ring = Current.get(ComponentRing.class);
         ring.findClassFiles(_archive, packageName, files);
      } else if (location == StandardLocation.SOURCE_PATH
            && kinds.contains(JavaFileObject.Kind.SOURCE)) {
         for (JavaFileObject file : fileObjects.values()) {
            if (file.getKind() == Kind.SOURCE && file.getName().startsWith(packageName))
               files.add(file);
         }
      }
      return Iterables.concat(result, files);
   }
}
