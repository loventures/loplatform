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

package com.learningobjects.cpxp.component.web;

import com.google.common.base.Joiner;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.BaseApiPage;
import com.learningobjects.cpxp.component.web.exception.UnsupportedRequestBodyType;
import com.learningobjects.cpxp.util.Out;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.net.URLUtils;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletRequest;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A richer {@link HttpServletRequest}. TODO rename HttpInputMessage
 */
public class WebRequest {

    /**
     * Nothing to see here...
     *
     * Clients correlate a schema to a request body by specifying the URI of the schema
     * in the request. But, instead of dereferencing it and caching it and being a good
     * REST citizen, I crack it open.
     */
    private static final Pattern SCHEMA_URI_PROFILE_PATTERN = Pattern.compile(".*?/api/v(\\d+)/schema/([^.]+)");


    private final HttpServletRequest httpServletRequest;

    /**
     * The path of the request URI stripped of matrix parameters and preamble, /api/v/. Always relative. For example,
     * the {@code districts/3/schools} is the {@link WebRequest} path in a request URI {@code
     * /api/v6/districts/3/schools}
     */
    private final String path;
    private final List<UriPathSegment> uriPathSegments;
    private final List<DePathSegment> dePathSegments;

    /**
     * The schema name that the client correlated in the request, null if no such correlation was made.
     */
    private final String requestBodySchemaName;

    private final int version;
    private final Method method;
    private final String requestUriApiHost;
    private final List<MediaType> acceptedMediaTypes;
    private final MediaType contentType;
    private final boolean embed;
    private final Map<Class<?>, Object> outs;

    /**
     * the limit and offset of this request, populated by matrix parameters on the URI or annotations on the Page
     * argument of the targeted {@link RequestMapping}. Absent if targeted {@link RequestMapping} has no {@link BaseApiPage}
     * argument. Present if targeted {@link RequestMapping} has a {@link BaseApiPage} argument, even if URI has no offset
     * and limit.
     */
    private Optional<ApiPage> page;

    /**
     * filter, order, paging parameters to this request, populated by the relevant matrix parameters
     */
    private Optional<ApiQuery> query;

    public WebRequest(final WebRequestBuilder builder) {
        httpServletRequest = builder.httpServletRequest;
        path = builder.path;
        uriPathSegments = builder.uriPathSegments;
        dePathSegments = builder.dePathSegments;
        requestBodySchemaName = builder.requestBodySchemaName;
        version = builder.version;
        method = builder.method;
        requestUriApiHost = builder.requestUriApiHost;
        acceptedMediaTypes = builder.acceptedMediaTypes;
        contentType = builder.contentType;
        page = builder.page;
        query = builder.query;
        embed = builder.embed;
        outs = new HashMap<>(1);
    }

    public HttpServletRequest getRawRequest() {
        return httpServletRequest;
    }

    public String getPath() {
        return path;
    }

    public List<UriPathSegment> getUriPathSegments() {
        return uriPathSegments;
    }

    public List<DePathSegment> getDePathSegments() {
        return dePathSegments;
    }

    public String getRequestBodySchemaName() {
        return requestBodySchemaName;
    }

    public int getVersion() {
        return version;
    }

    public Method getMethod() {
        return method;
    }

    public String getRequestUriApiHost() {
        return requestUriApiHost;
    }

    public List<MediaType> getAcceptedMediaTypes() {
        return acceptedMediaTypes;
    }

    public boolean acceptsMediaType(String mediaType) {
        return acceptsMediaType(MediaType.parseMediaType(mediaType));
    }

