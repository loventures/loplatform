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

package com.learningobjects.de.web.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.de.web.mediatype.DeMediaTypeProperty;

/**
 * The extra properties that are added to serialized {@link ComponentInterface}s.
 */
public class ExtraPropertiesMixIn {

    private final String type;

    public ExtraPropertiesMixIn(final String type) {
        this.type = type;
    }

    @JsonProperty(DeMediaTypeProperty.TYPE)
    public String getType() {
        return type;
    }
}
