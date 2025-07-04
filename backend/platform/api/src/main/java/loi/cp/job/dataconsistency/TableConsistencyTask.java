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

package loi.cp.job.dataconsistency;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableConsistencyTask {

    /*
    Item type is preferred, but unavailable for certain tables (Data, Item, etc).
    Table name accepted when itemType is unavailable.
     */
    @JsonProperty
    public String itemTypeOrTableName;

    @JsonProperty
    public boolean finished = false;

    @JsonProperty
    public Long lastProcessedId = -1L;

    @JsonProperty
    public String sql;
}
