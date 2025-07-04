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
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.script.ComponentArchiveFacade;
import com.learningobjects.cpxp.service.script.ScriptService;
import com.learningobjects.cpxp.util.CdnUtils;
import com.learningobjects.cpxp.util.FileUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: This could be a system servlet that does manual domain lookup. Maybe.
@Component
@ServletBinding(path = "/sys/install")
public class InstallServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(InstallServlet.class.getName());
    @Inject
    private ScriptService _scriptService;

    @Override
    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();
        try {
            DeployUtils.validateApiKey(request);
            boolean uninstall = "true".equals(request.getParameter("uninstall"));
            boolean overlord = Current.getDomainDTO().getType().equals(DomainConstants.DOMAIN_TYPE_OVERLORD);

            if (request.getContentType().contains("multipart")) {
                for (Part part : request.getParts()) {
                    if (part.getSubmittedFileName() != null) {
                        try (InputStream in = part.getInputStream()) {
                            installCar(in, pw, uninstall, overlord);
                        }
                    }
                    part.delete();
                }
            } else {
                installCar(request.getInputStream(), pw, uninstall, overlord);
            }

            if (overlord) {
                ManagedUtils.commit();
                Thread.sleep(3000L); // wait for cluster invalidations to propagate, should one day add some form of cluster barrier
                pw.println("Invalidating CDN");
                CdnUtils.incrementCdnVersion();
            }

            pw.println("OK");
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Install error", ex);
            DeployUtils.sendError(response, pw, ex);
        } finally {
            pw.close();
        }
    }

    private void installCar(InputStream in, PrintWriter pw, boolean uninstall, boolean overlord) throws Exception {
        File file = File.createTempFile("car", "zip");
        file.deleteOnExit();
        try {
            pw.println("Parsing archive"); pw.flush();

            try (OutputStream out = FileUtils.openOutputStream(file)) {
                IOUtils.copy(in, out);
            }

            pw.println("Installing component archive");
            // TODO: A better name
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String filename = "car-" + sdf.format(new Date()) + ".zip";
            Long folder = _scriptService.getDomainScriptFolder(Current.getDomain()).getId();
            ComponentArchiveFacade archive = _scriptService.installComponentArchive(folder, file, filename);

            if (overlord && uninstall) {
                _scriptService.clusterRemoveComponentArchive(archive.getIdentifier());
            }

            pw.println("Installed: " + archive.getName() + " (" + archive.getIdentifier() + ", version " + archive.getVersion() + ")");
        } finally {
            file.delete();
        }
    }
}
