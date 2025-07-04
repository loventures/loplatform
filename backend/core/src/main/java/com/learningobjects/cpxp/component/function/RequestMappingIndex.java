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

package com.learningobjects.cpxp.component.function;

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.registry.SchemaRegistry;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.exception.HttpRequestMethodNotSupportedException;
import com.learningobjects.cpxp.component.web.exception.UnsupportedRequestBodyType;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.ReflectionUtils;
import com.learningobjects.de.web.util.ModelUtils;
import com.learningobjects.de.web.util.UriTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * An index of request mappings. This class is often used by registries to store and
 * search for {@link RequestMapping} methods. Each registry will determine the scope of
 * methods stored in one of these indices.
 */
public class RequestMappingIndex {

    private static final String NO_REQUEST_BODY_TYPE_NAME = "{none}";

    private final RequestMappingSignatureValidator validator;

    /**
     * The index
     */
    private Map<UriTemplate, Map<Method, Map<String, FunctionDescriptor>>>
            functionRegistry = new HashMap<>();

    public RequestMappingIndex() {
        this.validator = RequestMappingSignatureValidator.INSTANCE;
    }

    public RequestMappingIndex(final RequestMappingSignatureValidator validator) {
        this.validator = validator;
    }

    public void put(
            final RequestMapping requestMapping,
            final FunctionDescriptor functionDescriptor) {

        validator.validateDeclaringClassRequestMapping(functionDescriptor);

        final Class<?> declaringClass =
                functionDescriptor.getDelegate().getDelegateClass();
        final RequestMapping declaringClassRequestMapping =
                ClassUtils.findAnnotation(declaringClass, RequestMapping.class).orElse(null);

        final String uriTemplatePath;
        if (declaringClassRequestMapping == null) {
            uriTemplatePath = requestMapping.path();
        } else {
            if (RequestMapping.EMPTY_PATH.equals(requestMapping.path())) {
                uriTemplatePath = declaringClassRequestMapping.path();
            } else {
                uriTemplatePath = declaringClassRequestMapping.path() + "/" + requestMapping.path();
            }
        }

        final UriTemplate uriTemplate = new UriTemplate(uriTemplatePath);
        final Method m = requestMapping.method();
        final Method httpMethod = m == Method.Any ? Method.GET : m; // TODO: deprecate this use of Any

        put(uriTemplate, httpMethod, functionDescriptor);
    }

    private void put(
            final UriTemplate uriTemplate, final Method httpMethod,
            final FunctionDescriptor functionDescriptor) {

        validator.validate(functionDescriptor);

        final String typeName = getRequestBodyTypeName(functionDescriptor);

        Map<Method, Map<String, FunctionDescriptor>> uriTemplateValue =
                functionRegistry.get(uriTemplate);
        if (uriTemplateValue == null) {
            uriTemplateValue = new HashMap<>();
            functionRegistry.put(uriTemplate, uriTemplateValue);
        }

        Map<String, FunctionDescriptor> httpMethodValue =
                uriTemplateValue.get(httpMethod);
        if (httpMethodValue == null) {
            httpMethodValue = new HashMap<>();
            uriTemplateValue.put(httpMethod, httpMethodValue);
        }

        FunctionDescriptor typeValue = httpMethodValue.get(typeName);
        if (typeValue == null) {
            httpMethodValue.put(typeName, functionDescriptor);
        } else {
            /*
             * then `functionDescriptor`'s method probably duplicates `typeValue`'s method
             */
            if (!typeValue.getMethod().equals(functionDescriptor.getMethod())) {
                /*
                 * stop loading the component environment; if we continue loading, then `functionDescriptor`'s method
                 * will not be registered in the component environment.
                 */
                throw new AmbiguousRequestMappingException(typeValue, functionDescriptor, uriTemplate, httpMethod,
                        typeName);
            }
            /*
             * else, ignore `functionDescriptor`'s method because the method has already been registered. We only got
             * here because the same class has been introspected more than once. This is common, for example, in
             * `class A extends B implements C {}` and `class B implements C {}`, the methods on C will be introspected
             * twice upon introspection of A.
             */
        }


    }

    private String getRequestBodyTypeName(final FunctionDescriptor functionDescriptor) {
        final java.lang.reflect.Method method = functionDescriptor.getMethod();
        final Type requestBodyType = ReflectionUtils
                .findGenericParameterWithAnnotation(method, RequestBody.class);
        final String typeName;
        if (requestBodyType == null) {
            typeName = NO_REQUEST_BODY_TYPE_NAME;
        } else {
            final Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(
                    functionDescriptor.getDelegate().getDelegateClass(), method.getDeclaringClass());
            final String schemaName = ModelUtils.getSchemaName(requestBodyType, typeArguments);
            typeName = schemaName == null ? NO_REQUEST_BODY_TYPE_NAME : schemaName;
        }
        return typeName;
    }

