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

import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.component.compiler.ClassLoaderUtils;
import com.learningobjects.cpxp.util.Dagly$;

import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class BaseComponentRing implements ComponentRing {
    private final ComponentRing _parent;
    private final Map<String, ComponentArchive> _archives = new HashMap<>();
    private boolean _failed;
    private Iterable<ComponentArchive> _sortedArchives = null;

    // TODO: Maintain a package->archive multimap

    BaseComponentRing(ComponentRing parent) {
        _parent = parent;
    }

    @Override
    public ComponentRing getParent() {
        return _parent;
    }

    @Override
    public synchronized void addArchive(ComponentArchive archive) {
        _archives.put(archive.getIdentifier(), archive);
        _sortedArchives = null;
    }

    @Override
    public ComponentArchive getArchive(String identifier) {
        return _archives.get(identifier);
    }

    @Override
    public ComponentArchive findArchive(String identifier) {
        ComponentArchive archive = _archives.get(identifier);
        return ((archive == null) && (_parent != null)) ? _parent.findArchive(identifier) : archive;
    }

    @Override
    public synchronized Iterable<ComponentArchive> getArchives() {
        if (_sortedArchives == null) {
            Iterable<ComponentArchive> rawArchives =
              _parent == null ? _archives.values() :
                Iterables.concat(_parent.getArchives(), _archives.values());
            _sortedArchives =
              Dagly$.MODULE$.sort(rawArchives, ComponentArchive::getAllDependencies);
        }
        return _sortedArchives;
    }

    @Override
    public Iterable<ComponentArchive> getLocalArchives() {
        return _archives.values();
    }

    @Override
    public void load() {
        for (ComponentArchive archive : getArchives()) {
            archive.scan(this);
        }
        for (ComponentArchive archive : getArchives()) {
            archive.load(this);
        }
    }

    @Override
    public boolean isFailed() {
        return _failed;
    }

    @Override
    public void setFailed() {
        _failed = true;
    }

    // TODO: A Ring ClassLoader.. That way components in ring1 can have ring0 classloader as their
    // parent which is important because it would allow ring0 supporting jars to take precedence
    // overs those same jars deployed into a ring1 component.

    @Override
    public void findClassFiles(ComponentArchive current, String packageName, List<JavaFileObject> files) {
        addFiles(ClassLoaderUtils.getCoreClasses(), packageName, files);
        for (ComponentArchive archive : current.getAllDependencies()) {
            addFiles(archive.getClassFiles(), packageName, files);
            addFiles(archive.getClassLoader().getClassFiles(), packageName, files);
        }
    }

    @Override
    public Class<?> getClass(ComponentArchive current, String className) {
        // The CAR classloaders are currently siblings so this does not
        // currently just magically work. If we used explicit dependencies
        // to form a classloader hierarchy then this would go away.
        for (ComponentArchive archive : current.getAllDependencies()) {
            Class<?> clas = archive.getClassLoader().getClass(className);
            if (clas != null) {
                return clas;
            }
        }
        return null;
    }

    // These methods assumes capital class names and lower package names so a package class will always preceed a subpackage

    private void addFiles(SortedMap<String, JavaFileObject> map, String packageName, List<JavaFileObject> files) {
        files.addAll(map.subMap(packageName + ".", packageName + ".a").values());
    }
}
