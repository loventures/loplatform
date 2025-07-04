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

import com.learningobjects.cpxp.component.ComponentSource;
import com.learningobjects.cpxp.component.compiler.ClassLoaderUtils;
import com.learningobjects.cpxp.component.compiler.ComponentClassLoader;
import com.learningobjects.cpxp.util.FileUtils;
import com.learningobjects.cpxp.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BaseArchiveContents implements ArchiveContents {
    private static final Logger logger = Logger.getLogger(BaseArchiveContents.class.getName());

    private final ComponentSource _source;
    // Class names available in the archive
    private final SortedMap<String, URL> _classes = new TreeMap<>();
    // Map from class names to sources available in the archive
    private final SortedMap<String, URL> _sources = new TreeMap<>();
    // Resources available in the archive
    private final Map<String, URL> _resources = new HashMap<>();

    // The parent class loader is for static classes that we won't recompile
    // so on component env reload i can reuse this classloader and just replace
    // the dynamic classloader.
    private URLClassLoader _parentClassLoader;

    public BaseArchiveContents(ComponentSource source) {
        _source = source;
        logger.log(Level.FINE, "Scanning archive contents, {0}", _source.getIdentifier());

        List<URL> libraries = new ArrayList<>();
        try {
            _source.getResources().forEach((name, path) -> {
                try {
                    String suffix = StringUtils.substringAfterLast(name, ".").toLowerCase();
                    if ("classes/".equals(name) || name.endsWith("/classes/") || name.endsWith("/classes")
                      || "test-classes/".equals(name) || name.endsWith("/test-classes/") || name.endsWith("/test-classes")) {
                        libraries.add(path.toUri().toURL());
                    } else if (suffix.equals("jar")) {
                        File jar = path.toFile();
                        libraries.add(jar.toURL());
                        // TODO: I'd like to only scan inside component jars; flag them somehow
                        URL base = new URL("jar:" + jar.toURL().toExternalForm() + "!/");
                        try (InputStream in = FileUtils.openInputStream(jar)) {
                            ZipInputStream zip = new ZipInputStream(in);
                            ZipEntry entry;
                            while ((entry = zip.getNextEntry()) != null) {
                                String entryName = entry.getName();
                                if (entryName.endsWith(".class")) {
                                    String className = ClassLoaderUtils.getClassNameFromFileName(entryName);
                                    _classes.put(className, new URL(base, entryName));
                                }
                            }
                        }
                    } else if ("java".equals(suffix)) {
                        String className = ClassLoaderUtils.getClassNameFromFileName(name);
                        _sources.put(className, path.toUri().toURL());
                    } else if (suffix.equals("class")) {
                        String localName = StringUtils.defaultIfEmpty(StringUtils.substringAfter(name, "classes/"), name);
                        String className = ClassLoaderUtils.getClassNameFromFileName(localName);
                        _classes.put(className, path.toUri().toURL());
                    } else if (!name.endsWith("/")) {
                        URL origURL = path.toUri().toURL();
                        //Hack to work around this: https://bugs.openjdk.java.net/browse/JDK-8172846
                        //Cousier stores jars with a URL encoded path segment, ends up being broken
                        //Shouldn't affect production. Burn everything down if it does.
                        if (origURL.getProtocol().equals("jar")) {
                            _resources.put(name, new URL(URLDecoder.decode(origURL.toExternalForm(), "UTF-8")));
                        } else {
                            _resources.put(name, origURL);
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failure in scanning Archive contents", ex);
                }
            });

            //TODO: See BaseComponentEnvironment constructor. Ideally there'd be a way to be more coherent about this.
            URL[] libs = libraries.toArray(new URL[libraries.size()]);
            _parentClassLoader = new URLClassLoader(libs, findNonComponentClassLoader(Thread.currentThread().getContextClassLoader()));
        } catch (Exception e) {
            throw new RuntimeException("Failure in scanning Archive contents", e);
        }
    }

    private ClassLoader findNonComponentClassLoader(ClassLoader cl) {
        if(ComponentClassLoader.class.isInstance(cl)) {
            return findNonComponentClassLoader(cl.getParent());
        } else {
            return cl;
        }
    }

    @Override
    public boolean hasSource(String className) {
        return _sources.containsKey(className);
    }

    @Override
    public boolean isSourceNewer(String className) throws Exception {
        File source = getSourceFile(className);
        URL compiled = _classes.get(className);
        if (source == null) {
            return false;
        } else if (compiled == null) {
            return true;
        }
        // Assume if I have the source then the class is not in a jar
        File compiledFile = new File(compiled.toURI());
        return source.lastModified() > compiledFile.lastModified();
    }

    @Override
    public File getSourceFile(String className) throws Exception {
        URL source = _sources.get(className);
        if (source == null) {
            return null;
        }
        return new File(source.toURI());
    }

    @Override
    public URL getClassURL(String className) {
        return _classes.get(className);
    }

    @Override
    public boolean hasClass(String className) {
        return _classes.containsKey(className);
    }

    @Override
    public Set<String> getAllClasses() {
        Set<String> all = new HashSet<>();
        all.addAll(_classes.keySet());
        all.addAll(_sources.keySet());
        return all;
    }

    @Override
    public URLClassLoader getParentClassLoader() {
        return _parentClassLoader;
    }

    @Override
    public Map<String, URL> getResources() {
        return _resources;
    }

    @Override
    public String toString() {
        return "ArchiveContents[" + _source.toString() + "]";
    }
}
