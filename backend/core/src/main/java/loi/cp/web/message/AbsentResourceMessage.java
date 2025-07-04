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

package loi.cp.web.message;

import loi.cp.i18n.BundleMessage;
import loi.cp.i18n.CoreBundle;

import java.util.function.Function;

/**
 * The message that SRS sends back if an Optional resource is absent/empty
 */
public final class AbsentResourceMessage implements Function<String, BundleMessage> {

    @Override
    public BundleMessage apply(String url) {
        return CoreBundle.SRS_MESSAGES.message("resource.absent", url);
    }
}
