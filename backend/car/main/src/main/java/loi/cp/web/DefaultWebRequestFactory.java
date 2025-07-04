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

package loi.cp.web;

import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.query.*;
import com.learningobjects.cpxp.component.query.ApiQuery.Builder;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.component.web.WebRequest.WebRequestBuilder;
import com.learningobjects.cpxp.component.web.exception.TypeMismatchException;
import com.learningobjects.cpxp.component.web.exception.UnsupportedVersionException;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.net.URLUtils;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.math.NumberUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.learningobjects.cpxp.component.web.MatrixParameterName.*;
import static com.learningobjects.cpxp.util.StringUtils.*;

/**
 * Creates {@link WebRequest}s from {@link HttpServletRequest}s
 */
@Service
public class DefaultWebRequestFactory implements WebRequestFactory {

    /**
     * A version path segment is a "v" followed by a 1 to 9 digit number
     */
    private static final Pattern VERSION_PATH_SEGMENT = Pattern.compile("v(\\d{1,9})");

    // property:operator(value),...
    private static final String FILTER_PATTERN = "(\\w+(?:\\.\\w+)?)(?::(\\w+))?\\(([^)]*)\\)";

    private static final Pattern FILTERS_RE =
            Pattern.compile(FILTER_PATTERN + "(?:," + FILTER_PATTERN + ")*");

    private static final Pattern FILTER_RE = Pattern.compile(FILTER_PATTERN);

    public WebRequest create(final HttpServletRequest httpServletRequest) {

        final WebRequest.WebRequestBuilder builder =
                new WebRequest.WebRequestBuilder(httpServletRequest);

        /*
         * pathSegments will be paths and their matrix parameters (if any)
         */
        final List<String> pathSegments =
                PATH_SPLITTER.splitToList(httpServletRequest.getRequestURI());

        /*
         * request URI will always be like /api/..., so first two splits are "" and api,
         * skip them
         */
        final int version = getVersion(pathSegments.get(2));

        /* skip "", "api", and the version */
        for (int i = 3, n = pathSegments.size(); i < n; ++i) {
            final String segment = pathSegments.get(i);

            final String uriPart = substringBefore(segment, ";");

            final String pathPart;
            if (i == n - 1) {
                pathPart = trimFileExt(uriPart);
            } else {
                pathPart = uriPart;
            }

            final String matrixParamPart = substringAfter(segment, ";");


            addUriPathSegment(builder, pathPart, matrixParamPart);

        }

        /** request URI API host */
        final String scheme = httpServletRequest.getScheme();
        final String serverName = httpServletRequest.getServerName();
        final int port = httpServletRequest.getServerPort();
        try {
            final URL requestUriApiHost =
                    new URL(scheme, serverName, port, "/api/v" + version);
            builder.requestUriApiHost(requestUriApiHost.toString());
        } catch (final MalformedURLException e) {
            throw Throwables.propagate(e);
        }

        builder.version(version);
        builder.method(Method.valueOf(httpServletRequest.getMethod()));

        final String contentType = httpServletRequest.getContentType();
        if (contentType != null) {
            // (would be null for GETs for example)
            builder.contentType(MediaType.parseMediaType(contentType));
        }

        for (final MediaType acceptedMediaType : getAcceptedMediaTypes(httpServletRequest)) {
            builder.addAcceptedMediaType(acceptedMediaType);
        }

        return builder.build();
    }

    public WebRequest create(final Method method, final String path) {

        final WebRequest.WebRequestBuilder builder =
                new WebRequest.WebRequestBuilder();

        /*
         * pathSegments will be paths and their matrix parameters (if any)
         */
        final List<String> pathSegments =
                PATH_SPLITTER.splitToList(path);

        /*
         * request URI will always be like /api/..., so first two splits are "" and api,
         * skip them
         */
        final int version = getVersion(pathSegments.get(2));

        /* skip "", "api", and the version */
        for (int i = 3, n = pathSegments.size(); i < n; ++i) {
            final String segment = pathSegments.get(i);

            final String uriPart = substringBefore(segment, ";");

            final String pathPart;
            if (i == n - 1) {
                pathPart = trimFileExt(uriPart);
            } else {
                pathPart = uriPart;
            }

            final String matrixParamPart = substringAfter(segment, ";");


            addUriPathSegment(builder, pathPart, matrixParamPart);

        }

        builder.version(version);
        builder.method(method);

        return builder.build();
    }

    public WebRequest createForEmbed(final String embedPath, final int version, final WebRequest original) {

        HttpServletRequest rawRequest = original.getRawRequest();
        final WebRequestBuilder builder = new WebRequestBuilder(rawRequest);
        builder.version(version);
        builder.method(Method.GET);
        builder.embed(true);

        for (final String s : PATH_SPLITTER.split(embedPath)) {

            final String pathPart = substringBefore(s, ";");
            final String matrixParamPart = substringAfter(s, ";");

            addUriPathSegment(builder, pathPart, matrixParamPart);
        }



        return builder.build();
    }

