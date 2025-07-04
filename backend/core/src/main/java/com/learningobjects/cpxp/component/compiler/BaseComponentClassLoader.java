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
import com.learningobjects.cpxp.component.ComponentRing;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * A custom ClassLoader which maps class names to JavaFileObjectImpl instances.
 */
public final class BaseComponentClassLoader extends ClassLoader implements ComponentClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(BaseComponentClassLoader.class);

    private final ComponentArchive _archive;
    private final Map<String, URL> _resources;
    private final SortedMap<String, JavaFileObject> _classFiles = new TreeMap<>();
    private final ComponentRing _ring;

    public BaseComponentClassLoader(ComponentArchive archive, URLClassLoader parentClassLoader, ComponentRing ring) {
        super(parentClassLoader);
        _archive = archive;
        _resources = archive.getArchiveContents().getResources();
        _ring = ring;
    }

    @Override
    public boolean containsClass(String name) {
        return _classFiles.containsKey(name);
    }

    @Override
    public SortedMap<String, JavaFileObject> getClassFiles() {
        return _classFiles;
    }

    @Override
    public Package findPackage(final String name) {
        return super.getDefinedPackage(name);
    }

    @Override
    public URL findResource(final String name) {
        return _resources.containsKey(name) ? _resources.get(name) : super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        return _resources.containsKey(name) ? Collections.enumeration(Arrays.asList(_resources.get(name)))
          : super.findResources(name);
    }

    @Override
    public Class<?> getClass(String name) {
        Class<?> clas = findLoadedClass(name);
        if (clas == null) {
            try {
                clas = getParent().loadClass(name);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                clas = getLocalClass(name);
            }
        }
        return clas;
    }

    // todo: I imagine this could be simplified dramatically
    private Class<?> getLocalClass(String name) {
        JavaFileObject file = _classFiles.get(name);
        if (file == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream stream = file.openInputStream()) {
            IOUtils.copy(stream, baos);
        } catch (Exception | VerifyError ex) {
            throw new RuntimeException(ex);
        }
        byte[] bytes = baos.toByteArray();
        String packageName = StringUtils.substringBeforeLast(name, ".");
        if (getDefinedPackage(packageName) == null) {
            definePackage(packageName, null, null, null, null, null, null, null);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clas = getLocalClass(name);
        if (clas == null) {
            clas = _ring.getClass(_archive, name);
            if (clas == null) {
                throw new ClassNotFoundException("Not found: " + name);
            }
        }
        return clas;
    }

    /**
     * Add a class name/JavaFileObject mapping
     *
     * @param qualifiedClassName the name
     * @param javaFile           the file associated with the name
     */
    @Override
    public void addClass(final String qualifiedClassName, final JavaFileObject javaFile) {
        _classFiles.put(qualifiedClassName, javaFile);
    }

    @Override
    public void addResource(final String qualifiedClassName, final URL url) {
        _resources.put(qualifiedClassName, url);
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        if (name.endsWith(".class")) { // TODO: Is this necessary? Shouldn't I ask parent first?
            String qualifiedClassName = StringUtils.removeEnd(name, ".class").replace('/', '.');
            JavaFileObject file = _classFiles.get(qualifiedClassName);
            if (file != null) {
                try {
                    return file.openInputStream();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return super.getResourceAsStream(name);
    }
}
