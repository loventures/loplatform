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

package loi.cp.request;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.component.web.TextResponse;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.util.logging.HttpServletRequestLogRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.logging.Level;

@Component
@ServletBinding(
    path = "/sys/request",
    system = true
)
public class RequestServlet extends AbstractComponentServlet {
    @Override
    public WebResponse service(HttpServletRequest request, HttpServletResponse response) {
        return TextResponse.plain(new HttpServletRequestLogRecord(Level.INFO, request, "").getMessage());
    }
}