    /** Error safe. */
    @Override
    public List<MediaType> getAcceptedMediaTypes(
            final HttpServletRequest httpServletRequest) {

        final String afterLastSlash =
                substringAfterLast(httpServletRequest.getRequestURI(), "/");
        final String beforeFirstMatrix = substringBefore(afterLastSlash, ";");
        final String suffix = substringAfter(beforeFirstMatrix, ".");

        final List<MediaType> acceptedMediaTypes = new ArrayList<>();
        if (isNotEmpty(suffix) && SUFFIX_MAP.containsKey(suffix.toLowerCase())) {
            MediaType type = SUFFIX_MAP.get(suffix.toLowerCase());
            acceptedMediaTypes.add((type == null) ? MediaType.APPLICATION_UNKNOWN : type);
        } else {
            final String acceptHeaderValue =
                    httpServletRequest.getHeader(HttpHeaders.ACCEPT);
            acceptedMediaTypes.addAll(MediaType.parseMediaTypes(acceptHeaderValue));
            if (acceptedMediaTypes.isEmpty()) {
                acceptedMediaTypes.add(MediaType.ALL);
            }
        }
        return acceptedMediaTypes;
    }

    private void addUriPathSegment(final WebRequest.WebRequestBuilder builder,
            final String pathPart, final String matrixParamPart) {

        final Map<String, String> params;

        if (isEmpty(matrixParamPart)) {
            params = Collections.emptyMap();
        } else {
            params = MATRIX_PARAM_SPLITTER.split(matrixParamPart);
        }

        final Map<String, String> applicationParams = new HashMap<>();
        final Map<MatrixParameterName, String> systemParams = new HashMap<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            final MatrixParameterName name = MatrixParameterName.byName(param.getKey());

            if (name == null) {
                applicationParams.put(param.getKey(), param.getValue());
            } else {
                systemParams.put(name.normalize(), param.getValue());
            }
        }

        final UriPathSegment uriPathSegment =
                new UriPathSegment(pathPart, applicationParams, systemParams);
        final DePathSegment dePathSegment = createDePathSegment(uriPathSegment);

