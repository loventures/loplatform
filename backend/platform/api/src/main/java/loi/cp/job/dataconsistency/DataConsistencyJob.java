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
import com.learningobjects.cpxp.component.annotation.Schema;
import loi.cp.job.EmailJob;

import java.util.List;
import java.util.Map;

/**
 * Boiler plate Interface for Data Consistency Jobs.
 */
@Schema("abstractDataConsistencyJobComponent")
public interface DataConsistencyJob<J extends DataConsistencyJob<J>> extends EmailJob<J> {

    @JsonProperty
    <C extends DataConsistencyJobConfig> C getConfig();

    /** List of update statements, grouped by ITEM_TYPE. */
    Map<String, List<String>> getQueryInfoMap();
}
