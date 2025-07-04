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

package com.learningobjects.cpxp.component.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Function;
import com.google.common.base.Functions;

import javax.annotation.Nullable;

public class JsonNodeFunctions {

    /**
     * A {@link Function} for {@link JsonNode#asText()}
     */
    public static final Function<JsonNode, String> AS_TEXT = new Function<JsonNode, String>() {

        @Nullable
        @Override
        public String apply(@Nullable final JsonNode node) {
            return node == null ? null : node.asText();
        }
    };

    /**
     * A {@link Function} for {@link JsonNode#asLong()}
     */
    public static final Function<JsonNode, Long> AS_LONG = new Function<JsonNode, Long>() {


        @Nullable
        @Override
        public Long apply(@Nullable final JsonNode node) {
            return node == null ? null : node.asLong();
        }
    };

    /**
     * A {@link Function} for {@link JsonNode#deepCopy()}
     */
    public static final Function<JsonNode, JsonNode> DEEP_COPY = new Function<JsonNode, JsonNode>() {

        @Nullable
        @Override
        public JsonNode apply(@Nullable final JsonNode node) {
            return node == null ? null : node.deepCopy();
        }
    };

    /**
     * A {@link Function} for {@link TextNode#valueOf(String)}}.
     */
    public static final Function<String, JsonNode> TO_TEXT_NODE = new Function<String, JsonNode>() {

        @Nullable
        @Override
        public JsonNode apply(@Nullable final String string) {
            return TextNode.valueOf(string);
        }
    };

    /**
     * A {@link Function} for {@link JsonNode#get(String)}
     */
    public static Function<JsonNode, JsonNode> get(final String fieldName) {
        return new Function<JsonNode, JsonNode>() {

            @Nullable
            @Override
            public JsonNode apply(@Nullable final JsonNode node) {
                return node == null ? null : node.get(fieldName);
            }
        };
    }

    /**
     * A {@link Function} for {@link JsonNode#at(String)}
     */
    public static Function<JsonNode, JsonNode> at(final String jsonPointer) {
        return new Function<JsonNode, JsonNode>() {
            @Override
            public JsonNode apply(final JsonNode jsonNode) {
                return jsonNode.at(jsonPointer);
            }
        };
    }

    /**
     * A {@link Function} for {@link JsonNode#get(String)} and {@link JsonNode#asText()}
     */
    public static Function<JsonNode, String> getAsText(final String fieldName) {
        return Functions.compose(AS_TEXT, get(fieldName));
    }

    /**
     * A {@link Function} for {@link JsonNode#get(String)} and {@link JsonNode#asLong()}
     */
    public static Function<JsonNode, Long> getAsLong(final String fieldName) {
        return Functions.compose(AS_LONG, get(fieldName));
    }

    public static Function<JsonNode, String> atAsText(final String jsonPointer) {
        return Functions.compose(AS_TEXT, at(jsonPointer));
    }

    public static Function<JsonNode, JsonNode> path(final String path) {
        return new Function<JsonNode, JsonNode>() {
            @Override
            public JsonNode apply(final JsonNode jsonNode) {
                return jsonNode.path(path);
            }
        };
    }
}
