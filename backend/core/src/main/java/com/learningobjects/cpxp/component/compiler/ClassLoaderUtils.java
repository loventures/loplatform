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

import org.apache.commons.lang3.StringUtils;

import javax.tools.JavaFileObject;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassLoaderUtils {
    private static final SortedMap<String, JavaFileObject> __coreClasses = new TreeMap<>();
    private static final List<String> __coreClasspath = new ArrayList<>();

    public static synchronized SortedMap<String, JavaFileObject> getCoreClasses() {
        if (__coreClasses.isEmpty()) {
            for (ClassLoader cl = ClassLoaderUtils.class.getClassLoader(); (cl instanceof URLClassLoader) && (cl != ClassLoader.getSystemClassLoader()); cl = cl.getParent()) {
                findClasses((URLClassLoader) cl, __coreClasses);
            }
        }
        return __coreClasses;
    }

    public static synchronized List<String> getCoreClasspath() {
        if (__coreClasspath.isEmpty()) {
            for (ClassLoader cl = ClassLoaderUtils.class.getClassLoader(); (cl != null); cl = cl.getParent()) {
                if (cl instanceof URLClassLoader) {
                    findClasspath((URLClassLoader) cl, __coreClasspath);
                }
            }
        }
        return __coreClasspath;
    }

    public static void findClasspath(URLClassLoader classLoader, List<String> list) {
        for (URL url : classLoader.getURLs()) {
            try {
                File file = getFile(url);
                if (file.isDirectory() || file.getName().endsWith(".jar")) {
                    list.add(file.getAbsolutePath());
                }
            } catch (Exception ignored) {
                // these often happen...
            }
        }
    }

    public static void findClasses(URLClassLoader classLoader, Map<String, JavaFileObject> map) {
        for (URL url : classLoader.getURLs()) {
            try {
                File file = getFile(url);
                if (file.isDirectory()) {
                    findDirectoryClasses(file, file.getPath(), map);
                } else if (file.getName().endsWith(".jar")) {
                    findJarClasses(file, map);
                }
            } catch (Exception ignored) {
                // these often happen...
            }
        }
    }

    private static File getFile(URL url) throws Exception {
        if (url.getFile().contains(" ")) { // Hack for Jenkins in a spacey directory
            url = new URL(url.toExternalForm().replaceAll(" ", "%20"));
        }
        return new File(url.toURI());
    }

    private static void findDirectoryClasses(File file, String prefix, Map<String, JavaFileObject> map) throws Exception {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                findDirectoryClasses(child, prefix, map);
            }
        } else if (file.getName().endsWith(".class")) {
            String fileName = file.getPath().substring(1 + prefix.length());
            String className = getClassNameFromFileName(fileName);
            map.put(className, new ClassFileObjectImpl(file.toURI(), file.toURL(), className));
        }
    }

    private static void findJarClasses(File file, Map<String, JavaFileObject> map) throws Exception {
        URL base = new URL("jar:" + file.toURL().toExternalForm() + "!/");
        ZipFile zip = new ZipFile(file);
        try {
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = getClassNameFromFileName(entry.getName());
                    URL url = new URL(base, entry.getName());
                    File fake = new File(file, entry.getName());
                    map.put(className, new ClassFileObjectImpl(fake.toURI(), url, className));
                }
            }
        } finally {
            zip.close();
        }
    }

    public static final String getClassNameFromFileName(String fileName) {

        return StringUtils.substringBeforeLast(fileName.startsWith("/") ? fileName.substring(1) : fileName, ".").replace('/', '.');
    }

}
