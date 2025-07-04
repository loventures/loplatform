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

package com.learningobjects.cpxp.service.exception;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception that is mapped to an http status code and return in Rest.
 */
public interface RestExceptionInterface {


    /**
     * The type of error return to the caller. Using Underscores instead of spaces please. If this is null, no body will be returned.
     *
     * @return
     */
    RestErrorType getErrorType();

    /**
     * The Http status code to return
     *
     * @return an http status code
     */
    int getHttpStatusCode();

    /**
     * Get the message formatted as a json map. If this is null, no body will be returned.
     *
     * @return
     */
    JsonNode getJson();



}
