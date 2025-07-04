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

import javax.tools.JavaFileObject;
import java.util.List;

public interface ComponentRing {
    ComponentRing getParent();

    void addArchive(ComponentArchive archive);

    ComponentArchive getArchive(String identifier);

    ComponentArchive findArchive(String identifier);

    /* All of the archives in this ring and its ancestors, sorted such that
     *every archive appears before any archives that depend upon it. */
    Iterable<ComponentArchive> getArchives();

    /* Archives only in this ring. I do not know a good use for this. */
    Iterable<ComponentArchive> getLocalArchives();

    // I use a threadlocal ComponentRing rather than a field on the archive
    // because for components that wind up not depending on other components
    // I can then reuse them in multiple rings without reloading...
    void load();

    boolean isFailed();

    void setFailed();

    void findClassFiles(ComponentArchive current, String packageName, List<JavaFileObject> files);

    Class<?> getClass(ComponentArchive current, String className);
}
