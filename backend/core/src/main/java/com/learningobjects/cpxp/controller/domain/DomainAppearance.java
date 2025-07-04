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

package com.learningobjects.cpxp.controller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.annotation.Service;

import java.util.Map;

@Service
public interface DomainAppearance {

    /**
     * This will configure the current domain with the color-primary, color-secondary, color-accent values
     * passed to it, whilst not disrupting any other configurations.
     * @param primaryColor the value to be used as the <strong>primary</string> color.
     * @param secondaryColor the value to be used as the <strong>secondary</string> color.
     * @param accentColor the value to be used as the <strong>accent</string> color.
     */
    public void setColors(String primaryColor, String secondaryColor, String accentColor);

    /**
     * Gets a map of all the style variables configured for the domain.
     * @return A map where the keys are the variables names and the values are the variable values.
     */
    public Map<String, String> getStyleConfigurations();

    /**
     * Gets the </strong>primary</strong> color configured for this domain.
     * @return the color value which is the primary color of the domain.
     */
    @JsonProperty
    public String getPrimaryColor();

    /**
     * Gets the <strong>secondary</strong> color configured for this domain.
     * @return the color value which is the secondary color of the domain.
     */
    @JsonProperty
    public String getSecondaryColor();

    /**
     * Gets the <strong>accent</strong> color configured for this domain.
     * @return the color value which is the accent color of the domain.
     */
    @JsonProperty
    public String getAccentColor();

    /**
     * Returns a unique hash of the style variables.
     */
    public String getStyleHash();

}
