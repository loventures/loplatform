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

package loi.cp.lti.storage

import java.net.URL
import java.time.Instant
import argonaut.*
import loi.cp.lti.LtiItemSyncStatus.*
import com.learningobjects.cpxp.scala.util.URLCodec.*
import loi.cp.lti.LtiItemSyncStatus
import scalaz.IList
import scaloi.json.ArgoExtras.*
import scalaz.syntax.std.option.*
import scalaz.syntax.nel.*
import scaloi.json.ArgoExtras

/** Lti configuration of a graded asset. Specialized over using the [Assignment] parameter on the Json Graph to make it
  * faster to find.
  *
  * @param system
  *   the id of the BasicLtiSystemComponent which will contain the security configurations to send this back.
  * @param url
  *   The outcomes service url to send results to.
  * @param resultSourceDid
  *   An identifier from the consumer uniquely identifying the user and activity
  */
case class LtiCallback(
  system: Long,
  url: URL,
  resultSourceDid: String,
  statusHistory: Option[LtiItemSyncStatusHistory[Double]]
):

  def pushStatus(status: LtiItemSyncStatus[Double]): LtiCallback =
    this.copy(statusHistory = Some(this.statusHistory.cata(_ :::> IList(status), status.wrapNel)))

object LtiCallback:

  def from(system: Long, url: URL, resultSourceDid: String, time: Instant): LtiCallback =
    new LtiCallback(system, url, resultSourceDid, None)

  implicit val ltiCodec: CodecJson[LtiCallback] =
    CodecJson.casecodec4(LtiCallback.apply, ArgoExtras.unapply)("system", "url", "resultSourceDid", "status")
