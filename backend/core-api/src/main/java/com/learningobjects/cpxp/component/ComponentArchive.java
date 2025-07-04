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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.component.annotation.Archive;
import com.learningobjects.cpxp.component.archive.ArchiveCompiler;
import com.learningobjects.cpxp.component.archive.ArchiveContents;
import com.learningobjects.cpxp.component.compiler.ComponentClassLoader;
import org.apache.commons.lang3.tuple.Pair;

import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.util.*;

public interface ComponentArchive {
    String getIdentifier();

    ComponentSource getSource();

    long getLastModified();

    Archive getArchiveAnnotation();

    void addDependency(ComponentArchive archive);

    Set<ComponentArchive> getDependencies();

    Set<ComponentArchive> getAllDependencies();

    Optional<ComponentArchive> getImplementation();

    void setImplementation(ComponentArchive impl);

    ArchiveContents getArchiveContents();

    void scan(ComponentRing ring);

    ArchiveCompiler getArchiveCompiler();

    ComponentClassLoader getClassLoader();

    SortedMap<String, JavaFileObject> getClassFiles();

    Iterable<Class<?>> getLoadedClasses();

    List<ComponentDescriptor> getComponents();

    Set<Class<?>> getBoundClasses();

    List<Pair<Annotation, Class<?>>> getResourceBoundClasses();

    Map<String, ComponentDescriptor> getAliasMap();

    /** Load the archive, using the given ring to find dependencies.
     * @param ring
     */
    void load(ComponentRing ring);

    boolean isStale();
}
