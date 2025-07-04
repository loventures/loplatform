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

package com.learningobjects.cpxp.component.web;

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.RpcInstance;
import com.learningobjects.cpxp.component.UserException;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.eval.PathInfoEvaluator;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.Html;
import com.learningobjects.cpxp.operation.AbstractOperation;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceException;
import com.learningobjects.cpxp.service.accesscontrol.AccessControlException;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.logging.HttpServletRequestLogRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.FileNotFoundException;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractRpcServlet extends AbstractComponent implements ServletComponent {
    private static final Logger logger = Logger.getLogger(AbstractRpcServlet.class.getName());

    @Infer
    private DelegateDescriptor _delegate;

    protected DelegateDescriptor getDelegateDescriptor() {
        return _delegate;
    }

    @Override
    public WebResponse service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return service(_delegate, getComponentInstance(), request, response, logger);
    }

    public static WebResponse service(DelegateDescriptor delegate, ComponentInstance instance, HttpServletRequest request, HttpServletResponse response, Logger logger) throws Exception {

        ServletBinding binding = delegate.getBinding(ServletComponent.class);
        String rel = StringUtils.removeStart(request.getPathInfo(), binding.path() + "/");
        String fragment = StringUtils.substringBefore(rel, "/");
        RpcInstance rpc = instance.getFunctionInstance(RpcInstance.class, request.getMethod(), fragment, rel);
        if (rpc == null) {
            throw new FileNotFoundException();
        }

        //regular rpc's path info is calculated as /foo/pi1/pi2 ->  pi1/pi2
        String pi = fragment.equals(rel) ? "" : rel.substring(1 + fragment.length());

        request.setAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO, pi);
        return perform(rpc, request, response, logger);
    }

    public static WebResponse perform(final RpcInstance rpc, final HttpServletRequest request, HttpServletResponse response, Logger logger) throws Exception {
        // TODO: Consider whether json errors are okay
        AbstractOperation<Object> operation = new AbstractOperation<Object>() {
            @Override
            public Object perform() {
                try {
                    return rpc.invoke(request);
                } catch (Exception ex) {
                    throw new RuntimeException("RPC error: " + ex.getMessage(), ex);
                }
            }
        };

        boolean isAjax = HttpUtils.isAjax(request), isResponded = false;

        HttpUtils.setExpired(response);
        try {
            if (rpc.isJson()) {
                Object result;
                if (EntityContext.inSession()) {
                    result = operation.perform();
                    // logger.log(Level.WARNING, "Commit Rpc");
                    ManagedUtils.commit();
                } else {
                    // logger.log(Level.WARNING, "Attempt transact"); // never seems to happen. hmm?
                    result = Operations.transact(operation);
                }

                if (result instanceof Callable) {
                    ((Callable) result).call();
                } else if (!response.isCommitted()) {
                    response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8);
                    if (rpc.isVoid()) {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        try (Writer writer = HttpUtils.getWriter(request, response)) {
                            try {
                                ComponentUtils.toJson(result, writer);
                            } catch (Throwable th) {
                                if (response.isCommitted()) {
                                    // I've commited a gzipped
                                    // response so there is no way for
                                    // higher-level machinery to add
                                    // any type of error response, so
                                    // just emit something and carry
                                    // on to rollback and log
                                    isResponded = true;
                                    String guid = GuidUtil.errorGuid();
                                    logger.log(Level.WARNING, "Error during response serialization", guid);
                                    try {
                                        writer.write("##ERROR(" + guid + ")##");
                                    } catch (Exception ignored) {
                                    }
                                }
                                throw th;
                            }
                        }
                    }
                }
            } else if (rpc.isWebResponse()) {
                Object o = operation.perform();
                WebResponse webResponse = (o instanceof WebResponse) ? (WebResponse) o : HtmlResponse.apply((Html) o);
                WebResponseOps.send(webResponse, request, response);
            } else {
                operation.perform();
            }
        } catch (Throwable th) {
            if (isAjax) {
                request.setAttribute("ug:exception", th);
                try {
                    ManagedUtils.rollback(); // start a new tx for the error page
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Rollback error", ex);
                }
                JsonMap errmap = new JsonMap();
                errmap.put("status", "error");
                int index = ExceptionUtils.indexOfType(th, ServiceException.class);
                int code = HttpServletResponse.SC_BAD_REQUEST;
                if (index >= 0) {
                    th = ExceptionUtils.getThrowables(th)[index];
                    if (th instanceof AccessControlException) {
                        // I'd love to set login to true for the first two cases, but
                        // the original page will have been rendered without the login
                        // form so there's no nice way to handle it other than RPC
                        // down a login form...
                        errmap.put("title", InternationalizationUtils.formatMessage("shale_RpcProcessor_accessDenied_title"));
                        boolean isAnonymous = UserType.Anonymous == Current.getUserDTO().getUserType();
                        String expired = SessionUtils.hasLoginExpired(request, response);
                        if (expired != null) {
                            errmap.put("message", InternationalizationUtils.formatMessage("shale_RpcProcessor_session" + expired + "_message"));
                        } else if (isAnonymous) {
                            errmap.put("message", InternationalizationUtils.formatMessage("shale_RpcProcessor_loginRequired_message"));
                        } else {
                            errmap.put("message", InternationalizationUtils.formatMessage("shale_RpcProcessor_accessDenied_message"));
                        }
                        errmap.put("logout", isAnonymous);
                        code = HttpServletResponse.SC_FORBIDDEN;
                    } else if (th instanceof UserException) {
                        UserException ue = (UserException) th;
                        errmap.put("status", ue.getStatus());
                        errmap.put("title", ue.getTitle());
                        errmap.put("message", ue.getMessage());
                        if (UserException.STATUS_CHALLENGE.equals(ue.getStatus()) || UserException.STATUS_CONFIRM.equals(ue.getStatus())) {
                            code = HttpServletResponse.SC_ACCEPTED;
                        }
                    } else {
                        ServiceException se = (ServiceException) th;
                        String prefix = "shale_RpcProcessor_serviceError_" + se.getMsg();
                        errmap.put("title", InternationalizationUtils.formatMessage(prefix + "_title", se.getParams()));
                        errmap.put("message", InternationalizationUtils.formatMessage(prefix + "_message", se.getParams()));
                    }
                } else {
                    logger.log(new HttpServletRequestLogRecord(Level.WARNING, request, logger.getName()));
                    String guid = GuidUtil.errorGuid();
                    logger.log(Level.WARNING, "RPC error: " + guid, th);
                    errmap.put("guid", guid);
                    errmap.put("detail", ExceptionUtils.getRootCauseMessage(th));
                }
                if (!isResponded) {
                    if (!response.isCommitted()) {
                        response.reset();
                    }
                    response.setStatus(code);
                    response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8);
                    ComponentUtils.toJson(errmap, response.getWriter());
                }
            } else {
                // request.setAttribute("ug:okError", rpc.isOkError());
                Throwables.propagateIfPossible(th, Exception.class);
                throw Throwables.propagate(th);
            }
        }
        return NoResponse.instance();
    }
}
