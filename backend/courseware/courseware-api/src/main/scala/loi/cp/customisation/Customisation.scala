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

package loi.cp.customisation

import argonaut.{DecodeJson, EncodeJson}
import loi.cp.reference.EdgePath

/** Instructor customisation model. */
final case class Customisation(overlays: Map[EdgePath, ContentOverlay]):
  def apply(path: EdgePath): ContentOverlay = overlays.getOrElse(path, ContentOverlay.empty)

  def withOverlay(path: EdgePath, overlay: ContentOverlay): Customisation =
    copy(overlays = overlays + (path -> overlay))

object Customisation:
  final val empty: Customisation = Customisation(Map.empty)

  implicit def encodeJson: EncodeJson[Customisation] = EncodeJson.derive[Customisation]
  implicit def decodeJson: DecodeJson[Customisation] = DecodeJson.derive[Customisation]
