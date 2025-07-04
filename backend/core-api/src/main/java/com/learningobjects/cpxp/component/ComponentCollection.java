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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ComponentCollection {
    ComponentCollection NONE = new DummyComponentCollection("None");
    ComponentCollection CLUSTER = new DummyComponentCollection("Cluster");

    class DummyComponentCollection implements ComponentCollection {
        private final String identifier;

        public DummyComponentCollection(final String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public long getLastModified() {
            return 0L;
        }

        @Override
        public void init() {}

        @Override
        public Map<String, Boolean> getEnabledMap() {
            return null;
        }

        @Override
        public Map<String, String> getConfigurationMap() {
            return null;
        }

        @Override
        public String getComponentConfiguration(String identifier) {
            return null;
        }

        @Override
        public Boolean getArchiveEnabled(String identifier) {
            return null;
        }

        @Override
        public Boolean getComponentEnabled(String identifier) {
            return null;
        }

        @Override
        public List<ComponentSource> getSources() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "DummyComponentCollection[\"" + identifier + "\"]";
        }
    }

    String getIdentifier();

    long getLastModified();

    void init();

    Map<String,Boolean> getEnabledMap();

    Map<String, String> getConfigurationMap();

    String getComponentConfiguration(String identifier);

    Boolean getArchiveEnabled(String identifier);

    Boolean getComponentEnabled(String identifier);

    List<ComponentSource> getSources();
}
