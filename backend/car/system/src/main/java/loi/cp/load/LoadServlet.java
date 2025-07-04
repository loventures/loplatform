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

package loi.cp.load;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.util.MimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Logger;

@Component(
    name = "$$name=Load Servlet",
    description = "$$description=This servlet reports system load.",
    version = "0.7"
)
@ServletBinding(
    path = "/sys/load",
    system = true,
    transact = false
)
public class LoadServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(LoadServlet.class.getName());
    @Override
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final OperatingSystemMXBean osStats = ManagementFactory.getOperatingSystemMXBean();
        final double loadAverage = osStats.getSystemLoadAverage();

        response.setContentType(MimeUtils.MIME_TYPE_TEXT_PLAIN + MimeUtils.CHARSET_SUFFIX_UTF_8);
        PrintWriter out = response.getWriter();
        out.println((int) Math.min(10 * loadAverage, 100));

    }
}
