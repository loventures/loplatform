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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class StandardLogFormatter extends Formatter {
    private static final String   RFC_3339_DATE_FORMAT =
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private final SimpleDateFormat __dateFormatter =
        new SimpleDateFormat(RFC_3339_DATE_FORMAT);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]" + LINE_SEPARATOR;

    private static final char FIELD_SEPARATOR = '|';
    private static final char NVPAIR_SEPARATOR = ';';
    private static final char NV_SEPARATOR = '=';

    private final Date _date = new Date();

    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder(RECORD_BEGIN_MARKER);

        _date.setTime(record.getMillis());
        sb.append(__dateFormatter.format(_date));
        sb.append(FIELD_SEPARATOR);

        sb.append(record.getLevel()).append(FIELD_SEPARATOR);

        sb.append(record.getThreadID()).append(NVPAIR_SEPARATOR);
        sb.append(Thread.currentThread().getName()).append(FIELD_SEPARATOR);

        sb.append(StringUtils.removeStart(record.getLoggerName(), "com.learningobjects.")).append(FIELD_SEPARATOR);

        sb.append(formatMessage(record));

        if (record.getThrown() != null) {
            for (String str : ExceptionUtils.getRootCauseStackTrace(record.getThrown())) {
                sb.append(LINE_SEPARATOR).append(str);
            }
        }
        sb.append(RECORD_END_MARKER);
        return sb.toString();
    }

}
