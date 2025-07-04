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

package com.learningobjects.cpxp.service.dump;

import java.io.File;
import javax.ejb.Local;

import com.learningobjects.cpxp.util.TempFileMap;

/**
 * The dump service.
 */
@Local
public interface DumpService {
    public void dump(Long id, String prefix, File file, boolean includeAttachments, boolean encrypt) throws Exception;

    public void dump(Long id, String prefix, File file, boolean includeAttachments, boolean encrypt, TempFileMap files) throws Exception;

    public void restoreInto(Long itemId, String url, File file) throws Exception;

    public void restoreReplace(Long itemId, File file) throws Exception;
}
