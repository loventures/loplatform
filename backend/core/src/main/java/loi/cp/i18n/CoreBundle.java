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

import java.util.Arrays;
import java.util.Collection;

public class CoreBundle{
    public static final ResourceBundleLoader JSON_PATCH_MESSAGES = new ResourceBundleLoader("loi.cp.i18n.JsonPatchMessages");
    public static final ResourceBundleLoader SRS_MESSAGES = new ResourceBundleLoader("loi.cp.i18n.SrsMessages");
    public static final Collection<ResourceBundleLoader> ALL_CORE_MESSAGES = Arrays.asList(JSON_PATCH_MESSAGES, SRS_MESSAGES);
}
