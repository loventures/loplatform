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

package loi.asset.competency.service

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.competency.model.CompetencySet
import loi.authoring.asset.Asset
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}

import java.util.UUID

/** @author
  *   sjordan
  */
@Service
trait CompetencyService:

  /** Finds the competency sets that contain `assets`. For example, the parents of the lvl1s, the grand parents of the
    * lvl2s, and the great grand parents of the lvl3s.
    *
    * @param ws
    *   the workspace that contains `assets`
    * @param assets
    *   the assets, if no competency assets in in this seq, then the return value is empty
    * @return
    *   the competency sets that contain `assets`
    */
  def findCompetencySets(ws: ReadWorkspace, assets: Seq[Asset[?]]): Seq[Asset[CompetencySet]]

  /** Finds the competency parents for `competency`. For example, returns the lvl2s that use a lvl3. Always returns an
    * empty seq for a lvl1 competency. Returns empty seq for non-competency types.
    * @param ws
    *   the workspace that contains `competency` and its parents
    * @param competency
    *   the competency whose parents are searched for
    * @return
    *   the competency parents for `competency`.
    */
  def findParents(ws: ReadWorkspace, competency: Asset[?]): Seq[Asset[?]]

  /** Returns a map from [[CompetencyService.normalize]] competency names to UUIDs for all non-archived competencies of
    * non-archived competency sets in the branch program.
    */
  def getCompetenciesByName(ws: AttachedReadWorkspace): Map[String, UUID]
end CompetencyService

object CompetencyService:

  /** Removes any prefix up to | (as seen in some QTI), then strips all non-alphabetic characters from, and lower-cases,
    * a competency name, for maximum chance of lookup success.
    */
  def normalize(name: String): String = name.replaceAll("^.*\\|", "").replaceAll("[^a-zA-Z]", "").toLowerCase
