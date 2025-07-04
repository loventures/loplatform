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

package com.learningobjects.cpxp.util;

import com.google.common.base.Predicate;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;
import com.learningobjects.cpxp.util.resource.ResourceFilter;
import com.learningobjects.cpxp.util.resource.ZipXmlLocalizer;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class utility methods.
 */
public class ClassUtils extends org.apache.commons.lang3.ClassUtils {

    private static final Pattern LOCALE_RE = Pattern.compile("([^_]*)(_([^_]*))?(_([^_]*))?");

    // TODO: doesn't belong here
    public static Locale parseLocale(String str) {
        Matcher matcher = LOCALE_RE.matcher(str);
        if (!matcher.matches()) {
          throw new RuntimeException("Locale parse error: " + str);
        }
        String language = matcher.group(1);
        String country = StringUtils.defaultString(matcher.group(3));
        String variant = StringUtils.defaultString(matcher.group(5));
        return new Locale(language, country, variant);
    }

    public static FileHandle getResourceAsTempFile(Class clas, String resource, ResourceFilter filter) {
        FileCache cache = FileCache.getInstance();
        String cachePath = "resource/" + getShortClassName(clas) + "/" + filter.getPath() + (resource.startsWith("/") ? resource.substring(1) : resource);
        FileHandle handle = cache.getFile(cachePath);
        if (!handle.exists()) {
            try {
                URL url = clas.getResource(resource);
                if (url == null) {
                    throw new IOException("Missing resource: " + resource);
                }
                // Hack to overcome default caching of JAR resources across ear redeployments
                URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                InputStream in = conn.getInputStream();
                try {
                    FileOutputStream out = FileUtils.openOutputStream(handle.getFile());
                    try {
                        filter.filter(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
                handle.created();
            } catch (Exception ex) {
                handle.failed();
                throw new RuntimeException("Resource error", ex);
            }
        }
        return handle;
    }

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("/(.*/)(.*)\\*(.*)");

    /**
     * Hacky method to fetch the resource files matching a pattern;
     * e.g. /messages/*.properties
     */
    public static List<String> getResources(Class clas, String pattern) {
        List<String> resources = new ArrayList<String>();

        Matcher matcher = RESOURCE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid pattern: " + pattern);
        }
        String path = matcher.group(1), midfix = matcher.group(2), suffix = matcher.group(3);
        Pattern p2 = Pattern.compile(Pattern.quote(path) + Pattern.quote(midfix) + "[^/]*" + Pattern.quote(suffix));

        URL url = clas.getResource("/" + path); // this will be /foo/
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.getPath());
            if (file.exists() && file.isDirectory()) {
                for (File child : file.listFiles()) {
                    String name = path + child.getName(); // this will be foo/bar.properties
                    if (p2.matcher(name).matches()) {
                        resources.add("/" + name);
                    }
                }
            }
        } else {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jfile = conn.getJarFile();
                Enumeration e = jfile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry)e.nextElement();
                    String name = entry.getName(); // this will be foo/bar.properties
                    if (p2.matcher(name).matches()) {
                        resources.add("/" + name);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return resources;
    }

    public static Properties loadResourceAsProperties(Class clas, String resource) throws IOException {
        InputStream in = clas.getResourceAsStream(resource);
        if (in == null) {
            throw new FileNotFoundException("Unknown resource: " + resource);
        }
        try {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } finally {
            in.close();
        }
    }

    public static FileHandle getLocalizedZipResourceAsTempFile(String resource, Locale locale, String messages, Map<String, String> resourceMap) {
        String bundleName = "messages." + messages;
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale, ClassUtils.class.getClassLoader());
        if (resourceMap != null) {
            final var handle = bundle;
            bundle = new ResourceBundle() {
                {
                    parent = handle;
                }

                @Override
                protected Object handleGetObject(String key) {
                    return resourceMap.get(key);
                }

                @Override
                protected Set<String> handleKeySet() {
                    return resourceMap.keySet();
                }

                @Override
                public Enumeration<String> getKeys() {
                    return Collections.enumeration(keySet());
                }
            };
        }
        ZipXmlLocalizer filter = ZipXmlLocalizer.getInstance(bundle, bundleName + '/' + locale + '/');
        return ClassUtils.getResourceAsTempFile(ClassUtils.class, resource, filter);
    }

