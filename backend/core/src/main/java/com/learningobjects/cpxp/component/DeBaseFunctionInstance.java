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

package com.learningobjects.cpxp.component;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.util.ReflectionSupport;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.component.web.converter.StringConverter.Raw;
import com.learningobjects.cpxp.component.web.exception.TypeMismatchException;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.Out;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;
import com.learningobjects.cpxp.util.lang.EnumLike;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.cpxp.util.lang.TypeTokens;
import com.learningobjects.cpxp.util.net.URLUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import scala.Function0;
import scala.Option;
import scala.collection.Seq;
import scala.compat.java8.OptionConverters;
import scala.jdk.javaapi.CollectionConverters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.learningobjects.cpxp.component.web.MatrixParameterName.LIMIT;
import static com.learningobjects.cpxp.component.web.MatrixParameterName.OFFSET;

abstract class DeBaseFunctionInstance extends AbstractFunctionInstance {

    /**
     * Walks the declared arguments of this instance's methods, finding values
     * for them.
     *
     * @param request
     *            the source of any argument values.
     * @return the arguments to invoke this instance's method with.
     */
    protected Object[] resolveHandlerArguments(
            final WebRequest request, final HttpServletResponse response,
            final List<DePathSegment> pathSegments, final boolean checkPaginationSupport) {
        /*
         * pattern segments of the template of the URI path space for this
         * invocation
         */
        final String uriTemplate = uriTemplate();

        final List<UriPathSegment> uriPathSegments =
          pathSegments.stream()
            .map(DePathSegment::getUriPathSegment)
            .collect(Collectors.toList());

        final Map<String, String> uriTemplateVariables = RequestMappingUtil.getInstance().applyPathToUriTemplate(
                uriTemplate, uriPathSegments);
        final Optional<DePathSegment> lastSegment =
          pathSegments.stream()
            .reduce((a, b) -> b); // stream magic kindly provided by IJ

        final Method method = _function.getMethod();
        final java.lang.reflect.Parameter[] params = method.getParameters();
        final Object[] args = new Object[params.length];
        RequestMapping rm = method.getAnnotation(RequestMapping.class);
        boolean supportsPagination = (rm != null) && com.learningobjects.cpxp.component.web.Method.Any.equals(rm.method()); // root component chaining
        final Set<String> consumedQueryParamNames = new HashSet<>();

        // TODO: the descriptor ought to have cached this information to avoid the
        // slow introspection.
        // TODO: make this just use the Evaluator framework so it just works
        for (int i = 0; i < args.length; i++) {
            java.lang.reflect.Parameter param = params[i];
            Annotation[] annotations = param.getAnnotations();

            /* --- Discover Annotations --- */
            String pathVarName = null;
            boolean requestBodyFound = false;
            int annotationsFound = 0;
            QueryParam queryParam = null;
            MatrixParam matrixParam = null;
            RequestBody requestBody = null;
            HttpHeader httpHeader = null;

            for (final Annotation anno : annotations) {

                if (anno instanceof PathVariable) {
                    final PathVariable pathVar = (PathVariable) anno;
                    pathVarName = pathVar.value();
                    annotationsFound++;
                } else if (anno instanceof QueryParam) {
                    queryParam = (QueryParam) anno;
                    annotationsFound++;
                } else if (anno instanceof MatrixParam) {
                    matrixParam = (MatrixParam) anno;
                    annotationsFound++;
                } else if (anno instanceof RequestBody) {
                    requestBody = (RequestBody) anno; // multipart will require more effort
                    requestBodyFound = true;
                    annotationsFound++;
                } else if (anno instanceof HttpHeader) {
                    httpHeader = (HttpHeader) anno;
                    annotationsFound++;
                }
            }

            /* --- Resolve Argument --- */
            final Object arg;

            if (annotationsFound == 0) {

                // TODO: refactor to pluggable handlers rather than static
                // if/else statements (to support more exact RequestMapping vs
                // Counter handling)

                /* must be one of the allowed request mapping argument types */

                final Class<?> paramType = param.getType();
                final Type targetType = param.getParameterizedType();
                if (ApiPage.class.isAssignableFrom(paramType) && lastSegment.isPresent()) {

                    final ApiPage page = resolvePage(lastSegment.get(), annotations);
                    request.setPage(page);
                    supportsPagination = true;

                    arg = page;
                } else if (ApiQuery.class.isAssignableFrom(paramType) && lastSegment.isPresent()) {
                    // TODO: I'm confused why I only put this on the webrequest if
                    // it is an explicit parameter but...
                    final ApiQuery query = resolveQuery(lastSegment.get(), request.isEmbed(), annotations, TypeToken.of(method.getGenericReturnType()));
                    request.setQuery(query);
                    supportsPagination = true; // TODO: what if it just doesn't support pagination?

                    arg = query;
                } else if (HttpServletRequest.class.equals(paramType)) {
                    arg = request.getRawRequest();
                } else if (WebRequest.class.equals(paramType)) {
                    arg = request;
                } else if (HttpServletResponse.class.equals(paramType)) {
                    arg = response;
                } else if (HttpSession.class.equals(paramType)) {
                    arg = request.getRawRequest().getSession(true);
                } else if (HttpContext.class.equals(paramType)) {
                    arg = HttpContext.apply(request, response);
                } else if (AsyncContext.class.equals(paramType)) {
                    arg = HttpContext.apply(request, response);
                } else if (ServiceMeta.class.equals(paramType)) {
                    arg = BaseServiceMeta.getServiceMeta();
                } else if (DomainDTO.class.equals(paramType)) { // ultimately prefer DI of "Current" but not yet
                    arg = Current.getDomainDTO();
                } else if (Out.class.equals(paramType)) {
                    final Class<?> subType = (Class<?>) ((ParameterizedType) targetType).getActualTypeArguments()[0];
                    arg = request.getOutParameter(subType);
                } else {

                    /* TODO HTTP 500 with error body */
                    throw new RuntimeException("Unsupported request mapping method argument '" + paramType.getName()
                            + "' for method '" + method.getName() + "'");
                }
            } else {
                final Type targetType = param.getParameterizedType();

                if (pathVarName != null) {
                    final String pathVarRawValue = uriTemplateVariables.get(pathVarName);
                    /* TODO support all types */

                    final TypeToken<?> paramType = TypeToken.of(targetType).wrap();

                    arg = convert(pathVarRawValue, pathVarName, paramType, true);
                } else if ((queryParam != null) || (matrixParam != null) || (httpHeader != null)) {
                    final TypeToken<?> paramType = TypeToken.of(targetType).wrap();
                    final boolean listy = paramType.isArray() || List.class.isAssignableFrom(paramType.getRawType()) || Seq.class.isAssignableFrom(paramType.getRawType());

                    final String paramName;
                    final boolean isParamRequired;
                    final String[] paramValues;
                    final Class<?> erasedType;
                    if (queryParam != null) {
                        paramName = StringUtils.defaultIfBlank(queryParam.value(), param.getName());
                        isParamRequired = queryParam.required();
                        paramValues = request.getRawRequest().getParameterValues(paramName);
                        consumedQueryParamNames.add(paramName);
                        erasedType = queryParam.decodeAs();
                    } else if (matrixParam != null) {
                        paramName = StringUtils.defaultIfBlank(matrixParam.value(), param.getName());
                        isParamRequired = matrixParam.required();
                        final String value = lastSegment.get().getUriPathSegment().getMatrixParameters().get(paramName);
                        final String decodedValue = URLUtils.decode(value);

                        if (listy) {
                            paramValues = (decodedValue == null) ? null : decodedValue.split(",");
                        } else {
                            paramValues = (decodedValue == null) ? null : new String[]{ decodedValue }; // TODO: can matrix params be repeated?
                        }
                        erasedType = Object.class;
                    } else {
                        paramName = httpHeader.value();
                        isParamRequired = httpHeader.required();
                        paramValues = Collections.list(request.getRawRequest().getHeaders(paramName))
                            .toArray(new String[0]);
                        erasedType = Object.class;
                    }

                    // ensure required param is there
                    if(isParamRequired && (paramValues == null) && !OptionLike.isOptionLike(paramType.getRawType()) && !listy) {
                        throw new TypeMismatchException("[empty]", paramName, paramType.getRawType());
                    }

                    if (paramType.isArray()) {
                        TypeToken<?> componentType = paramType.getComponentType();
                        if (paramValues == null) {
                            arg = Array.newInstance(componentType.getRawType(), 0);
                        } else {
                            arg = Array.newInstance(componentType.getRawType(), paramValues.length);
                            for (int j = 0; j < paramValues.length; ++j) {
                                Array.set(arg, j, convert(paramValues[j], paramName, componentType, true));
                            }
                        }
                    } else if (List.class.isAssignableFrom(paramType.getRawType())) {
                        final TypeToken<?> componentType =
                                TypeTokens.resolveIterableElementType(paramType, TypeToken.of(_function.getDelegate().getDelegateClass())).get();

                        arg = buildRawList(paramValues, componentType, pathVarName);
                    } else if (Seq.class.isAssignableFrom(paramType.getRawType())) {
                        final TypeToken<?> componentType =
                          unerase(TypeTokens.resolveSeqElementType(paramType, TypeToken.of(_function.getDelegate().getDelegateClass())).get(), method, i, erasedType);
                        final List<Object> rawList = buildRawList(paramValues, componentType, pathVarName);
                        final Seq<Object> rawSeq = CollectionConverters.asScala(rawList).toSeq(); // ! It's not an immutable Seq???? something something it fails at runtime without toSeq making the Buffer a Seq, but why not caught by the type system...
                        arg = scala.collection.immutable.List.class.isAssignableFrom(paramType.getRawType()) ? rawSeq.toList() : rawSeq;
                    } else if (ArgoBody.class.isAssignableFrom(paramType.getRawType())) {
                        final String rawValue = ((paramValues == null) || (paramValues.length == 0)) ? null : paramValues[0];
                        arg = ArgoBody.parse(Optional.ofNullable(rawValue).orElse("null"));
                    } else {
                        final String rawValue = ((paramValues == null) || (paramValues.length == 0)) ? null : paramValues[0];
                        final TypeToken<?> targetTypeToken = TypeToken.of(targetType);
                        final Optional<TypeToken<?>> optionalTypeToken =
                          TypeTokens.resolveOptionLikeElementType(targetTypeToken);

                        // Check for optional when param not required
                        if (optionalTypeToken.isPresent()) {
                            final TypeToken<?> genericType = unerase(optionalTypeToken.get(), method, i, erasedType);
                            final Object value = (rawValue == null) ? null : convert(rawValue, pathVarName, genericType, isParamRequired);
                            arg = OptionLike.ofNullable(targetTypeToken.getRawType(), value);
                        } else {
                            arg = (rawValue == null)
                              ? getDefaultValue(i).orElse(null)
                              : convert(rawValue, pathVarName, paramType, isParamRequired);
                        }
                    }
                } else if (requestBodyFound) {
                    arg = resolveRequestBody(requestBody, request, targetType);
                } else {
                    throw new RuntimeException("Unsupported request mapping method argument '" + method
                            .getParameterTypes()[i] + "' for method '" + method.getName() + "'");
                }
            }

            args[i] = arg;

        }

        if (request.getRawRequest() != null) {
            final Map<String, String[]> unexpectedQueryParams = Maps.filterKeys(
              request.getRawRequest().getParameterMap(),
              paramName -> !consumedQueryParamNames.contains(paramName)
            );

            if (!unexpectedQueryParams.isEmpty()) {
                SrsLog.logUnexpectedQueryParams(unexpectedQueryParams);
            }
        }

        // TODO: I think this should also probably check for ApiQuery support but I'm
        // not sure the use case or mechanics.
        if (checkPaginationSupport && !request.isEmbed() && pathSpecifiesApiPage(lastSegment) && !supportsPagination) {
            throw new ResourceNotFoundException(
                    "A '" + joinPathSegments(uriPathSegments) +
                            "' that supports pagination " +
                    "cannot be " +
                    "found.");
        }

        return args;
    }

