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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.learningobjects.cpxp.component.web.HttpResponse;
import jakarta.servlet.http.HttpServletResponse;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

public class HttpResponseEntity<T> implements HttpResponse {
    private final int _statusCode;
    private final Map<String, String> _headers;
    private final T _object;

    public HttpResponseEntity(int statusCode, T object) {
        _statusCode = statusCode;
        _object = object;
        _headers = Map$.MODULE$.empty(); //todo
    }

    /**
     * Only exists to capture wildcards in S so you can instantiate a new
     * HttpResponseEntity with an unknown body type, that is:
     * <pre>
     *     // Can't do this:
     *     HttpResponseEntity&lt;?&gt; entity = new HttpResponseEntity&lt;?&gt;(...)
     *
     *     // So do this:
     *     HttpResponseEntity&lt;?&gt; entity = HttpResponseEntity.from(...)
     * </pre>
     *
     */
    public static <S> HttpResponseEntity<S> from(final int statusCode, final S object) {
        return new HttpResponseEntity<>(statusCode, object);
    }

    @Override
    public int statusCode() {
        return _statusCode;
    }

    @JsonUnwrapped
    @JsonProperty
    public Object get() {
        return _object;
    }

    public static <T> HttpResponseEntity<T> okay(T object) {
        return new HttpResponseEntity<T>(HttpServletResponse.SC_OK, object);
    }

    public static <T> HttpResponseEntity<T> accepted(T object) {
        return new HttpResponseEntity<T>(HttpServletResponse.SC_ACCEPTED, object);
    }

    public static HttpResponseEntity<?> noContent() {
        return new HttpResponseEntity<Void>(HttpServletResponse.SC_NO_CONTENT, null);
    }

    public static HttpResponseEntity<?> noResponse() {
        return new HttpResponseEntity<Void>(-1, null);
    }

    public static final <T> HttpResponseEntity<T> apply(int statusCode, T object){
        return new HttpResponseEntity<T>(statusCode, object);
    }

    @Override
    public Map<String, String> headers() {
        return _headers;
    }
}
