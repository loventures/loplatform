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

package loi.cp.deploy;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.util.FileUtils;
import com.learningobjects.cpxp.util.ImageUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

// ugh
@Component
@ServletBinding(path = "/sys/styling")
public class StylingServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(StylingServlet.class.getName());
    @Inject
    private DomainWebService _domainWebService;

    @Override
    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final PrintWriter pw = response.getWriter();
        try {
            DeployUtils.validateApiKey(request);

            if (!request.getContentType().contains("multipart")) {
                throw new Exception("Expected a multipart upload");
            }
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    Operations.asRoot(
                      new VoidOperation() {
                          @Override
                          public void execute() throws Exception {
                              installFile(part, pw);
                          }
                      });
                }
                part.delete();
            }
            pw.println("OK");
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Styling error", ex);
            DeployUtils.sendError(response, pw, ex);
        } finally {
            pw.close();
        }
    }

    private void installFile(Part part, PrintWriter pw) throws Exception {
        File file = File.createTempFile("car", "zip");
        file.deleteOnExit();
        try {
            pw.println("Parsing attachment"); pw.flush();

            try (InputStream in = part.getInputStream()) {
                try (OutputStream out = FileUtils.openOutputStream(file)) {
                    IOUtils.copy(in, out);
                }
            }

            installFile(part.getName(), part.getSubmittedFileName(), file, pw);
        } finally {
            file.delete();
        }
    }

    public void installFile(String name, String fileName, File file, PrintWriter pw) throws Exception {
        String dataType;
        if ("favicon".equals(name)) {
            dataType = DomainConstants.DATA_TYPE_FAVICON;
        } else if ("css".equals(name)) {
            dataType = DomainConstants.DATA_TYPE_DOMAIN_CSS;
        } else if ("logo".equals(name)) {
            dataType = DataTypes.DATA_TYPE_LOGO;
        } else {
            throw new Exception("Unknown field: " + name);
        }

        Long width = null, height = null;
        try {
            ImageUtils.Dim dim = ImageUtils.getImageDimensions(file);
            width = (long) dim.getWidth();
            height = (long) dim.getHeight();
        } catch (Exception ignored) {
        }

        pw.println("Installing attachment: " + name + " (" + fileName + ")");

        if ("css".equals(name) && StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
            _domainWebService.setCssZip(fileName, file);
        } else {
            _domainWebService.setImage(dataType, fileName, width, height, file);
        }
    }
}
