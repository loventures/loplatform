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

package loi.cp.script;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

class LoggingWriter extends PrintWriter {
    private final Logger _logger;

    private StringBuffer _sb;

    private LoggingWriter(Logger logger, StringWriter sw) {
        super(sw);
        this._logger = logger;
        _sb = sw.getBuffer();
    }

    public LoggingWriter(Logger logger) {
        this(logger, new StringWriter());
    }

    @Override
    public void write(String s) {
        super.write(s);
        if (s.endsWith("\n")) {
            _logger.log(Level.INFO, _sb.toString().trim());
            _sb.setLength(0);
        }
    }
}
