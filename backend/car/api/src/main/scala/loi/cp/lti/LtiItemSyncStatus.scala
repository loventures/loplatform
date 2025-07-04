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

package loi.cp.lti

import argonaut.*
import argonaut.Argonaut.*
import org.apache.commons.lang3.exception.ExceptionUtils
import scalaz.NonEmptyList
import scaloi.json.ArgoExtras.*

import java.time.Instant

sealed trait LtiItemSyncStatus[+A]

object LtiItemSyncStatus:

  /** This grade is queued up & ready to be sent
    */
  case class Queued(time: Instant) extends LtiItemSyncStatus[Nothing]

  /** This item was attempted to be synced, but ultimately unsuccessful
    */
  case class Attempted(time: Instant, error: LtiItemSyncError) extends LtiItemSyncStatus[Nothing]
  object Attempted:
    def apply(time: Instant, e: Throwable): Attempted =
      new Attempted(time, InternalError(ExceptionUtils.getStackTrace(e)))

  /** This grade has been sent through the message bus and has failed to send.
    *
    * @param attempt
    *   The time that the failure happened
    */
  case class Failed(attempt: Instant, error: LtiItemSyncError) extends LtiItemSyncStatus[Nothing]
  object Failed:
    def apply(time: Instant, e: Throwable): Attempted =
      new Attempted(time, InternalError(ExceptionUtils.getStackTrace(e)))

  /** This grade was successfully synced & should exist in the consumer's gradebook.
    *
    * @param modified
    *   the time of the modification that necessitated this change
    * @param time
    *   the time that the last successful message was sent (This is usually a second or two after [[modified]], but can
    *   be longer if a sync failed and then was re-attempted).
    * @param syncedValue
    *   The last successful grade that was sent.
    */
  case class Synced[A](modified: Instant, time: Instant, syncedValue: A) extends LtiItemSyncStatus[A]

  case class Deleted(time: Instant) extends LtiItemSyncStatus[Nothing]

  implicit def itemSyncDecodeJson[A: DecodeJson]: DecodeJson[LtiItemSyncStatus[A]] = DecodeJson[LtiItemSyncStatus[A]] {
    hc =>
      hc.as[Json] match
        case DecodeResult(Right(obj)) if obj.isObject =>
          obj.field("type").flatMap(_.string) match
            case Some("Queued")    => hc.as[Queued](using DecodeJson.derive[Queued]).widen
            case Some("Attempted") => hc.as[Attempted](using DecodeJson.derive[Attempted]).widen
            case Some("Failed")    => hc.as[Failed](using DecodeJson.derive[Failed]).widen
            case Some("Synced")    => hc.as[Synced[A]](using DecodeJson.derive[Synced[A]]).widen
            case Some("Deleted")   => hc.as[Deleted](using DecodeJson.derive[Deleted]).widen
            case Some(tpe)         => DecodeResult.fail(s"Unknown type ${tpe}", hc.history)
            case None              => DecodeResult.fail("Cannot determine import item type as it has no `_type` field", hc.history)
        case DecodeResult(Right(_))                   =>
          DecodeResult.fail("ImportItems should be objects", hc.history)
        case DecodeResult(Left(err))                  =>
          DecodeResult.fail(err._1, err._2)
  }

  implicit def itemSyncEncodeJson[A: EncodeJson]: EncodeJson[LtiItemSyncStatus[A]] = EncodeJson[LtiItemSyncStatus[A]] {
    case queued: Queued               => ("type" := "Queued") ->: queued.asJson(using EncodeJson.derive[Queued])
    case attempted: Attempted         => ("type" := "Attempted") ->: attempted.asJson(using EncodeJson.derive[Attempted])
    case failed: Failed               => ("type" := "Failed") ->: failed.asJson(using EncodeJson.derive[Failed])
    case deleted: Deleted             => ("type" := "Deleted") ->: deleted.asJson(using EncodeJson.derive[Deleted])
    case synced: Synced[A] @unchecked => ("type" := "Synced") ->: synced.asJson(using EncodeJson.derive[Synced[A]])
  }

  type LtiItemSyncStatusHistory[A] = NonEmptyList[LtiItemSyncStatus[A]]

  private val logger = org.log4s.getLogger

  implicit class LtiItemSyncStatusHistoryOps[A](val history: LtiItemSyncStatusHistory[A]) extends AnyVal:
    def isSynced: Boolean = current match
      case Synced(_, _, _) => true
      case _               => false

    def failed: Boolean = current match
      case Failed(_, _) => true
      case _            => false

    def current: LtiItemSyncStatus[A] = history.last

    def lastValid: Option[Synced[A]] = history.reverse.stream.collectFirst({ case s: Synced[A] @unchecked => s })

    def isUpToDate(t: Instant): Boolean =
      current match
        case Synced(lastSentTime, _, _) =>
          (lastSentTime `isAfter` t) || (lastSentTime `equals` t)
        case _                          => false
  end LtiItemSyncStatusHistoryOps
end LtiItemSyncStatus
