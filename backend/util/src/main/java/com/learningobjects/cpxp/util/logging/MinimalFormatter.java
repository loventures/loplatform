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

package com.learningobjects.cpxp.util.logging;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A minimal log formatter suitable, for example, for streaming log progress to a human.
 */
public class MinimalFormatter extends Formatter {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final String traceStop;
    private final boolean timestamp;

    /**
     * @param traceStop Strop reporting exception stack traces when this classname is found in a line.
     * @param timestamp Log timestamps.
     */
    public MinimalFormatter(Class<?> traceStop, boolean timestamp) {
        this.traceStop = traceStop.getName();
        this.timestamp = timestamp;
    }

    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        if (timestamp) {
            sb.append(ISO8601Utils.format(new Date(record.getMillis()))).append(": ");
        }

        if (record.getLevel().intValue() > Level.INFO.intValue()) {
            sb.append(record.getLevel()).append(": ");
        }

        sb.append(record.getLoggerName().replaceAll("\\$.*", "").replaceAll(".*\\.", "")).append(": ");

        sb.append(formatMessage(record));

        if (record.getThrown() != null) {
            for (String str : ExceptionUtils.getRootCauseStackTrace(record.getThrown())) {
                sb.append(LINE_SEPARATOR).append(str);
                if (str.contains(traceStop)) {
                    break;
                }
            }
        }

        sb.append(LINE_SEPARATOR);

        return sb.toString();
    }
}
