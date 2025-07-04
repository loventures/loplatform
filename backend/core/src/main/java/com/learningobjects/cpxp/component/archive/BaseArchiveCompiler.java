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

package com.learningobjects.cpxp.component.archive;

import com.learningobjects.cpxp.component.ComponentArchive;
import com.learningobjects.cpxp.component.ComponentRing;
import com.learningobjects.cpxp.component.annotation.Archive;
import com.learningobjects.cpxp.component.compiler.BaseComponentClassLoader;
import com.learningobjects.cpxp.component.compiler.CharSequenceCompiler;
import com.learningobjects.cpxp.component.compiler.ClassFileObjectImpl;
import com.learningobjects.cpxp.component.compiler.ComponentClassLoader;
import com.learningobjects.cpxp.util.FileUtils;
import com.learningobjects.cpxp.util.StringUtils;

import javax.tools.JavaFileObject;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for loading classes packaged with a component,
 * compiling any sources found that post-date their compiled artifacts.
 *
 * In an environment free of dynamic compilation this would degenerate to
 * just loading classes.
 */
public class BaseArchiveCompiler  implements ArchiveCompiler {

    private static Logger logger = Logger.getLogger(BaseArchiveCompiler.class.getName());

    private final ComponentArchive _archive;
    private final ArchiveContents _contents;
    private final BaseComponentClassLoader _classLoader;
    private final CharSequenceCompiler _compiler;
    private final SortedMap<String, JavaFileObject> _classFiles = new TreeMap<>();

    public BaseArchiveCompiler(ComponentArchive archive, ComponentRing ring) {
        _archive = archive;
        _contents = archive.getArchiveContents();
        _classLoader = new BaseComponentClassLoader(archive, _contents.getParentClassLoader(), ring);
        _compiler = new CharSequenceCompiler(archive, _classLoader);
    }

    @Override
    public ComponentClassLoader getClassLoader() {
        return _classLoader;
    }

    @Override
    public SortedMap<String, JavaFileObject> getClassFiles() {
        return _classFiles;
    }

    @Override
    public Archive loadArchiveAnnotation() throws Exception {
        logger.log(Level.FINE, "Loading archive annotation, {0}", _archive.getIdentifier());
        String className = _archive.getIdentifier() + ".package-info";
        loadClasses(Collections.singleton(className));
        Package pkg = _classLoader.findPackage(_archive.getIdentifier());
        return (pkg == null) ? null : pkg.getAnnotation(Archive.class);
    }

    @Override
    public Set<Class<?>> loadAllClasses() throws Exception {
        logger.log(Level.FINE, "Loading all classes, {0}", _archive.getIdentifier());
        Set<String> classNames = _contents.getAllClasses(); // REALLY or iterables.concat???
        classNames.remove(_archive.getIdentifier() + ".package-info");
        return loadClasses(classNames);
    }

    private Set<Class<?>> loadClasses(Set<String> classNames) throws Exception {
        Set<String> compile = new HashSet<>();
        for (String className : classNames) {
            if (_contents.isSourceNewer(className)) {
                compile.add(className);
            } else {
                String baseClass = StringUtils.substringBefore(className, "$");
                if (className.equals(baseClass) || !compile.contains(baseClass)) {
                    URL url = _contents.getClassURL(className);
                    if (url == null) {
                        continue;
                    }
                    // BUG: jar:file:/path/to throws an exception in toURI()
                    URI uri = "jar".equals(url.getProtocol()) ? new URI(url.toExternalForm().substring(4)) : url.toURI();
                    JavaFileObject jf = new ClassFileObjectImpl(uri, url, className);
                    //_classes.put(className, jf); // for javac
                    _classLoader.addClass(className, jf); // for the ClassLoaderUtils
                }
            }
        }
        if (!compile.isEmpty()) {
            compileClasses(compile);
        }
        Set<Class<?>> classes = new HashSet<>();
        for (String className : classNames) {
            // proxies etc not needed
            if (!className.contains("$$")) {
                try {
                    classes.add(_classLoader.getClass(className));
                } catch (NoClassDefFoundError ignored) {
                    logger.warning("Couldn't load "+className+" from "+ _archive.getIdentifier());
                }
            }
        }
        return classes;
    }

    private void compileClasses(Set<String> classNames) throws Exception {
        logger.log(Level.FINE, "Compiling classes, {0}, {1}", new Object[]{_archive.getIdentifier(), classNames});
        long then = System.currentTimeMillis();
        Map<String, File> files = new HashMap<>();
        Map<String, CharSequence> sources = new HashMap<>();
        for (String className : classNames) {
            File file = _contents.getSourceFile(className);
            files.put(className, file);
            String sourceCode = FileUtils.readFileToString(file, "UTF-8");
            sources.put(className, sourceCode);
        }
        then = System.currentTimeMillis();
        _compiler.compile(sources, null, files);
        long delta = System.currentTimeMillis() - then;
        logger.log(Level.INFO, "Javac: " + _archive.getIdentifier() + " - " + StringUtils.niceList(sources.keySet()) + " in " + delta + " ms");
    }

    public void compile(Map<String, CharSequence> sources) throws Exception {
        _compiler.compile(sources, null, null);
    }

}
