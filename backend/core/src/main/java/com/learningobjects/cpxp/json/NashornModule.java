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

package com.learningobjects.cpxp.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * This module supports serializing Java 8 Nashorn arrays as .. arrays.
 */
public class NashornModule extends SimpleModule {
    private static final Logger logger = Logger.getLogger(NashornModule.class.getName());

    public NashornModule() {
        super("Nashorn");
        addSerializer(new ScriptObjectMirrorSerializer());
    }

    // This class uses Object as its type parameter in order to prevent
    // static linkage to Java 8 which would break Java 7 builds. Special.
    static class ScriptObjectMirrorSerializer extends JsonSerializer<ScriptObjectMirror> {
        @Override
        @SuppressWarnings("unchecked")
        public Class<ScriptObjectMirror> handledType() {
            return ScriptObjectMirror.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void serialize(ScriptObjectMirror input, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (input.isArray()) {
                gen.writeObject(input.values());
            } else {
                gen.writeObject(new HashMap<>(input));
            }
        }
    }
}
