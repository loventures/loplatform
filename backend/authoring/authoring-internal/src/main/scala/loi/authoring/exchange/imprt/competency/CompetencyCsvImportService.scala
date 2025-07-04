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

package loi.authoring.exchange.imprt.competency

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.de.task.TaskReport
import loi.authoring.exchange.model.ExchangeManifest

/** service used to import asset trees from a CSV
  */
@Service
trait CompetencyCsvImportService:

  /** parses a CSV into a sequence of row objects.
    *
    * @param parsed
    *   raw values parsed from the csv file
    * @param report
    *   TaskReport to attach errors to
    * @return
    *   Sequence of parsed rows if valid, 'None' if not
    */
  def parseCsv(parsed: List[Map[String, String]], report: TaskReport): Option[Seq[CompetencyCsvImportRow]]

  /** generates the ExchangeManifest from a valid list of rows
    *
    * @param rows
    *   valid list of rows
    * @return
    *   manifest to be used in asset import
    */
  def generateManifest(rows: Seq[CompetencyCsvImportRow]): ExchangeManifest
end CompetencyCsvImportService