    private static TypeToken<?> unerase(TypeToken<?> token, Method method, int index, Class<?> erasedType) {
        return (token.getRawType() != Object.class) ? token
            : TypeToken.of(erasedType); // ReflectionSupport.getTypeParameter(method, index);
    }

    /**
     * Will one day be a sophisticated conversion facility.
     * ... "one day" ...
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> Object convert(final String source, final String name, TypeToken<T> paramType, boolean required) {
        Function0<T> bail = () -> {
            if (required || source != null) {
                throw new TypeMismatchException(source, name, paramType.getRawType());
            } else {
                return null;
            }
        };

        // relatively horrid special case but Guava doesn't understand scala bytecode peculiarities
        if (EnumLike.isEnumType(paramType.getRawType())) {
            if (source == null) {
                return bail.apply();
            } else {
                return EnumLike.fromName(source, paramType.getRawType())
                  .getOrElse(bail);
            }
        }

        // poor man's "for comprehension"
        Optional<StringConverter<?>> foundConverter = ConversionService.getInstance().get(paramType);
        if (foundConverter.isPresent()) {
            Option<Object> converted = foundConverter.get().apply(new Raw(source, paramType));
            if (converted.isDefined()) {
                return converted.get();
            }
        }

        return bail.apply();
    }

    private List<Object> buildRawList(String[] values, TypeToken<?> componentType, String pathVarName) {
        final List<Object> list = new ArrayList<>();
        if (values != null) {
            for (final String rawValue : values) {
                list.add(convert(rawValue, pathVarName, componentType, true));
            }
        }

        return list;
    }

    private String joinPathSegments(final List<UriPathSegment> pathSegments) {
        return Joiner.on('/').join(pathSegments);
    }

    private String uriTemplate() {

        final Class<?> delegateClass = _function.getDelegate().getDelegateClass();
        final Optional<Class<?>> requestMappingClass =
                BreadthFirstSupertypeIterable
                  .from(delegateClass)
                  .filter(ClassUtils.hasAnnotation(RequestMapping.class))
                  .findFirst();

        final String methodLevelPath = methodUriTemplate();

        final String uriTemplate;
        if (requestMappingClass.isPresent()) {
            final RequestMapping classLevelAnnotation =
                    requestMappingClass.get().getAnnotation(RequestMapping.class);
            final String classLevelPath = classLevelAnnotation.path();
            if (RequestMapping.EMPTY_PATH.equals(methodLevelPath)) {
                uriTemplate = classLevelPath;
            } else {
                uriTemplate = classLevelPath + "/" + methodLevelPath;
            }
        } else {
            uriTemplate = methodLevelPath;
        }

        return uriTemplate;
    }

    protected abstract String methodUriTemplate();

    protected abstract ApiPage resolvePage(
            final DePathSegment lastSegment, final Annotation[] parameterAnnotations);

    protected abstract ApiQuery resolveQuery(
            DePathSegment lastSegment, boolean embed,
            final Annotation[] parameterAnnotations, TypeToken<?> returnType);

    protected abstract <T> T resolveRequestBody(
            final RequestBody requestBody, final WebRequest webRequest, final Type targetType);

    private boolean pathSpecifiesApiPage(final Optional<DePathSegment> lastSegment) {
        if (lastSegment.isPresent()) {

            final Map<MatrixParameterName, String> systemMatrixParameters =
                    lastSegment.get().getUriPathSegment().getSystemMatrixParameters();

            return systemMatrixParameters.containsKey(OFFSET) ||
                    systemMatrixParameters.containsKey(LIMIT);

        } else {
            return false;
        }
    }

    private Optional<Object> getDefaultValue(int ix) {
        return OptionConverters.toJava(
          ReflectionSupport.defaultValue(getObject(), getFunction().getMethod(), ix)
        );
    }

}
