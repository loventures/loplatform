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

package loi.cp.i18n;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * Validates things about resource bundles. This should be in test
 * sources, but test sources aren't included in dependencies for downstream projects
 *
 * WARNING: This depends on the current locale and so may be flaky as a test
 */
public class ResourceBundleValidator {

    /**
     * Loads the bundle and parses each message.
     *
     * @throws IllegalArgumentException if any message cannot be parsed
     */
    public static void parseAllKeys(final ResourceBundle bundle) {

        final Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final String template = bundle.getString(key);

            final Object[] args = {};

            try {
                MessageFormat.format(template, args);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(
                  e.getMessage() + " Key: '" + key + "' Bundle: '" + bundle.getBaseBundleName() + "'");
            }

        }
    }

}
