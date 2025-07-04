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

package com.learningobjects.cpxp.filter;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.InternationalizationUtils;
import com.learningobjects.cpxp.util.message.BaseMessageMap;
import com.learningobjects.cpxp.util.message.MessageMap;
import com.learningobjects.cpxp.util.message.MessageMapCompositor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message filter. Adds a map to the request attributes under the name
 * "msg" that maps resource bundle keys to the appropriate value. Also
 * maps "constant" to the constant bundle.
 *
 * I do this rather than a proper ResourceBundle class because that
 * winds up being a hack since I can't tell what Locale I was
 * instantiated under since one bundle serves all requests, so my
 * handleGetObject() method ends up having to call
 * FacesUtil.getCurrentLocale() for every message etc. etc. And I also
 * can't get a handle on the appropriate web svcs without a filter
 * initializing me, etc.
 *
 * The default messages are overriden by the domain messages in
 * the current filter when the user's domain is known.
 *
 * @see CurrentFilter
 */
public class MessageFilter extends AbstractFilter {
    private static final Logger logger = Logger.getLogger(MessageFilter.class.getName());

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    public static final String REQUEST_ATTRIBUTE_MSG = "msg";
    public static final String REQUEST_ATTRIBUTE_CONSTANT = "constant";
    public static final String REQUEST_ATTRIBUTE_MSG_VER = "msgVer";

    private MessageMap _constants;
    private MessageMapCompositor _messages;
    private TimeZone _timeZone;

    public MessageFilter() {
        super();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        initConstants();
        initMessages();
        _timeZone = TimeZone.getDefault();
    }

    // TODO: Reconfig on change

    protected void filterImpl(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Locale locale = CurrentFilter.getRequestLocale(request);
        logger.log(Level.FINE, "Request locale, {0}", locale);
        MessageMap messages = _messages.getCompositeMap(locale);
        logger.log(Level.FINE, "Request messages, {0}", messages);
        BaseWebContext.getContext().initMessages(messages);
        request.setAttribute(REQUEST_ATTRIBUTE_MSG, messages);
        request.setAttribute(REQUEST_ATTRIBUTE_CONSTANT, _constants);
        BaseWebContext.getContext().initConstants(_constants);
        request.setAttribute(REQUEST_ATTRIBUTE_MSG_VER, 0L);
        response.setLocale(messages.getLocale());
        chain.doFilter(request, response);
    }

    private void initConstants() {
        try {
            Properties properties = ClassUtils.loadResourceAsProperties(ClassUtils.class, "/messages/Constants.properties");
            _constants = new BaseMessageMap(new Locale("constant"), _timeZone, properties);
        } catch (Exception ex) {
            throw new RuntimeException("Constants error", ex);
        }
    }

    /**
     * This initializes the default internationalizations that ship with
     * the product. These are only used for rendering domain-less pages:
     * 1. The domain initialization page
     * 2. Errors that occurs before the current domain is set
     */
    private void initMessages() {
        _messages = InternationalizationUtils.defaultMessages(_timeZone);
    }
}
