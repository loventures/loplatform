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

package com.learningobjects.cpxp.component.util;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.logging.*;

public class StackTraceLogFormatter extends Formatter {

    private String _lineSeparator = System.getProperty("line.separator");

    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        if (record.getLevel().intValue() > Level.INFO.intValue()) {
            sb.append(record.getLevel().getLocalizedName());
            sb.append(": ");
        }
        String message = formatMessage(record);
        sb.append(message);
        sb.append(_lineSeparator);
        Object[] params = record.getParameters();
        if (params != null && params.length > 0) {
            for (Object p: params) {
                sb.append(p.toString());
                sb.append(_lineSeparator);
            }
        }
        Throwable t = record.getThrown();
        if (t != null) {
            for (String str : ExceptionUtils.getRootCauseStackTrace(t)) {
                sb.append(str);
                sb.append(_lineSeparator);
            }
        }
        return sb.toString();
    }

    private static boolean _attached = false;

    public static void ensureAttachedToConsoleLogger() {
        ensureAttachedToConsoleLogger("");
    }

    public static synchronized void ensureAttachedToConsoleLogger(String name) {
        if (_attached) {
            return;
        }
        Logger logger = Logger.getLogger(name);
        Handler[] handlers = logger.getHandlers();
        for (int h = 0; h < handlers.length; ++ h) {
            logger.removeHandler(handlers[h]);
        }
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new StackTraceLogFormatter());
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
        Logger.getLogger("com.learningobjects").setLevel(Level.ALL);
        _attached = true;
    }

}
