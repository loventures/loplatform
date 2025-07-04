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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComponentDirectory implements ComponentSource {
    private final File _root;
    private final String _identifier;

    public ComponentDirectory(File root) {
        this(root, root.getPath());
    }

    public ComponentDirectory(File root, String identifier) {
        _root = root;
        _identifier = identifier;
    }

    public File getRoot() {
        return _root;
    }

    @Override
    public ComponentCollection getCollection() {
        return null; // TODO
    }

    @Override
    public String getIdentifier() {
        return _identifier;
    }

    @Override
    public String getVersion() {
        return "0.0";
    }

    @Override
    public long getLastModified() {
        return _root.lastModified();
    }

    @Override
    public Map<String, Path> getResources() throws IOException {
        int stripLength = _root.getPath().length() + 1; // +1 for trailing slash
        return Files.walk(_root.toPath())
          .filter(path -> !_root.toPath().equals(path))
          .collect(Collectors.toMap(path -> path.toString().substring(stripLength), Function.identity()));
    }

    @Override
    public Optional<Path> getResource(String name) {
        Path resolved = _root.toPath().resolve(name);
        if (Files.exists(resolved)) {
            return Optional.of(resolved);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public long getLastModified(String name) throws IOException {
        Optional<Path> pathMaybe = getResource(name);
        if(pathMaybe.isPresent()) {
            return Files.getLastModifiedTime(pathMaybe.get()).toMillis();
        } else {
            return -1L;
        }
    }

    @Override
    public String toString() {
        return "ComponentDirectory[{" + _root.getAbsolutePath() + "}/" + _identifier + "]";
    }
}
