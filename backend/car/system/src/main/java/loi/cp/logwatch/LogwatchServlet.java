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

package loi.cp.logwatch;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.HtmlResponse;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.component.web.WebResponseOps;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.logging.StandardLogFormatter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.overlord.EnforceOverlordAuth;

import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Log watching servlet. Requires overlord auth. This is not a system servlet so that it
 * can scope log messages down to the domain being accessed.
 */
@Component
@EnforceOverlordAuth
@ServletBinding(path = "/sys/logwatch")
public class LogwatchServlet extends AbstractComponentServlet {
    @Override
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // server can do new logwatch and client didn't ask for old logwatch
        if (SendFileFilter.isNioAvailable() && !MimeUtils.MIME_TYPE_TEXT_PLAIN.equals(request.getHeader("Accept"))) {
            WebResponseOps.send(HtmlResponse.apply(this, "logwatch.html"), request, response);
            return;
        }

        ManagedUtils.end();
        HttpUtils.setExpired(response);
        response.setContentType(MimeUtils.MIME_TYPE_TEXT_PLAIN + MimeUtils.CHARSET_SUFFIX_UTF_8);
        final PrintWriter writer = response.getWriter();
        final String ak = "logwatch." + request.getPathInfo();
        final Thread me = Thread.currentThread();
        request.getServletContext().setAttribute(ak, me);
        final ServiceMeta serviceMeta = BaseServiceMeta.getServiceMeta();
        String st = "/sys/logwatch cpxp-" + serviceMeta.getBuild() + "/" + serviceMeta.getNode() + " on " + new Date();
        writer.println(st);
        // Without this it takes a while for buffers to get large enough for the browser to start displaying...
        for (int i = 1; i < 16; ++ i) {
            int n = (int) (st.length() * Math.sin(Math.PI * i / 16));
            writer.println(StringUtils.repeat(" ", n) + StringUtils.repeat(".", st.length() - n));
        }
        writer.flush();
        response.flushBuffer();
        final String host = request.getServerName();
        final Long domain = Current.getDomain();
        final AtomicBoolean done = new AtomicBoolean(false);
        Handler handler = new Handler() {
            private StandardLogFormatter _formatter = new StandardLogFormatter();

            @Override
            public void close() {
            }

            @Override
            public void flush() {
            }

            @Override
            public void publish(LogRecord record) {
                if (done.get()) {
                    return;
                }
                try {
                    synchronized (writer) { // concurrent writes can cause corruption
                        HttpServletRequest req = BaseWebContext.getContext().getRequest();
                        String hst = (req == null) ? null : req.getServerName();
                        Long dom = Current.getDomain();
                        if (((dom != null) && !domain.equals(dom)) || ((hst != null) && !host.equals(hst))) {
                            // Only log messages for the watched domain.. messy because early requests are
                            // logged before current is set up.
                            return;
                        }
                        writer.write(_formatter.format(record));
                        writer.flush();
                        if (writer.checkError()) {
                            done.set(true);
                        }
                    }
                } catch (Exception ignored) {
                    done.set(true);
                }
            }
        };
        handler.setLevel(Level.ALL);
        Logger logger = Logger.getLogger("");
        logger.addHandler(handler);
        try {
            while (!done.get() && (me == request.getServletContext().getAttribute(ak))) {
                Thread.sleep(1000L);
            }
        } finally {
            logger.removeHandler(handler);
        }

        writer.close();
    }
}
