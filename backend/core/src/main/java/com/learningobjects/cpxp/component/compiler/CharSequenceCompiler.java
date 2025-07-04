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

import com.learningobjects.cpxp.component.ComponentArchive;
import com.learningobjects.cpxp.util.StringUtils;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CharSequenceCompiler  {
    private static final Logger logger = Logger.getLogger(CharSequenceCompiler.class.getName());
    // Compiler requires source files with a ".java" extension:
    static final String JAVA_EXTENSION = ".java";

    private final String origin;

    private final BaseComponentClassLoader classLoader;

    // The compiler instance that this facade uses.
    private final JavaCompiler compiler;

    // collect compiler diagnostics in this instance.
    private DiagnosticCollector<JavaFileObject> diagnostics;

    // The FileManager which will store source and class "files".
    public/*private*/ final FileManagerImpl javaFileManager;

    /**
     * Construct a new instance which delegates to the named class loader.
     *
     * @param loader
     *           the application ClassLoader. The compiler will look through to
     *           this // class loader for dependent classes
     * @throws IllegalStateException
     *            if the Java compiler cannot be loaded.
     */
    public CharSequenceCompiler(ComponentArchive archive, BaseComponentClassLoader loader) {
        this.origin = archive.getIdentifier();
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. " + "Check that your class path includes tools.jar");
        }
        classLoader = loader;
        diagnostics = new DiagnosticCollector<JavaFileObject>();
        final JavaFileManager fileManager = compiler.getStandardFileManager(diagnostics,
                null, null);
        // create our FileManager which chains to the default file manager
        // and our ClassLoader
        javaFileManager = new FileManagerImpl(fileManager, classLoader, archive);
    }

    /**
     * Compile multiple Java source strings and return a Map containing the
     * resulting classes.
     * <p>
     * Thread safety: this method is thread safe if the <var>classes</var> and
     * <var>diagnosticsList</var> are isolated to this thread.
     *
     * @param classes
     *           A Map whose keys are qualified class names and whose values are
     *           the Java source strings containing the definition of the class.
     *           A map value may be null, indicating that compiled class is
     *           expected, although no source exists for it (it may be a
     *           non-public class contained in one of the other strings.)
     * @return A mapping of qualified class names to their corresponding classes.
     *         The map has the same keys as the input <var>classes</var>; the
     *         values are the corresponding Class objects.
     */
    public synchronized Map<String, Class<?>> compile(Map<String, CharSequence> classes, Object unused, Map<String, File> fileNames) throws Exception {
        Map<String, Class<?>> compiled = new HashMap<>();
        if (classes.isEmpty()) {
            return compiled;
        }
            List<JavaFileObject> sources = new ArrayList<>();
            for (Entry<String, CharSequence> entry : classes.entrySet()) {
                String qualifiedClassName = entry.getKey();
                CharSequence javaSource = entry.getValue();
                if (javaSource != null) {
                    final int dotPos = qualifiedClassName.lastIndexOf('.');
                    final String className = dotPos == -1 ? qualifiedClassName
                        : qualifiedClassName.substring(dotPos + 1);
                    final String packageName = dotPos == -1 ? "" : qualifiedClassName
                        .substring(0, dotPos);
                    final JavaFileObjectImpl source = new JavaFileObjectImpl(origin, qualifiedClassName, className,
                            javaSource);
                    sources.add(source);
                    // Store the source file in the FileManager via package/class
                    // name.
                    // For source files, we add a .java extension
                    javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName,
                            className + JAVA_EXTENSION, source);
                }
            }
            final boolean debug = false;
            Collection<String> options = debug ? Collections.singleton("-g") :
                    Collections.singleton("-g:lines,vars");
            // Get a CompliationTask from the compiler and compile the sources
            final CompilationTask task = compiler.getTask(null, javaFileManager, diagnostics,
                    options, null, sources);
            final Boolean result = task.call();
            if (result == null || !result.booleanValue()) {
                List<String> names = new ArrayList<String>();
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    JavaFileObjectImpl source = (JavaFileObjectImpl) d.getSource();
                    if (source == null) {
                        logger.log(Level.WARNING, "javac: " + d);
                    } else {
                        long lineNumber = d.getLineNumber();
                        names.add(source.getName() + ":" + lineNumber);
                        logger.log(Level.WARNING, "javac " + source.getName() + " line " + lineNumber + ": " + d.getMessage(null));
                    }
                }
                throw new Exception("Compilation error" + (names.isEmpty() ?
                        "" : " (" + StringUtils.join(names, ", ") + ")"));
            }
            try {
                for (String qualifiedClassName : classes.keySet()) {
                    JavaFileObject fileObject = classLoader.getClassFiles().get(qualifiedClassName);
                    if (fileObject == null) {
                        throw new Exception("Wrong package: " + qualifiedClassName);
                    }

                    File infile = (fileNames == null) ? null : fileNames.get(qualifiedClassName);
                    String outfilename = qualifiedClassName.replaceFirst(
                            "^(?:.*\\.)?([^.]+)$", "$1.java");
                    String infilename = (infile == null) ? outfilename : infile.getName(); // was getAbsolutePath but it makes messy traces. not sure what is right. absolute path in dev?

                    BytecodeEngineer.engineer(fileObject, infilename, null);
                }

                // For each class name in the inpput map, get its compiled
                // class and put it in the output map
                for (String qualifiedClassName : classes.keySet()) {
                    final Class<?> newClass = loadClass(qualifiedClassName);
                    compiled.put(qualifiedClassName, newClass);
                }
                return compiled;
            } catch (Exception e) {
                throw new Exception("Compile error", e);
            }
        }

    /**
     * Load a class that was generated by this instance or accessible from its
     * parent class loader. Use this method if you need access to additional
     * classes compiled by
     * {@link #compile(String, CharSequence, DiagnosticCollector, Class...) compile()},
     * for example if the primary class contained nested classes or additional
     * non-public classes.
     *
     * @param qualifiedClassName
     *           the name of the compiled class you wish to load
     * @return a Class instance named by <var>qualifiedClassName</var>
     * @throws ClassNotFoundException
     *            if no such class is found.
     */
    @SuppressWarnings("unchecked")
    public Class<?> loadClass(final String qualifiedClassName)
        throws ClassNotFoundException {
        return classLoader.loadClass(qualifiedClassName);
    }

    /**
     * COnverts a String to a URI.
     *
     * @param name
     *           a file name
     * @return a URI
     */
    static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
