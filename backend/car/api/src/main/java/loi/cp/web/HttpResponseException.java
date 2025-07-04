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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.learningobjects.cpxp.component.web.HttpResponse;
import com.learningobjects.cpxp.service.ServiceException;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

@JsonAutoDetect(getterVisibility = Visibility.NONE, creatorVisibility   = Visibility.NONE,
                fieldVisibility  = Visibility.NONE, isGetterVisibility  = Visibility.NONE,
                setterVisibility = Visibility.NONE)
public class HttpResponseException extends ServiceException implements HttpResponse {
    private final int _statusCode;
    private final Map<String, String> _headers;

    public HttpResponseException(String message, int statusCode) {
        super(message);
        _statusCode = statusCode;
        _headers = Map$.MODULE$.empty();
    }

    @Override
    public int statusCode() {
        return _statusCode;
    }

    @Override
    public Map<String, String> headers() {
        return _headers;
    }
}

