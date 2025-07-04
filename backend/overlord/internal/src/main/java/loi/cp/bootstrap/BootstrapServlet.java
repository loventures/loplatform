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

package loi.cp.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.logging.LogCapture;
import com.learningobjects.cpxp.util.logging.ServletThreadLogWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import loi.cp.overlord.EnforceOverlordAuth;
import loi.nashorn.Nashorn;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@EnforceOverlordAuth
@ServletBinding(
  path = "/sys/bootstrap",
  system = true
)
public class BootstrapServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(BootstrapServlet.class.getName());

    @Override
    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpUtils.setExpired(response);

        response.setContentType(MimeUtils.MIME_TYPE_TEXT_PLAIN + MimeUtils.CHARSET_SUFFIX_UTF_8);
        try (PrintWriter writer = response.getWriter()) {
            ServletThreadLogWriter handler = new ServletThreadLogWriter(response, writer, BootstrapServlet.class);
            LogCapture.captureLogs(handler, () -> {
                try {
                    JsonNode bootstrap = parseBootstrap(request);
                    bootstrap(null, bootstrap);
                    logger.log(Level.INFO, "OK");
                } catch (Throwable th) {
                    ManagedUtils.rollback();
                    logger.log(Level.WARNING, "Bootstrap failed", th);
                }
            });
        } finally {
            BootstrapInstance.clearS3();
        }
    }

    private JsonNode parseBootstrap(HttpServletRequest request) throws Exception {
        Reader reader;
        boolean javascript = false;
        if (request.getContentType().contains("multipart")) {
            StringBuilder sb = new StringBuilder();
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    javascript |= part.getContentType().contains("javascript");
                    try (InputStream in = part.getInputStream()) {
                        sb.append(IOUtils.toString(in, "UTF-8"));
                    }
                }
                part.delete();
            }
            reader = new StringReader(sb.toString());
        } else {
            javascript = request.getContentType().contains("javascript");
            reader = request.getReader();
        }
        if (!javascript) {
            return ComponentUtils.fromJson(reader, JsonNode.class);
        }
        Object o = new Nashorn().eval(reader);
        String json = (o instanceof String) ? (String) o : ComponentUtils.toJson(o);
        logger.log(Level.INFO, "JSON {0}", json);
        return ComponentUtils.fromJson(json, JsonNode.class);
    }

    @Bootstrap("bootstrap.configure")
    public void initS3(JsonConfig json) {
        BootstrapInstance.initS3(json.s3Identity, json.s3Credential);
    }

    public static class JsonConfig {
        public String s3Identity;
        public String s3Credential;
    }

    void bootstrap(Long ctx, JsonNode bootstrap) throws Exception {
        for (JsonNode entry : bootstrap) {
            String phase = entry.path("phase").textValue();
            logger.log(Level.INFO, "Bootstrap phase {0}", phase);
            if (phase.startsWith("#")) {
                continue;
            }
            JsonNode config = entry.path("config");
            BootstrapInstance function = BootstrapInstance.lookup(phase);
            if (function == null) {
                throw new Exception("Unknown bootstrap phase: " + phase);
            }
            Long context = null;
            if (config.isArray() && !function.hasCollectionParameter()) {
                for (JsonNode cf : config) {
                    function.invoke(ctx, cf);
                }
            } else {
                context = function.invoke(ctx, config);
            }
            ManagedUtils.commit(); // meh, but probably wise
            JsonNode setup = entry.path("setup");
            if (!setup.isMissingNode()) {
                bootstrap(context, setup);
            }
        }
    }
}
