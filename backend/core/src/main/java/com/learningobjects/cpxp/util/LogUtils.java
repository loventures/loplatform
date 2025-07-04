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

import com.learningobjects.cpxp.BaseServiceMeta;
import org.slf4j.Logger;

public class LogUtils {
    /**
     * Logs a message meant to be visible in the production logs but not the default local development logs.
     * If local, maps to DEBUG, else INFO. Previously these were captured in java.util.logging.Level.CONFIG, which does
     * not have an equivalent unique mapping in SLF4J.
     */
    public static void prod(Logger logger, String message) {
        if (BaseServiceMeta.getServiceMeta().isLocal()) {
            logger.debug(message);
        } else {
            logger.info(message);
        }
    }
}
