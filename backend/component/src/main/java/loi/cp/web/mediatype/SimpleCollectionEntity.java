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

package loi.cp.web.mediatype;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An entity in the Difference Engine media type that has a collection of entities (without metadata).
 */
public class SimpleCollectionEntity implements DeEntity, WithComponents {

    private final List<DeEntity> entities;

    public SimpleCollectionEntity(final List<DeEntity> components) {
        this.entities = components;
    }

    @JsonValue
    public List<DeEntity> getEntities() {
        return entities;
    }

    @Override
    public List<ComponentEntity> getComponents() {
        return entities.stream().filter(ComponentEntity.class::isInstance).map(ComponentEntity.class::cast).collect(Collectors.toList());
    }

}
