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

package loi.cp.gradebook.csv;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.Writer;

public class CsvWriter { // 3rd party CSVWriter API is messy
    private final Writer _writer;
    private boolean _written;

    public CsvWriter(Writer writer) {
        _writer = writer;
    }

    public void write(String cell) throws IOException {
        if (!_written) {
            _written = true;
        } else {
            _writer.write(",");
        }
        _writer.write(StringEscapeUtils.escapeCsv((cell == null) ? "" : cell));
    }

    public void crlf() throws IOException {
        _written = false;
        _writer.write("\r\n");
    }
}
