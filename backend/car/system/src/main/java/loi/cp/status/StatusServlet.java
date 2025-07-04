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

package loi.cp.status;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Messages;
import com.learningobjects.cpxp.component.eval.PathInfoEvaluator;
import com.learningobjects.cpxp.component.util.HtmlTemplate;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.status.StatusWebService;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component(
    name = "$$name=Status Servlet",
    description = "$$description=This servlet reports system status.",
    version = "0.7"
)
@ServletBinding(
    path = "/control/status",
    system = true,
    transact = false
)
public class StatusServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(StatusServlet.class.getName());
    private static final long MAX_OPERATION_TIME = 9000; // 9s should be more than enough time

    private static final String OK = "OK";

    private static long __then;
    private static int __good;
    private static int __bad;

    @Inject
    private StatusWebService _statusWebService;

    @Override
    @Messages({ "$$title=Status: {0}",
                "$$status_cacheStorage=Cache Storage: {0}",
                "$$status_attachmentStorage=Attachment Storage: {0}",
                "$$status_database=Database: {0}",
                "$$status_system=System Status: {0}" })
    public WebResponse service(HttpServletRequest req, HttpServletResponse rsp) {
        if (!req.getMethod().toUpperCase().equals("GET")) {
            return ErrorResponse.methodNotAllowed();
        }

        String path= (String) req.getAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO);
        if (StringUtils.isNotEmpty(path)) {
            return ErrorResponse.notFound(path);
        }

        long then = System.currentTimeMillis();
        String cacheStorage = test("CacheStorage", new VoidOperation() {
                    @Override
                    public void execute() {
                        _statusWebService.testCacheStorage();
                    }
                });
        String attachmentStorage = OK/*test("AttachmentStorage", new VoidOperation() {
                    @Override
                    public void execute() {
                        _statusWebService.testAttachmentStorage();
                    }
                })*/; // disabled to avoid the daily 7am alerts due to the backup process which causes the NFS to stall for 30s
        String database = test("Database", new VoidOperation() {
                    @Override
                    public void execute() {
                        _statusWebService.testDatabase();
                    }
                });
        boolean good = OK.equals(cacheStorage) && OK.equals(attachmentStorage) && OK.equals(database);
        String systemStatus = good ? "GOOD" : "BAD";
        long now = System.currentTimeMillis(), elapsed = now - then;

        logger.log(Level.FINE, "System Status, {0}, {1}", new Object[]{systemStatus, elapsed});

        synchronized (StatusServlet.class) {
            if (good) {
                ++ __good;
            } else {
                ++ __bad;
            }
            if (now - __then >= 60000L) {
                logger.log(Level.INFO, "System Status summary, {0}, {1}", new Object[]{__good, __bad});
                __then = now;
                __good = __bad = 0;
            }
        }

        HtmlTemplate html = HtmlTemplate.apply(this, "status.html")
          .bind("attachmentStorage", attachmentStorage)
          .bind("cacheStorage",      cacheStorage     )
          .bind("database",          database         )
          .bind("systemStatus",      systemStatus     )
          .bind("now",               Current.getTime())
          ;
        return HtmlResponse.apply(html);
    }

    private String test(String name, VoidOperation op) {
        try {
            long then = System.currentTimeMillis();
            Operations.transact(op);
            long elapsed = System.currentTimeMillis() - then;
            if (elapsed >= MAX_OPERATION_TIME) {
                logger.log(Level.WARNING, name + " Status operation timeout, {0}", new Object[]{elapsed});
                return "Fail: Timeout";
            }
            logger.log(Level.FINE, name + " Status operation completed, {0}", new Object[]{elapsed});
            return OK;
        } catch (Exception ex) {
            logger.log(Level.WARNING, name + " Status operation error", ex);
            return "Fail: " + ex.getMessage();
        }
    }
}