    public static String getOuterName(final String className) {
        final String shortName = StringUtils.substringAfterLast(className, ".");
        return shortName.startsWith("$") ? shortName : StringUtils.substringBefore(shortName, "$");
    }

    public static String getJvmName(Class<?> cls) {
        if (cls.isArray()) {
            return cls.getName().replace('.', '/'); // [Lxxxxx;
        } else if (cls.isPrimitive()) {
            if (Void.TYPE.equals(cls)) {
                return "V";
            } else if (Boolean.TYPE.equals(cls)) {
                return "Z";
            } else if (Long.TYPE.equals(cls)) {
                return "J";
            } else {
                return cls.getName().substring(0, 1).toUpperCase();
            }
        } else {
            return "L" + cls.getName().replace('.', '/') + ";";
        }
    }

    /**
     * Returns the simple name for outer classes, but a package-less name for inner
     * classes.
     *
     * <pre>
     *     getUnqualifiedName(Map.class) -> "Map"
     *     getUnqualifiedName(Map.Entry.class) -> "Map$Entry"
     * </pre>
     *
     * @param clas the class to get the unqualified name for
     * @return the class file name without its package
     */
    public static String getUnqualifiedName(@Nonnull final Class<?> clas) {

        final String simpleName = clas.getSimpleName();

        final String unqualifiedName;
        final Class<?> enclosingClass = clas.getEnclosingClass();
        if (enclosingClass != null) {
            unqualifiedName =
                    getUnqualifiedName(enclosingClass) + INNER_CLASS_SEPARATOR +
                            simpleName;
        } else {
            unqualifiedName = simpleName;
        }

        return unqualifiedName;

    }

    public static String getClassAndMethodName(final Method method) {
        if (method == null) {
            return null;
        } else {
            return method.getDeclaringClass().getName() + "#" + method.getName();
        }
    }

    /**
     * Returns a {@link Predicate} that defines classes that are assignable from the given
     * class.
     *
     * @param cls the class over which the returned {@link Predicate} closes.
     * @return a {@link Predicate} that defines classes that are assignable from the given
     * class.
     */
    public static Predicate<Class<?>> isAssignableFrom(final Class<?> cls) {
        return new Predicate<Class<?>>() {

            @Override
            public boolean apply(@Nullable final Class<?> t) {
                return t != null && t.isAssignableFrom(cls);
            }
        };
    }

    /**
     * Returns a {@link Predicate} that defines classes that are assignable to the given
     * class.
     *
     * @param cls the class over which the returned {@link Predicate} closes
     * @return a {@link Predicate} that defines classes that are assignable to the given
     * class
     */
    public static Predicate<Class<?>> isAssignableTo(@Nonnull final Class<?> cls) {
        return new Predicate<Class<?>>() {

            @Override
            public boolean apply(@Nullable final Class<?> t) {
                return t != null && cls.isAssignableFrom(t);
            }
        };
    }


    public static List<PropertyDescriptor> getAllProperties(final Class<?> clazz) {

        final List<PropertyDescriptor> props = new ArrayList<>();

        final Stream<Class<?>> types;
        if (clazz.isInterface()) {
            /*
             * PropertyUtils.getPropertyDescriptors(Class) only introspects
             * declared methods when given an interface type, for class types,
             * it introspects all methods (wat?). So for interfaces we'll walk up
             * the inheritance tree ourselves.
             */
            types = BreadthFirstSupertypeIterable.from(clazz);
        } else {
            types = Stream.of(clazz);
        }
        types.forEach(type -> props.addAll(Arrays.asList(PropertyUtils.getPropertyDescriptors(type))));

        return props;
    }

    /**
     * @return a {@link Predicate} that defines {@link AnnotatedElement}s that have an annotation of the given type
     */
    public static Predicate<AnnotatedElement> hasAnnotation(@Nonnull final Class<? extends Annotation> annotationType) {

        checkNotNull(annotationType);

        return element -> element != null && element.isAnnotationPresent(annotationType);

    }

