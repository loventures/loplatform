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

package loi.cp.role.impl;

import com.learningobjects.cpxp.util.FormattingUtils;
import com.learningobjects.cpxp.util.ObjectUtils;
import loi.cp.role.SupportedRoleFacade;

import java.util.Comparator;

public class SupportedRoleComparator implements Comparator<SupportedRoleFacade> {
    public int compare(SupportedRoleFacade f0, SupportedRoleFacade f1) {
        String s1 = FormattingUtils.contentName(f0.getRole());
        String s2 = FormattingUtils.contentName(f1.getRole());
        return ObjectUtils.compareIgnoreCase(s1, s2);
    }
}
