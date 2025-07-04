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

import com.learningobjects.cpxp.component.BaseHtmlWriter;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.template.LohtmlTemplate;
import com.learningobjects.cpxp.component.template.RenderContext;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.util.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;

@Component
@ServletBinding(path = "/sys/render")
public class RenderServlet extends AbstractComponentServlet {
    @Inject
    private NameService _nameService;

    @Override
    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();

        try {
            DeployUtils.validateApiKey(request);

            final String path = request.getParameter("path");
            final Item item = _nameService.getItem(path);
            if (item == null) {
                throw new Exception("Unknown path: " + path);
            }

            final ComponentDescriptor itemSiteComponent = ComponentSupport.lookupComponent
              (ItemSiteComponent.class, item.getType(), null, null);
            final ComponentDescriptor itemComponent;
            final ComponentInstance itemInstance;
            if (itemSiteComponent != null) {
                itemComponent = itemSiteComponent;
                itemInstance = itemComponent.getInstance(item, null);
            } else {
                itemInstance = ComponentSupport.getComponent(item, null);
                itemComponent = itemInstance.getComponent();
            }
            Object itemActual = itemInstance.getInstance();

            BaseHtmlWriter writer = new BaseHtmlWriter(response.getWriter());
            RenderContext context = new RenderContext(itemComponent, itemActual, writer, null, null);

            try (UploadInfo file = UploadInfo.tempFile()) {
                try (OutputStream out = FileUtils.openOutputStream(file.getFile())) {
                    IOUtils.copy(request.getInputStream(), out);
                }
                LohtmlTemplate.getInstance(file.getFile().toURL()).render(context);
            }
        } catch(Exception e){
            DeployUtils.sendError(response, pw, e);
        } finally {
            pw.close();
        }
    }
}