    /**
     * Searches for an annotation of the given annotation type on {@code clazz} and its super types. The first
     * annotation found is returned, or {@link Optional#empty()} if no such annotation exists. The search order is defined
     * by {@link com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable#from(Class)}.
     *
     * @param clazz          the class whose super types are searched (this class is included in the search)
     * @param annotationType the type of annotation to find
     * @param <A>            the type of annotation to find
     * @return the first annotation of the given type in {@code clazz}'s inheritance tree, or {@link Optional#empty()} if
     * no such annotation exists.
     */
    public static <A extends Annotation> Optional<A> findAnnotation(@Nonnull final Class<?> clazz,
            @Nonnull final Class<A> annotationType) {

        final Class<?> annotatedElement = BreadthFirstSupertypeIterable.from(clazz).filter
                (hasAnnotation(annotationType)).findFirst().orElse(null);

        final Optional<A> annotation;
        if (annotatedElement != null) {
            annotation = java.util.Optional.ofNullable(annotatedElement.getAnnotation(annotationType));
        } else {
            annotation = Optional.empty();
        }

        return annotation;
    }

    public static boolean isAnnotationPresent(Class<? extends Annotation> clas, Annotation[] annotations) {
        return Arrays.stream(annotations).anyMatch(clas::isInstance);
    }

    public static <A extends Annotation> A getAnnotation(Class<A> clas, Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (clas.isInstance(a)) {
                return clas.cast(a);
            }
        }
        return null;
    }

    /**
     * Marshals an annotation into a form that can be jsonized (a map from strings
     * to simple properties). Needed because annotation method names do not follow
     * bean naming conventions.
     */
    public static Map<String, Object> marshalAnnotation(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        Map<String, Object> map = new HashMap();
        for (java.lang.reflect.Method m : annotation.getClass().getDeclaredMethods()) {
            if ((m.getParameterTypes().length > 0) || "toString".equals(m.getName()) || "hashCode".equals(m.getName()) || "annotationType".equals(m.getName())) {
                continue;
            }
            try {
                Object value = m.invoke(annotation);
                map.put(m.getName(), marshalAnnotationValue(value));
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private static Object marshalAnnotationValue(Object value) {
        if (value instanceof Annotation) {
            return marshalAnnotation((Annotation) value);
        } else if (value instanceof Class) {
            return ((Class<?>) value).getName();
        } else if (value.getClass().isArray()) {
            int n = Array.getLength(value);
            Object[] a = new Object[n];
            for (int i = 0; i < n; ++ i) {
                a[i] = marshalAnnotationValue(Array.get(value, i));
            }
            return a;
        } else {
            return value;
        }
    }

    /**
     * Returns a proxy over the specified object that implements all of
     * its interfaces plus the specified additional marker interface.
     */
    public static <T> T mark(final T object, Class<?> markerInterface) {
        if (!markerInterface.isInterface() || (markerInterface.getDeclaredMethods().length > 0)) {
            throw new IllegalArgumentException("Not a marker interface: " + markerInterface);
        }
        Class<?> objectClass = object.getClass();
        Class<?>[] objectInterfaces = objectClass.getInterfaces();
        if (objectInterfaces.length == 0) {
            throw new IllegalArgumentException("Object implements no interfaces: " + objectClass);
        }
        Class<?>[] interfaces = ArrayUtils.add(objectInterfaces, markerInterface);
        return (T) Proxy.newProxyInstance(objectClass.getClassLoader(), interfaces, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return method.invoke(object, args);
            }
        });
    }

    /**
     * Returns whether the given class has declared a field with the given name.
     *
     * @param clasz the clasz
     * @param name the name
     * @return whether clasz has a field with that name
     */
    public static boolean hasDeclaredField(Class<?> clasz, String name) {
        return Arrays.stream(clasz.getDeclaredFields()).anyMatch(f -> f.getName().equals(name));
    }

    /**
     * Returns whether the given class has declared a method with the given name.
     *
     * @param clasz the clasz
     * @param name the name
     * @return whether clasz has a method with that name (and any arity)
     */
    public static boolean hasDeclaredMethod(Class<?> clasz, String name) {
        return Arrays.stream(clasz.getDeclaredMethods()).anyMatch(f -> f.getName().equals(name));
    }
}
