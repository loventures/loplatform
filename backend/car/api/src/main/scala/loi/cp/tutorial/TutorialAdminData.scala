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

package loi.cp.tutorial

import argonaut.{CodecJson, Json}
import loi.cp.storage.Storeable
import scaloi.json.ArgoExtras

// this is all slapdash. the tutorials should be part of the code not stored for quick updating
// being part of the code would be a better tutorial experience
final case class TutorialAdminData(tutorials: Json)

object TutorialAdminData:
  // same JSON key as TutorialUserData but slapdash
  implicit val codecJson: CodecJson[TutorialAdminData] =
    CodecJson.casecodec1(TutorialAdminData.apply, ArgoExtras.unapply1)("tutorials")

  implicit val tutorialAdminDataSotreable: Storeable[TutorialAdminData] =
    Storeable.instance("tutorials")(TutorialAdminData(Json.jEmptyObject))
