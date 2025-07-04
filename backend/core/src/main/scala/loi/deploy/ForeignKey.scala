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

package loi.deploy

case class ForeignKey(
  table: String,
  column: String,
  refTable: String,
  refColumn: String
):
  val alterSql = s"ALTER TABLE IF EXISTS $table ADD FOREIGN KEY ($column) REFERENCES $refTable($refColumn);"

object ForeignKey:

  lazy val keys: Set[ForeignKey] = Set(
    ForeignKey("quizattempt", "attemptfolder_id", "quizattemptfolder", "id")
  )
