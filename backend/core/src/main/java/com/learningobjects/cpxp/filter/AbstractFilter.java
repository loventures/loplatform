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

import com.learningobjects.cpxp.servlet.AbstractServlet;
import com.learningobjects.cpxp.util.ManagedUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for filter implementations.
 */
public abstract class AbstractFilter extends HttpFilter {
    private static final Logger logger = Logger.getLogger(AbstractFilter.class.getName());

    public AbstractFilter() {
        ManagedUtils.di(this, false);
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response,
                            FilterChain chain) throws IOException, ServletException {
        try {
            filterImpl(request, response, chain);
        } catch (Throwable th) {
            doException(th, request, response);
        }
    }

    protected void doException(Throwable th, HttpServletRequest request, HttpServletResponse response) {
        try {
            AbstractServlet.doException(th, request, response, logger);
        } catch (Throwable th2) {
            logger.log(Level.WARNING, "Suppressed error", th);
            logger.log(Level.WARNING, "Error handling error", th2);
        }
    }

    protected abstract void filterImpl(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws Exception;

}
