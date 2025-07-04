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

package com.learningobjects.cpxp.service.session;

import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;

public abstract class SessionSupport {
    // Even session ids are real, odd are transient
    public static final int SESSION_ID_LENGTH = 20;

    public static boolean isSessionId(String id) {
        return !StringUtils.isEmpty(id) && (id.length() == SESSION_ID_LENGTH);
    }

    public static boolean isPersistentId(String id) {
        return isSessionId(id) && ((id.charAt(id.length() - 1) & 1) == 0);
    }

    public static boolean isTransientId(String id) {
        return !isSessionId(id) || ((id.charAt(id.length() - 1) & 1) == 1);
    }

    public static String getTransientId() {
        return getSessionId(1);
    }

    public static String getPersistentId() {
        return getSessionId(0);
    }

    private static String getSessionId(int x) {
        String id;
        do { // meh, 50/50 chance..
            id = NumberUtils.getBase62String(SESSION_ID_LENGTH);
        } while ((id.charAt(id.length() - 1) & 1) != x);
        return id;
    }
}
