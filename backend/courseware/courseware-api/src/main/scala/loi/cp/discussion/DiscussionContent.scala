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

package loi.cp.discussion

import loi.asset.discussion.model.*
import loi.authoring.asset.Asset

sealed trait DiscussionContent[T]:

  /** @return
    *   the authored content
    */
  def asset: Asset[T]

  def title: String

object DiscussionContent:

  def apply(asset: Asset[?]): Option[DiscussionContent[?]] =
    PartialFunction.condOpt(asset) { case Discussion1.Asset(discussion1) =>
      Discussion1Content(discussion1)
    }

  def unapply(asset: Asset[?]): Option[DiscussionContent[?]] = apply(asset) // sigh

case class Discussion1Content(asset: Asset[Discussion1]) extends DiscussionContent[Discussion1]:
  override def title: String = asset.data.title