        builder.addUriPathSegment(uriPathSegment);
        builder.addDePathSegment(dePathSegment);
    }

    private DePathSegment createDePathSegment(final UriPathSegment segment) {

        final FilterOperator filterOp = resolveFilterOp(segment);
        final List<ApiFilter> preFilters = resolvePredicates(PREFILTER, segment);
        final List<ApiFilter> filters = resolvePredicates(FILTER, segment);
        final List<ApiOrder> orders = resolveOrders(segment);
        final ApiPage page = resolvePage(segment);
        final Set<String> embeds = resolveEmbeds(segment);

        final ApiQuery query =
                new Builder().setFilterOp(filterOp).addPrefilters(preFilters)
                        .addFilters(filters).addOrders(orders).setPage(page)
                        .addEmbeds(embeds).build();

        return new DePathSegment(segment, query);
    }

    private ApiPage resolvePage(final UriPathSegment segment) {

        final String rawOffset = segment.getSystemMatrixParameters().get(OFFSET);
        final String rawLimit = segment.getSystemMatrixParameters().get(LIMIT);

        final int offset;
        final int limit;

        if (rawOffset == null) {
            offset = 0;
        } else {
            offset = convertInteger(rawOffset, OFFSET.getName());
        }

        if (rawLimit == null) {
            limit = ApiPage.UNBOUNDED_LIMIT;
        } else {
            final int convertedLimit = convertInteger(rawLimit, LIMIT.getName());
            if (convertedLimit < 0) {
                throw new ValidationException("limit", rawLimit,
                        "limit must be a positive number");
            } else {
                limit = convertedLimit;
            }
        }

        return new BaseApiPage(offset, limit);

    }

    /**
     * Pulls the {@link FilterOperator} out of the request-URI
     */
    private FilterOperator resolveFilterOp(final UriPathSegment lastSegment) {

        final String filterOp =
                lastSegment.getSystemMatrixParameters().get(MatrixParameterName
                        .FILTER_OP);


        final FilterOperator fc;
        if (filterOp == null) {
            fc = FilterOperator.AND;
        } else {
            fc = FilterOperator.byName(filterOp);
        }

        if (fc == null) {
            throw new ValidationException("filterOp", filterOp,
                    "Invalid filterOp value. Must be one of " +
                            Arrays.toString(FilterOperator.values()));
        } else {
            return fc;
        }
    }

    private List<ApiFilter> resolvePredicates(final MatrixParameterName parameter, final UriPathSegment lastSegment) {
        List<ApiFilter> filter = new ArrayList<>();
        final String rawFilter = lastSegment.getSystemMatrixParameters().get(parameter);
        if (rawFilter != null) {

            if (!FILTERS_RE.matcher(rawFilter).matches()) {
                throw new ValidationException("filter", rawFilter,
                        "invalid filter expression");
            }

            final Matcher matcher = FILTER_RE.matcher(rawFilter);
            while (matcher.find()) {

                final String property = matcher.group(1);
                final String operator = matcher.group(2);
                final String value = URLUtils.decode(matcher.group(3));

                final PredicateOperator predicateOperator =
                        PredicateOperator.byName(operator);

                if ((operator != null) && (predicateOperator == null)) {
                    throw new ValidationException("filter", operator,
                            "Unknown predicate operator");
                }

                filter.add(new BaseApiFilter(property, predicateOperator, value));
            }
        }

        return filter;
    }


    private List<ApiOrder> resolveOrders(final UriPathSegment lastSegment) {
        List<ApiOrder> order = new ArrayList<>();

        final String rawOrder = lastSegment.getSystemMatrixParameters().get(ORDER);
        if (rawOrder != null) {
            for (final String ordering : StringUtils.split(rawOrder, ',')) {
                final String[] parts = StringUtils.split(ordering, ':');
                if (parts.length > 2) {
                    throw new ValidationException("order", rawOrder,
                            "order value must be in 'propertyName:direction' form");
                }
                final OrderDirection direction;
                if (parts.length == 1) {
                    direction = OrderDirection.ASC;
                } else {
                    final String dir = parts[1];
                    direction = OrderDirection.byName(dir);
                    if (direction == null) {
                        throw new ValidationException("order", rawOrder,
                                "order direction must be one of " +
                                        Arrays.toString(OrderDirection.values()));
                    }
                }
                order.add(new BaseApiOrder(parts[0], direction));
            }
        }

        return order;
    }

    private Set<String> resolveEmbeds(final UriPathSegment lastSegment) {
        Set<String> embed = new HashSet<>();
        final String rawEmbed = lastSegment.getSystemMatrixParameters().get(EMBED);
        if (rawEmbed != null) {
            for (final String embedding : StringUtils.split(rawEmbed, ',')) {
                embed.add(URLUtils.decode(embedding));
            }
        }
        return embed;
    }

    private int getVersion(String pathSegment) {
        Matcher m = VERSION_PATH_SEGMENT.matcher(pathSegment);
        if (m.matches()) {
            /* regex match makes this safe */
            return Integer.parseInt(m.group(1));
        } else {
            /* version is syntactically invalid */
            throw new UnsupportedVersionException(pathSegment);
        }
    }

    /**
     * <pre>
     * trimFileExt("foo.json") -> "foo"
     * trimFileExt("foo.CSV") -> "foo"
     * trimFileExt("foo") -> "foo"
     * trimFileExt("foo.json.question") -> "foo.json.question"
     * </pre>
     */
    private String trimFileExt(final String untrimmed) {

        final String trimmed;
        final String fileExt = lowerCase(substringAfterLast(untrimmed, "."));
        if (SUFFIX_MAP.containsKey(fileExt)) {
            trimmed = substringBefore(untrimmed, ".");
        } else {
            trimmed = untrimmed;
        }

        return trimmed;
    }

    private int convertInteger(final String source, final String name) {
        if (!NumberUtils.isNumber(source)) {
            throw new TypeMismatchException(source, name, Long.class);
        } else {
            return Integer.parseInt(source);
        }
    }

    private static final Map<String, MediaType> SUFFIX_MAP = new HashMap<>();

    static {
        // Ugh but...
        SUFFIX_MAP.put("txt", MediaType.TEXT_PLAIN_UTF_8);
        SUFFIX_MAP.put("html", MediaType.TEXT_HTML_UTF_8);
        SUFFIX_MAP.put("xml", MediaType.TEXT_XML_UTF_8);
        SUFFIX_MAP.put("csv", MediaType.TEXT_CSV_UTF_8);
        SUFFIX_MAP.put("css", MediaType.TEXT_CSS_UTF_8);
        SUFFIX_MAP.put("json", MediaType.APPLICATION_JSON_UTF8);
        SUFFIX_MAP.put("pdf", MediaType.APPLICATION_PDF);
        SUFFIX_MAP.put("zip", MediaType.APPLICATION_ZIP);
        SUFFIX_MAP.put("docx", MediaType.APPLICATION_OOXML_DOC);
        SUFFIX_MAP.put("properties", new MediaType("application", "properties"));
    }
}
