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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.learningobjects.cpxp.component.ComponentInterface;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An entity in the Difference Engine media type that has a component, metadata about the component,
 * and optional embedded entities.
 */
public class ComponentEntity implements DeEntity, WithComponents {

    /**
     * State of the resource (business properties)
     */
    private final ComponentInterface state;

    /**
     * Embedded entities
     */
    private final Map<String, DeEntity> embeds;

    public ComponentEntity(final ComponentInterface state) {
        this.state = state;
        this.embeds = new HashMap<>();
    }

    @JsonUnwrapped
    @JsonProperty
    public ComponentInterface getState() {
        return state;
    }

    @JsonAnyGetter
    public Map<String, DeEntity> getEmbeds() {
        return embeds;
    }

    public void putEmbed(final String embedName, final DeEntity embed) {
        this.embeds.put(embedName, embed);
    }

    @Override
    public List<ComponentEntity> getComponents() {
        return Collections.singletonList(this);
    }
}
