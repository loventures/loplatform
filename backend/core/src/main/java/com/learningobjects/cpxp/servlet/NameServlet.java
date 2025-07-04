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

package com.learningobjects.cpxp.servlet;

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.RpcInstance;
import com.learningobjects.cpxp.component.eval.PathInfoEvaluator;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.web.AbstractRpcServlet;
import com.learningobjects.cpxp.component.web.WebResponseOps;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.service.accesscontrol.AccessControlException;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.data.DataWebService;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.domain.DomainWebService.PathStatus;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemWebService;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "Name", asyncSupported = true, loadOnStartup = 2, urlPatterns = "/*")
public class NameServlet extends AbstractServlet {
    private static final Logger logger = Logger.getLogger(NameServlet.class.getName());
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    @Inject
    private DataWebService _dataWebService;

    @Inject
    private DomainWebService _domainWebService;

    @Inject
    private ItemWebService _itemWebService;

    @Inject
    private MimeWebService _mimeWebService;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            doService(request, response);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Name servlet error", ex);
        }
    }

    private void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pathInfo = URLDecoder.decode(StringUtils.substringBefore(request.getRequestURI(), ";jsessionid"), "UTF-8");
        String view = getViewFromRequestPath(pathInfo);
        String path = getPathFromRequestPath(pathInfo);
        String queryString = request.getQueryString();

        if (path.startsWith("/control")) {
            logger.log(Level.FINE, "Access denied" + " {0}", path);
            throw new AccessControlException(path);
        } else if ("null".equals(view)) {
            logger.log(Level.FINE, "Ignoring null action" + " {0}", pathInfo);
            return;
        }

        HttpUtils.setExpired(response);

        int rscIndex = path.indexOf("/$rsc/"); // hack
        String rsc = (rscIndex < 0) ? null : path.substring(6 + rscIndex);
        path = (rscIndex < 0) ? path : path.substring(0, rscIndex);

        PathStatus pathStatus = _domainWebService.checkPathStatus(path);

        Item item = pathStatus.getItem();

        if (pathStatus.isNotFound()) {
            Long id = (item == null) ? null : item.getId();
            String itemType = (item == null) ? null : item.getItemType();
            final ComponentDescriptor component = ComponentSupport.lookupComponent(ItemSiteComponent.class, itemType, null, "view");
            final ComponentInstance instance;
            if (component != null) {
                instance = component.getInstance(null, null);
            } else {
                instance = ComponentSupport.getComponent(item, null);
            }
            final RpcInstance rpc = (instance == null) ? null
              : instance.getFunctionInstance(RpcInstance.class, request.getMethod(), "notFound");
            if (rpc == null) {
                throw new FileNotFoundException();
            }
            final String url = _dataWebService.getString(id, DataTypes.DATA_TYPE_URL);
            final String pi = path.substring(url.length());
            request.setAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO, pi);
            AbstractRpcServlet.perform(rpc, request, response, logger);
            return;
        }

        if (item == null) {
            logger.log(Level.FINE, "Access denied" + " {0}", path);
            throw new AccessControlException(path);
        }
        Long itemId = item.getId();
        String itemType = item.getItemType();

        request.setAttribute(SessionUtils.REQUEST_ATTRIBUTE_ITEM, itemId);

        // TODO: This is just a piece of fscking carp waiting for better infrastructure

        ComponentDescriptor component = null;
        String identifier = _dataWebService.getString(itemId, ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER);
        if (identifier != null) {
            component = ComponentSupport.getComponentDescriptor(identifier);
        }
        if ((component == null) || !component.isSupported(ItemSiteComponent.class)) {
            String subType = _dataWebService.getString(itemId, DataTypes.DATA_TYPE_TYPE);
            component = ComponentSupport.lookupComponent(ItemSiteComponent.class, itemType, subType, view);
        }

        if (component != null) {
            siteView(request, response, component, itemId, view);
        } else {
            if (rsc != null) {
                // this is arguably useful but can probably die.
                URL url = component.getResource(rsc); // ???
                if (url == null) {
                    logger.log(Level.FINE, "Unknown script resource" + " {0}", rsc);
                }
                File file = null;
                try {
                    file = new File(url.toURI());
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Invalid script resource" + " {0}, {1}", new Object[] {rsc, url});
                }
                if ((file == null) || !file.exists() || !file.isFile() || !file.canRead()) {
                    throw new FileNotFoundException();
                }

                FileInfo fileInfo = new LocalFileInfo(file);
                // force no cache for now because his is intended for use in Web deploy
                // of front end resources and caching will make life hard.
                fileInfo.setExpires(0);
                fileInfo.setEternal(false);
                fileInfo.setDoCache(false);

                String contentType = _mimeWebService.getMimeType(rsc); // the filename may be md5'ed
                fileInfo.setContentType(contentType);

                logger.log(Level.FINE, "Sending file" + " {0}", fileInfo);
                request.setAttribute(SendFileFilter.REQUEST_ATTRIBUTE_SEND_FILE, fileInfo);
            } else {
                siteView(request, response, null, itemId, view);
            }
        }
    }

    private static String getPathFromRequestPath(String pathInfo) {
        int index = pathInfo.lastIndexOf('!');
        return (index < 0) ? pathInfo : pathInfo.substring(0, index);
    }

    private static String getViewFromRequestPath(String pathInfo) {
        int index = pathInfo.lastIndexOf('!');
        return (index < 0) ? null : pathInfo.substring(1 + index);
    }

    private void siteView(final HttpServletRequest request, final HttpServletResponse response, ComponentDescriptor itemComponent, Long id, String view) throws Exception {
        Item dto = _itemWebService.getItem(id);
        ComponentInstance itemInstance;
        if (itemComponent != null) {
            itemInstance = itemComponent.getInstance(dto, null);
        } else {
            itemInstance = ComponentSupport.getComponent(dto, null);
            itemComponent = itemInstance.getComponent();
        }

        String subview = null;
        String pathInfo = null;
        if (StringUtils.isNotEmpty(view)) {
            int slash = view.indexOf('/');
            subview = (slash < 0) ? view : view.substring(0, slash);
            pathInfo = (slash < 0) ? "" : view.substring(slash);
        }

        ComponentDescriptor viewComponent = itemComponent;
        ComponentInstance viewInstance = itemInstance;

        String rm = request.getMethod();
        if ("GET".equals(rm) && StringUtils.isEmpty(subview)) {
            subview = "view";
        }
        if (StringUtils.isNotEmpty(subview)) {
            FunctionDescriptor function =
              ComponentSupport.getEnvironment().getRegistry().getFunction(viewComponent, RpcInstance.class, rm, subview);
            if (function != null) {
                request.setAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO, pathInfo);
                RpcInstance rpc = viewInstance.getFunctionInstance(RpcInstance.class, function);
                if (rpc == null) {
                    throw new FileNotFoundException();
                }
                AbstractRpcServlet.perform(rpc, request, response, logger);
                return;
            }
        }

        final ItemSiteComponent site = viewInstance.getInstance(ItemSiteComponent.class);
        WebResponseOps.send(site.renderSite(view), request, response);
    }

}
