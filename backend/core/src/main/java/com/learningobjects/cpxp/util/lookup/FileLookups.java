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

package com.learningobjects.cpxp.util.lookup;

import com.learningobjects.cpxp.util.TempFileMap;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FileLookups {

    public static FileLookup lookup(final TempFileMap tempFileMap) {
        checkNotNull(tempFileMap);

        class TempFileMapAdapter implements FileLookup {
            final Lookup<String, File> delegate = Lookups.lookup((Map<String, File>) tempFileMap);

            @Override
            public Optional<File> get(String key) {
                return delegate.get(key);
            }

            @Override
            public Iterator<Entry<String, File>> iterator() {
                return delegate.iterator();
            }

            @Override
            public Collection<String> keySet() {
                return delegate.keySet();
            }

            @Override
            public Optional<FileLookup> getZipAsFileLookup(String name) {
                checkNotNull(name);
                checkArgument(name.endsWith(".zip"), "not a zip file");

                return get(name).flatMap(zip -> {
                    final TempFileMap map = tempFileMap.newTempFileMap();
                    try {
                        map.importZip(zip);
                        return Optional.ofNullable(lookup(map));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                });
            }

            @Override
            public void close() {
                tempFileMap.close();
            }
            
            @Override
            public File createFile(String key) throws IOException {
                return tempFileMap.create(key);
            }
        }

        return new TempFileMapAdapter();
    }
}
