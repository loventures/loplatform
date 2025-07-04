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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import loi.cp.i18n.BundleMessage;
import com.learningobjects.de.web.UncheckedMessageException;
import org.apache.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Don't use this for server exceptions, because server exceptions must have the server
 * error guid included in the response. Just throw any old exception that isn't resolved
 * by SRS for a server exception that has the guid automatically set on the client
 * response.
 */
// seriously jackson, this is the shortest way to turn off autodetection?
@JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class HttpApiException extends RuntimeException {

    private final Iterable<BundleMessage> errors;

    private final int statusCode;

    /**
     * The request could not be understood by the server due to malformed syntax.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">400
     * Bad Request</a>
     */
    public static HttpApiException badRequest(final BundleMessage... errors) {
        return badRequest(Arrays.asList(errors));
    }

    /**
     * The request could not be understood by the server due to malformed syntax.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">400
     * Bad Request</a>
     */
    public static HttpApiException badRequest(final List<BundleMessage> errors) {
        return new HttpApiException(errors, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * The request could not be understood by the server due to malformed syntax.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">400
     * Bad Request</a>
     */
    public static HttpApiException badRequest(final UncheckedMessageException cause) {
        return new HttpApiException(cause, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * The server understands the content type of the request entity and the syntax is
     * correct, but was unable to process the contained instructions.
     *
     * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">422 Unprocessable
     * Entity</a>
     */
    public static HttpApiException unprocessableEntity(final BundleMessage... errors) {
        return unprocessableEntity(Arrays.asList(errors));
    }

    /**
     * The server understands the content type of the request entity and the syntax is
     * correct, but was unable to process the contained instructions.
     *
     * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">422 Unprocessable
     * Entity</a>
     */
    public static HttpApiException unprocessableEntity(final Iterable<BundleMessage> errors) {
        return new HttpApiException(errors, HttpStatus.SC_UNPROCESSABLE_ENTITY);
    }

    /**
     * The server understands the content type of the request entity and the syntax is
     * correct, but was unable to process the contained instructions.
     *
     * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">422 Unprocessable
     * Entity</a>
     */
    public static HttpApiException unprocessableEntity(
      final UncheckedMessageException cause) {
        return new HttpApiException(cause, HttpStatus.SC_UNPROCESSABLE_ENTITY);
    }


    /**
     * The server has not found anything matching the Request-URI.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">404
     * Not Found</a>
     */
    public static HttpApiException notFound(final BundleMessage... errors) {
        return notFound(Arrays.asList(errors));
    }

    /**
     * The server has not found anything matching the Request-URI.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">404
     * Not Found</a>
     */
    public static HttpApiException notFound(final UncheckedMessageException... errors) {
        return notFound(Stream.of(errors).map(UncheckedMessageException::getErrorMessage)
          .collect(toList()));
    }

    /**
     * The server has not found anything matching the Request-URI.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">404
     * Not Found</a>
     */
    public static HttpApiException notFound(final UncheckedMessageException cause) {
        return new HttpApiException(cause, HttpStatus.SC_NOT_FOUND);
    }

    /**
     * The server has not found anything matching the Request-URI.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">404
     * Not Found</a>
     */
    public static HttpApiException notFound(final Iterable<BundleMessage> errors) {
        return new HttpApiException(errors, HttpStatus.SC_NOT_FOUND);
    }

    public static HttpApiException conflict(BundleMessage error) {
        return new HttpApiException(error, HttpStatus.SC_CONFLICT);
    }

    private HttpApiException(final Iterable<BundleMessage> errors, final int statusCode) {
        this.errors = errors;
        this.statusCode = statusCode;
    }

    public HttpApiException(final BundleMessage error, final int statusCode) {
        this.errors = Collections.singleton(error);
        this.statusCode = statusCode;
    }

    private HttpApiException(final UncheckedMessageException cause, final int statusCode) {
        super(cause);
        this.errors = Collections.singleton(cause.getErrorMessage());
        this.statusCode = statusCode;
    }

    @JsonProperty
    public Iterable<BundleMessage> getErrors() {
        return errors;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpApiException that = (HttpApiException) o;
        return statusCode == that.statusCode &&
          Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errors, statusCode);
    }

    @Override
    public String toString() {
        return "HttpApiException{" +
          "errors=" + errors +
          ", statusCode=" + statusCode +
          '}';
    }
}
