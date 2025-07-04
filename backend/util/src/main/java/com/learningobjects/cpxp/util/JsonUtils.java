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

package com.learningobjects.cpxp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON utility methods.
 */
public class JsonUtils {
    private static final ObjectMapper __mapper = new ObjectMapper();

    public static String toJson(Object object) throws JsonMappingException {
        try {
            StringWriter sw = new StringWriter();
            __mapper.writeValue(sw, object);
            return sw.toString();
        } catch (Exception ex) {
            throw new JsonMappingException("Error JSON-mapping object: " + object, ex);
        }
    }

    public static Map<String, String> toMap(String objectStr) throws JsonMappingException {
        try {
            TypeReference<HashMap<String, String>> type = new TypeReference<HashMap<String,String>>(){};
            return __mapper.readValue(objectStr, type);
        } catch (Exception ex) {
            throw new JsonMappingException("Error JSON-unmapping object: " + objectStr, ex);
        }
    }
}
