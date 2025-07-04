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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.mrbean.AbstractTypeMaterializer;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.controller.upload.Uploads;
import com.learningobjects.cpxp.jackson.PekkoModule;
import com.learningobjects.cpxp.jackson.ValidatingObjectMapper;
import com.learningobjects.cpxp.json.NashornModule;
import com.learningobjects.cpxp.scala.json.DeScalaModule;
import com.learningobjects.cpxp.scala.json.OptionalFieldModule;
import com.learningobjects.de.web.jackson.ComponentInterfaceModule;

public class JacksonUtils {

    private static  ObjectMapper MAPPER;
    private static  ObjectMapper VALIDATING_MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        MAPPER.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
//        MAPPER.enable(DeserializationFeature.USE_LONG_FOR_INTS);
//        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        /*configure date format to be ISO 8601 with seconds and Z for GMT) */
        MAPPER.setDateFormat(new ExtendedISO8601DateFormat());

        MAPPER.registerModule(new Jdk8Module());
        MAPPER.registerModule(new DeScalaModule());
        MAPPER.registerModule(new ComponentInterfaceModule());
        MAPPER.registerModule(new GuavaModule());
        MAPPER.registerModule(new PekkoModule());
        MAPPER.registerModule(new NashornModule());
        MAPPER.registerModule(new Uploads.UploadInfoModule());
        MAPPER.registerModule(new OptionalFieldModule());
        MAPPER.registerModule(new JavaTimeModule());

        VALIDATING_MAPPER = new ValidatingObjectMapper();
        VALIDATING_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        VALIDATING_MAPPER.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        VALIDATING_MAPPER.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        VALIDATING_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        VALIDATING_MAPPER.setDateFormat(new ExtendedISO8601DateFormat());
        VALIDATING_MAPPER.registerModule(new DeScalaModule());
        VALIDATING_MAPPER.registerModule(new JavaTimeModule());
        VALIDATING_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
    }

    /**
     * Jackson quirks
     * <ul>
     *     <li>deserializes null/missing properties to null/None, regardless of
     *         existence of default value
     *      </li>
     *      <li>blows up there is an unrecognizable property in the JSON when
     *          deserializing
     *      </li>
     *      <li>is annoying with serializing doubles, dropping trailing 0s and
     *          making equality comparison difficult
     *      </li>
     * </ul>
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Formerly finatra with quirks. No more, but keeping the getter separate in case
     * something special is needed.
     *
     * <ul>
     *     <li>Does not ignore Scala defaults when deserializing
     *         <ul>
     *             <li>If prop is missing and there is a default, Finatra adds
     *                 default value
     *             </li>
     *             <li>If prop is missing or null and Scala type + default is
     *                 Option[Foo] = Some(Bar), Finatra adds Some(Bar). In this
     *                 way the JSON can never deserialize to None; it will
     *                 always be Some(_)
     *             </li>
     *             <li>If prop is missing, is not an Option, and there is no default, Finatra
     *                 throws an exception. (Option types are literally optional, so then Finatra
     *                 defaults Options to `None` unless otherwise specified. ^
     *             </li>
     *         </ul>
     *     </li>
     *     <li>Excludes props entirely with null/None values when serializing</li>
     *     <li>Has a lot of helpful annotation validators that throw exceptions
     *         when violated
     *     </li>
     *     <li>Quietly ignores unrecognizable properties in JSON when
     *         deserializing
     *     </li>
     *
     * </ul>
     */
    public static ObjectMapper getFinatraMapper() {
        return VALIDATING_MAPPER;
    }

    /**
     * Return a new {@link ObjectMapper} that can materialize interface implementations
     * using the specified {ClassLoader}. Typically you should not use this directly,
     * but should use the {@link ComponentSupport#getObjectMapper()}.
     *
     * @return a new {@link ObjectMapper} that can materialize interfaces
     */
    public static ObjectMapper newEnvironmentMapper(ClassLoader classLoader) {
        return getMapper().copy().registerModule
            (new MrBeanModule(new AbstractTypeMaterializer(classLoader)));
    }

    public static ObjectNode objectNode() {
        return getMapper().createObjectNode();
    }

    public static ArrayNode arrayNode() {
        return getMapper().createArrayNode();
    }

}
