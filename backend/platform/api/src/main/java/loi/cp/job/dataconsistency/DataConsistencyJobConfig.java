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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import loi.cp.job.JobConfig;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataConsistencyJobConfig implements JobConfig {
    @JsonProperty
    public Long timeLimitInSeconds = 1L;

    @JsonProperty
    public Long initialMaxId = -1L;

    @JsonProperty
    public boolean runForever = false;

    @JsonProperty
    public Long batchSizeLimit = 100000L;

    @JsonProperty
    public List<TableConsistencyTask> tableConsistencyTasks;
}
