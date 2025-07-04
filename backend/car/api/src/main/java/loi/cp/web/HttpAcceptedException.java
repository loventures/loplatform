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
import jakarta.servlet.http.HttpServletResponse;

public class HttpAcceptedException extends HttpResponseException {
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_CHALLENGE = "challenge";
    public static final String STATUS_ASYNC = "async";

    private final String _status;

    public HttpAcceptedException(String message) {
        this(message, STATUS_ERROR);
    }

    public HttpAcceptedException(String message, String status) {
        super(message, HttpServletResponse.SC_ACCEPTED);
        _status = status;
    }

    @JsonProperty
    public String getStatus() {
        return _status;
    }
}

