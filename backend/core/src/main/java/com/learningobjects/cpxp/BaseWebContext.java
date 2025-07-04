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

package com.learningobjects.cpxp;

import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.util.message.BaseMessageMap;
import com.learningobjects.cpxp.util.message.MessageMap;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BaseWebContext implements WebContext {
    private HttpServletRequest _request;
    private HttpServletResponse _response;
    private MessageMap _messages;
    private MessageMap _constants;
    private ComponentEnvironment _scriptEnvironment;
    private HtmlWriter _scriptWriter;

    @Override
    public final void init(HttpServletRequest request, HttpServletResponse response) {
        _request = request;
        _response = response;
    }

    @Override
    public final void initMessages(MessageMap messages) {
        _messages = messages;
    }

    @Override
    public final void initConstants(MessageMap constants) {
        _constants = constants;
    }

    @Override
    public final void clear() {
        init(null, null);
        _scriptEnvironment = null;
        _scriptWriter = null;
    }

    @Override
    public final HttpServletRequest getRequest() {
        return _request;
    }

    @Override
    public final HttpServletResponse getResponse() {
        return _response;
    }

    @Override
    public final MessageMap getMessages() {
        if (_messages == null) {
            return new BaseMessageMap(java.util.Locale.getDefault(), java.util.TimeZone.getDefault(), new java.util.Properties());
        }
        return _messages;
    }

    @Override
    public final MessageMap getConstants() {
        if (_constants == null) {
            return new BaseMessageMap(java.util.Locale.getDefault(), java.util.TimeZone.getDefault(), new java.util.Properties());
        }
        return _constants;
    }

    @Override
    public final void setComponentEnvironment(ComponentEnvironment scriptEnvironment) {
        _scriptEnvironment = scriptEnvironment;
    }

    @Override
    public final ComponentEnvironment getComponentEnvironment() {
        return _scriptEnvironment;
    }

    @Override
    public final HtmlWriter setHtmlWriter(HtmlWriter scriptWriter) {
        HtmlWriter old = _scriptWriter;
        _scriptWriter = scriptWriter;
        return old;
    }

    @Override
    public final HtmlWriter getHtmlWriter() {
        return _scriptWriter;
    }

    private static ThreadLocal<WebContext> __tls = new ThreadLocal<WebContext>() {
        protected synchronized WebContext initialValue() {
            return new BaseWebContext();
        }
    };

    public static WebContext getContext() {
        return __tls.get();
    }

    public static void setContext(WebContext wc) {
        if (wc == null) {
            __tls.remove();
        } else {
            __tls.set(wc);
        }
    }

    public static WebContext copy(WebContext wc) { // very grue
        BaseWebContext res = new BaseWebContext();
        res.init(wc.getRequest(), wc.getResponse());
        res.initMessages(wc.getMessages());
        res.initConstants(wc.getConstants());
        res.setComponentEnvironment(wc.getComponentEnvironment());
        res.setHtmlWriter(wc.getHtmlWriter());
        return res;
    }

    private static ServletContext __context;
    private static boolean __debug;

    private static final String LOI_CONFIG_DEBUG = "debug";

    public static void staticInit(ServletContext sc, Boolean debug) {
        __context = sc;
        __debug = debug;
    }

    public static ServletContext getServletContext() {
        return __context;
    }

    public static boolean getDebug() {
        return __debug;
    }
}
