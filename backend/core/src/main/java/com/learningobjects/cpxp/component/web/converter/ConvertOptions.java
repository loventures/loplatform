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

package com.learningobjects.cpxp.component.web.converter;

import com.learningobjects.cpxp.component.web.CacheOptions;
import com.learningobjects.de.web.MediaType;

import java.util.Optional;

/**
 * Options for message conversion.
 */
public class ConvertOptions {
    private final MediaType mediaType;
    private final Optional<CacheOptions> cacheOptions;

    /**
     * Create new message conversion options.
     * @param mediaType the requested media type
     * @param cacheOptions cache options
     */
    public ConvertOptions(final MediaType mediaType, final Optional<CacheOptions> cacheOptions) {
        this.mediaType = mediaType;
        this.cacheOptions = cacheOptions;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Optional<CacheOptions> getCacheOptions() {
        return cacheOptions;
    }
}
