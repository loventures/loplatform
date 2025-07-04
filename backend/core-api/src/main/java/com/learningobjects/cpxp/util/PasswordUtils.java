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

package com.learningobjects.cpxp.util;

import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.user.UserConstants;

public final class PasswordUtils {
    /**
     * Encode a password. Salt is DomainID + "/" + userName. SHA-1 digests salt
     * + \u0000 + password, returns salt + ':' + digested password.
     *
     * @param password the passphrase for the password
     * @return the encoded password
     */
    public static String encodePassword(DomainDTO domain, String userName, String password) {
        return encodePassword(domain.getDomainId(), userName, password);
    }

    public static String encodePassword(String domainId, String userName, String password) {
        String salt = ObjectUtils.getFirstNonNullIn(domainId, "") + "/" + userName;
        String digested = digest(salt, password);
        return salt + ':' + digested;
    }

    /**
     * Digest a password and its salt.
     *
     * @param salt
     *            the salt
     * @param password
     *            the password or null
     *
     * @return the hex-encoded digest
     */
    public static String digest(String salt, String password) {
        String saltedPassword = salt + ((password != null) ? "\u0000" + password : "");
        return DigestUtils.sha1Hex(saltedPassword);
    }
}
