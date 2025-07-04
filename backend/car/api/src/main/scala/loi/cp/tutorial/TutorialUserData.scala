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

import argonaut.CodecJson
import enumeratum.{ArgonautEnum, Enum, EnumEntry}
import loi.cp.storage.Storeable
import scaloi.json.ArgoExtras

final case class TutorialUserData(tutorials: Map[String, TutorialInfo])

object TutorialUserData:
  implicit val codecJson: CodecJson[TutorialUserData] =
    CodecJson.casecodec1(TutorialUserData.apply, ArgoExtras.unapply1)("tutorials")

  implicit val tutorialUserDataStoreable: Storeable[TutorialUserData] =
    Storeable.instance("tutorials")(TutorialUserData(Map.empty))

final case class TutorialInfo(status: TutorialStatus)

object TutorialInfo:
  implicit val codeJson: CodecJson[TutorialInfo] =
    CodecJson.casecodec1(TutorialInfo.apply, ArgoExtras.unapply1)("status")

sealed abstract class TutorialStatus extends EnumEntry

object TutorialStatus extends Enum[TutorialStatus] with ArgonautEnum[TutorialStatus]:
  val values: IndexedSeq[TutorialStatus] = findValues

  case object Complete extends TutorialStatus
