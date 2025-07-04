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

package loi.cp.job

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.service.job.JobFinder.*
import com.learningobjects.cpxp.service.job.RunFinder.*

/** Data model evidence for the various job components.
  */
object JobDataModels:

  /** Job component data model evidence.
    */
  implicit val JobDataModel: DataModel[Job[?]] =
    DataModel(ITEM_TYPE_JOB, singleton = false, schemaMapped = true, Map(Job.NameProperty -> DATA_TYPE_JOB_NAME))

  /** Run component data model evidence.
    */
  implicit val RunDataModel: DataModel[Run] = DataModel(
    ITEM_TYPE_RUN,
    singleton = true,
    schemaMapped = false,
    Map(
      Run.StartTimeProperty -> DATA_TYPE_RUN_START_TIME,
      Run.EndTimeProperty   -> DATA_TYPE_RUN_STOP_TIME,
      Run.SuccessProperty   -> DATA_TYPE_RUN_SUCCESS
    )
  )
end JobDataModels
