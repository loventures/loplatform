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

package com.learningobjects.cpxp.service.accesscontrol;

import com.learningobjects.cpxp.service.ServiceException;
import org.apache.commons.lang3.ArrayUtils;


public class AccessControlException extends ServiceException {
    public AccessControlException(String msg) {
        super(msg);
    }

    public AccessControlException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public AccessControlException(Long id, String permission) {
        super(permission + " on item " + id);
    }

    public AccessControlException(Long id, String permission, Long userId) {
        super(permission + " on item " + id + " by user " + userId);
    }

    public AccessControlException(Long id, String[] permissions) {
        super(ArrayUtils.toString(permissions) + " on item " + id);
    }

    public AccessControlException(Long id, String[] permissions, Long userId) {
        super(ArrayUtils.toString(permissions) + " on item " + id + " by user " + userId);
    }
}