    /**
     * Search the {@link RequestMapping} methods of this component for the best {@link RequestMapping} method.
     *
     * @param path       the path, must be nonnull
     * @param httpMethod the HTTP method, must be nonnull
     * @param typeName   schema name of the request body, may be null
     * @return the {@link RequestMapping} method for the path, HTTP method, and request body type.
     * @throws ResourceNotFoundException              for unmapped paths
     * @throws HttpRequestMethodNotSupportedException for unsupported HTTP methods
     * @throws UnsupportedRequestBodyType             for unsupported request body types
     */
    @Nonnull
    public FunctionDescriptor get(@Nonnull final String path, @Nonnull final Method httpMethod,
            @Nullable final String typeName) {

        final TreeSet<UriTemplate> matchingUriTemplates = new TreeSet<>(UriTemplate.GREEDY_ORDERING);
        for (final UriTemplate uriTemplate : functionRegistry.keySet()) {
            if (uriTemplate.matches(path)) {
                matchingUriTemplates.add(uriTemplate);
            }
        }

        final FunctionDescriptor result;
        if (matchingUriTemplates.isEmpty()) {
            // resource not found
            throw new ResourceNotFoundException("No such resource: '" + path + "'");
        } else {

            final UriTemplate bestPath = matchingUriTemplates.first();
            final UriTemplate searchPath = new UriTemplate(path);
            final Map<Method, Map<String, FunctionDescriptor>> methodRegistry = functionRegistry.get(bestPath);



            final boolean lastLink = searchPath.getNumSegments() == bestPath.getNumSegments();

            final Method effectiveMethod;
            if (!lastLink || Method.PATCH == httpMethod) {
                effectiveMethod = Method.GET;
            } else {
                effectiveMethod = httpMethod;
            }

            final Map<String, FunctionDescriptor> exactMap = methodRegistry.get(effectiveMethod);

            final Map<String, FunctionDescriptor> typeMap;
            if ((exactMap == null) && (effectiveMethod != Method.GET)) {
                typeMap = methodRegistry.get(Method.GET);
            } else if (exactMap == null && httpMethod == Method.PATCH) {
                typeMap = methodRegistry.get(Method.PATCH);
            } else {
                typeMap = exactMap;
            }

            if (typeMap == null) {
                // method not supported
                throw new HttpRequestMethodNotSupportedException(httpMethod);
            } else {

                if ((Method.GET == effectiveMethod) || StringUtils.isEmpty(typeName) || (exactMap == null)) {

                    if (typeMap.size() == 1) {
                        // very, very common case
                        result = typeMap.values().iterator().next();
                    } else {

                        // then pick the @RequestMapping with the most generic @RequestBody parameter

                        FunctionDescriptor mostGenericDescriptor = null;
                        Class<?> mostGeneric = null;
                        for (final FunctionDescriptor functionDescriptor : typeMap.values()) {
                            final Class<?> clazz = ReflectionUtils
                                    .findParameterWithAnnotation(
                                            functionDescriptor.getMethod(),
                                            RequestBody.class);

                            if (mostGeneric == null || (clazz != null && clazz.isAssignableFrom(mostGeneric))) {
                                // clazz can be null if the @RequestMapping doesn't have a @RequestBody parameter
                                mostGenericDescriptor = functionDescriptor;
                                mostGeneric = clazz;
                            }
                        }
                        result = mostGenericDescriptor;

                        if (result == null) {
                            // I don't think this can happen, but I don't want a silent failure
                            throw new UnsupportedRequestBodyType(typeName, typeMap.keySet());
                        }

                    }
                } else {
                    // then pick the @RequestMapping with the @RequestBody parameter closest to the client's
                    // requested type
                    final FunctionDescriptor supportedFunction = typeMap.get(typeName);

                    if (supportedFunction == null) {
                        // then the request body type of the request isn't explicitly supported by this template's
                        // path and
                        // HTTP method, but it may be implicitly supported. A request body type is implicitly
                        // supported if it

                        // can substitute for a supertype. Only if no supertypes are
                        // supported is the request bad.
                        final SchemaRegistry.Registration registration = ComponentSupport
                                .lookupResource(Schema.class,
                                        SchemaRegistry.Registration.class, typeName);
                        if (registration == null) {
                            // the type specified in the request is not known by the server
                            throw new UnsupportedRequestBodyType(typeName, typeMap.keySet());
                        } else {
                            // do a breadth-first search of the interface type tree of requestBodyType until we find a
                            // function
                            final FunctionDescriptor function = findClosestFunction(typeMap, registration.getSchemaClass());
                            if (function == null) {
                                // the type is known, but not supported by this path and HTTP method
                                throw new UnsupportedRequestBodyType(typeName, typeMap.keySet());
                            } else {
                                result = function;
                            }
                        }
                    } else {
                        result = supportedFunction;
                    }
                }
            }
        }

        return result;


    }

    public Collection<FunctionDescriptor> values() {

        final Collection<FunctionDescriptor> functions = new ArrayList<>();
        for (final Map<Method, Map<String, FunctionDescriptor>> methods : functionRegistry.values()) {
            for (final Map<String, FunctionDescriptor> types : methods.values()) {
                functions.addAll(types.values());
            }
        }
        return functions;

    }

    private FunctionDescriptor findClosestFunction(final Map<String, FunctionDescriptor> supportedRequestBodyTypes, final Class<?> type) {
        for (Class<?> iface : (List<Class<?>>) ClassUtils.getAllInterfaces(type)) {
            final Optional<Schema> schema = ClassUtils.findAnnotation(iface, Schema.class);
            //TODO: Use functional combinators with Optional instead, filter/map instead of isPresent/value
            if (schema.isPresent()) {
                final FunctionDescriptor function = supportedRequestBodyTypes.get(schema.get().value());
                if (function != null) {
                    return function;
                }
            }
        }

        return null;
    }
}