    public boolean acceptsMediaType(MediaType mediaType) {
        for (MediaType accepted : acceptedMediaTypes) {
            if (accepted.includes(mediaType)) { // mediaType.is(accepted)) {
                return true;
            }
        }
        return false;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public Optional<ApiPage> getPage() {
        return page;
    }

    public void setPage(@Nonnull final ApiPage page) {
        this.page = Optional.of(page);
    }

    public Optional<ApiQuery> getQuery() {
        return query;
    }

    public void setQuery(@Nonnull final ApiQuery query) {
        this.query = Optional.of(query);
    }

    public boolean isEmbed() { return this.embed; }

    /** Mutable out values on the request are somewhat abominable, but this is the only container
     * that is currently passed through the request process to the requisite parts. A better option
     * would be to return a composite result within SRS that encapsulated the return value and any
     * out parameters but that would be quite invasive. Alternatively, use SequenceContext.
     * But again, hard. Making this totally generic may, in fact, be just wrong-headed. Might we
     * want to accumulate invalidation keys along the chain, but only consider cacheability from the
     * final invocation? */

    public void clearOutValues() { this.outs.clear(); }

    public <T> T getOutValue(Class<T> type) { return type.cast(this.outs.get(type)); }

    /** Get an out parameter that sets the specified type attribute on this request. */
    public <T> Out<T> getOutParameter(final Class<T> type) {
        return t -> outs.put(type, t);
    }

    public static WebRequest withoutRawRequest(WebRequest request) {
        return new WebRequestBuilder()
            .addUriPathSegments(request.getUriPathSegments())
            .addDePathSegments(request.getDePathSegments())
            .version(request.getVersion())
            .method(request.getMethod())
            .requestUriApiHost(request.getRequestUriApiHost())
            .addAcceptedMediaTypes(request.getAcceptedMediaTypes())
            .contentType(request.getContentType())
            .page(request.getPage().orElse(null))
            .query(request.getQuery().orElse(null))
            .build();
    }

    /* Just using a builder so constructor arg list isn't huge */
    public static class WebRequestBuilder {
        private final HttpServletRequest httpServletRequest;
        private String path;
        private List<UriPathSegment> uriPathSegments = new ArrayList<>();
        private List<DePathSegment> dePathSegments = new ArrayList<>();
        private String requestBodySchemaName;
        private int version;
        private Method method;
        private String requestUriApiHost;
        private List<MediaType> acceptedMediaTypes = new ArrayList<>();
        private MediaType contentType;
        private Optional<ApiPage> page = Optional.empty();
        private Optional<ApiQuery> query = Optional.empty();
        private boolean embed;

        public WebRequestBuilder() {
            this.httpServletRequest = null;
        }

        public WebRequestBuilder(final HttpServletRequest httpServletRequest) {
            this.httpServletRequest = httpServletRequest;
        }

        public WebRequestBuilder addUriPathSegment(final UriPathSegment uriPathSegment) {
            this.uriPathSegments.add(uriPathSegment);
            return this;
        }

        public WebRequestBuilder addUriPathSegments(final List<UriPathSegment> uriPathSegments) {
            this.uriPathSegments.addAll(uriPathSegments);
            return this;
        }

        public WebRequestBuilder addDePathSegment(final DePathSegment dePathSegment) {
            this.dePathSegments.add(dePathSegment);
            return this;
        }

        public WebRequestBuilder addDePathSegments(final List<DePathSegment> dePathSegments) {
            this.dePathSegments.addAll(dePathSegments);
            return this;
        }

        public WebRequestBuilder version(final int version) {
            this.version= version;
            return this;
        }

        public WebRequestBuilder method(final Method method) {
            this.method = method;
            return this;
        }

        public WebRequestBuilder requestUriApiHost(final String requestUriApiHost) {
            this.requestUriApiHost = requestUriApiHost;
            return this;
        }

        public WebRequestBuilder addAcceptedMediaType(final MediaType acceptedMediaType) {
            acceptedMediaTypes.add(acceptedMediaType);
            return this;
        }

        public WebRequestBuilder addAcceptedMediaTypes(final List<MediaType> acceptedMediaTypes) {
            this.acceptedMediaTypes.addAll(acceptedMediaTypes);
            return this;
        }

        public WebRequestBuilder contentType(final MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public WebRequestBuilder page(ApiPage page) {
            this.page = Optional.ofNullable(page);
            return this;
        }

        public WebRequestBuilder query(ApiQuery query) {
            this.query = Optional.ofNullable(query);
            return this;
        }

        public WebRequestBuilder embed(boolean embed) {
            this.embed = embed;
            return this;
        }

        public WebRequest build() {

            //checkNotNull(httpServletRequest); // for in-app GET requests these are null
            checkState(version > 0);
            checkNotNull(method);
            //checkNotNull(requestUriApiHost);
            checkState(uriPathSegments.size() == dePathSegments.size());

            uriPathSegments = List.copyOf(uriPathSegments);
            dePathSegments = List.copyOf(dePathSegments);
            path = Joiner.on('/').join(uriPathSegments.stream().map(UriPathSegment.GET_SEGMENT).collect(Collectors.toList()));

            final List<MediaType> mediaRanges = new ArrayList<>(acceptedMediaTypes);
            MediaType.sortBySpecificityAndQuality(mediaRanges);
            acceptedMediaTypes = List.copyOf(mediaRanges);

            if (contentType != null) {
                String profile = contentType.getParameter("profile");

                if (profile != null) {

                    final String profileValue;
                    if (StringUtils.isQuotedString(profile)) {
                        // isQuotedString->true means length is at least 2.
                        profileValue = profile.substring(1, profile.length()-1);
                    } else {
                        profileValue = URLUtils.decode(profile);
                    }

                    final Matcher m = SCHEMA_URI_PROFILE_PATTERN.matcher(profileValue);
                    if (!m.matches()) {
                        throw new UnsupportedRequestBodyType(httpServletRequest.getContentType(), Collections.emptySet());
                    }
                    requestBodySchemaName = m.group(2);
                }

            }

            return new WebRequest(this);
        }
    }


}
