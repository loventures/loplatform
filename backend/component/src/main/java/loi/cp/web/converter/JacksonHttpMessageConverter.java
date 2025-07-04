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

package loi.cp.web.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.registry.SchemaRegistry;
import com.learningobjects.cpxp.component.web.ArgoBody;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.converter.ConvertOptions;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverter;
import com.learningobjects.cpxp.component.web.exception.HttpMessageNotReadableException;
import com.learningobjects.cpxp.component.web.exception.HttpMessageNotWriteableException;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.AugurySupport;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.validation.DefaultHttpRequestGroupAssignmentStrategy;
import com.learningobjects.de.web.MediaType;
import de.mon.ThreadStatistics;
import loi.cp.datatype.SchemaService;
import loi.cp.web.cache.WebCache;
import loi.cp.web.cache.WebEntry;
import scala.Option;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Converts application/json HTTP entities to and from our own types using Jackson.
 */
@Component
public class JacksonHttpMessageConverter extends AbstractComponent implements HttpMessageConverter<Object> {
    private static final Logger logger = Logger.getLogger(JacksonHttpMessageConverter.class.getName());

    private static final List<MediaType> SUPPORTED_MEDIA_TYPES = List.of(
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_IMS_LINE_ITEM, MediaType.APPLICATION_IMS_RESULT);

    @Inject
    private SchemaService schemaService;

    @Inject
    private JacksonHttpMessageIO io;

    @Inject
    private ObjectMapper mapper;

    @Infer
    private WebCache cache;

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    private boolean supports(final MediaType mediaType) {
        return SUPPORTED_MEDIA_TYPES.stream().anyMatch(MediaType.isCompatibleWithPredicate(mediaType));
    }

    @Override
    public boolean canRead(final Type type, final MediaType mediaType) {
        return supports(mediaType) && !isArgoType(type);
    }

    private boolean isArgoType(final Type type) {
        return (type instanceof ParameterizedType) && ArgoBody.class.equals(((ParameterizedType) type).getRawType());
    }

    @Override
    public boolean canWrite(final Object value, final MediaType mediaType) {
        return supports(mediaType) && !(value instanceof ArgoBody);
    }

    @Override
    public Object read(final RequestBody requestBody, final WebRequest request, final Type targetType) {
        final JavaType effectiveTargetType;

        final SchemaRegistry.Registration schemaRegistration;
        if (request.getRequestBodySchemaName() != null) {
            schemaRegistration = schemaService.findSchema(request.getMethod(), request.getRequestBodySchemaName());
        } else {
            schemaRegistration = schemaService.findSchema(request.getMethod(), targetType);
        }
        if (schemaRegistration == null) {
            effectiveTargetType = JacksonUtils.getMapper().getTypeFactory().constructType(targetType);
        } else {
            effectiveTargetType = JacksonUtils.getMapper().getTypeFactory().constructType(schemaRegistration.getSchemaClass());
        }

        return readInternal(request, effectiveTargetType, requestBody.log());
    }

    private static final int MAX_LOGGABLE_JSON_SIZE = 65536; // 64kB ok?

    private Object readInternal(final WebRequest request, final JavaType targetType,final boolean doLog) {

        try {

            final Object result;
            final Class<?> view = DefaultHttpRequestGroupAssignmentStrategy.getGroup(request.getMethod().name());
            final int length = request.getRawRequest().getContentLength();
            final InputStream in = request.getRawRequest().getInputStream();
            if (doLog && (length < MAX_LOGGABLE_JSON_SIZE)) {
                Supplier<InputStream> contents = io.read(in, Math.min(256, length));
                JsonLogging.withLogMetaJson(contents.get(), () -> logger.info("Parsing JSON for: " + request.getPath()));
                result = io.parse(Option.apply(view), targetType, contents.get());
            } else {
                if (doLog) {
                    logger.info("Not logging JSON for submission of size " + length + " bytes");
                }
                result = io.parse(Option.apply(view), targetType, in);
            }

            return result;

        } catch (final IOException ex) {
            throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void write(final Object source, final ConvertOptions options, final HttpServletRequest request, final HttpServletResponse response) {

        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        if (source != null) {

            boolean prod = BaseServiceMeta.getServiceMeta().isProdLike();

            try {
                // TODO: Ideally this would cache the gzip-compressed response and return the response using sendfile or
                // other async I/O.
                final byte[] rspData;
                if (options.getCacheOptions().isPresent()) {
                    // TODO: Arguably I should also support rm.mode == READ_ONLY if i could make the post body a part of the cache key
                    if (!"GET".equals(request.getMethod())) {
                        throw new RuntimeException("Invalid CacheOptions on non-GET request");
                    }
                    String uri = Current.getDomain() + ":" + request.getRequestURI();
                    Option<byte[]> cached = cache.get(uri);
                    rspData = cached.getOrElse(() -> {
                        byte[] serialized = io.serialize(source);
                        cache.put(new WebEntry(uri, serialized, options.getCacheOptions().get()));
                        return serialized;
                    });
                    if (!prod) {
                        response.addHeader("X-WebCache", (cached.isEmpty()) ? "Miss" : "Hit");
                    }
                } else {
                    rspData = io.serialize(source);
                }

                if (!prod) {
                    ThreadStatistics.statistics().foreach(stats -> {
                        response.addHeader("X-QueryCount", String.valueOf(stats.count()));
                        return null;
                      });
                }

                // uncomment if you're a fun-sucker
                // if (!prod) {
                    AugurySupport.augurize(response);
                //}

                // This does not belong here, but... Release database connection before writing data.
                ManagedUtils.end();

                /*
                 * Now that there are no Jackson exceptions, commit to the HttpServletResponse.
                 * This could be avoided if we had a "delayed" HttpServletResponseWrapper, see US7250
                 */
                io.write(rspData, request, response);

            } catch (final Exception ex) {
                throw new HttpMessageNotWriteableException("Could not write JSON: " + ex.getMessage(), ex);
            }
        }

    }
}
