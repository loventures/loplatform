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

package loi.cp.content

import java.time.temporal.ChronoUnit.{DAYS, MINUTES}
import java.time.{Instant, ZonedDateTime}

import loi.cp.asset.edge.{DueDateGateEdgeData, GateEdgeData}
import loi.cp.reference.EdgePath
import scalaz.std.anyVal.*
import scalaz.syntax.order.*
import scalaz.syntax.std.option.*
import scaloi.misc.InstantInstances.*
import scaloi.syntax.option.*

/** Divines course content dates.
  *
  * The implementation is fully general although authoring imposes the following constraints.
  *
  * Only modules get a gate date and it is with respect to the course start date, index 0, wound back to 0:00.
  *
  * Only contents get a due date, not lessons, and it is offset from the module (or course for top-level discussions)
  * date plus 23 hours 59 minutes.
  */
object ContentDateUtils:

  /** Given a content tree and start date, return the configured gate dates and due dates of all content for which an
    * override or authored setting is provided.
    *
    * The start date would typically be the course start date wound forward to midnight on the next morning, but in a
    * self-study course it could be otherwise.
    *
    * @param contents
    *   the course content tree
    * @param start
    *   the start date for interpreting date offsets
    * @return
    *   the content and due dates
    */
  def contentDates(contents: ContentTree, start: ZonedDateTime): ContentDates =

    /** Fold over a tree accumulating gate and due date tuples. */
    def loop(
      tree: ContentTree,
      baseDate: ZonedDateTime,
      maxDate: ZonedDateTime,
      gateDates: List[(EdgePath, Instant)],
      dueDates: List[(EdgePath, DueDate)]
    ): (List[(EdgePath, Instant)], List[(EdgePath, DueDate)]) =
      val GateTriple(newBase, newMax, gateDate) = contentGate(tree.rootLabel, baseDate, maxDate)
      val dueDate                               = contentDue(tree.rootLabel, newBase)
      val edgePath                              = tree.rootLabel.edgePath
      val newGateDates                          = gateDate.cata(d => edgePath -> d.toInstant :: gateDates, gateDates)
      val newDueDates                           = dueDate.cata(d => edgePath -> DueDate(d.toInstant) :: dueDates, dueDates)
      tree.subForest.foldLeft((newGateDates, newDueDates)) { case ((gates, dues), subTree) =>
        loop(subTree, newBase, newMax, gates, dues)
      }
    end loop
    val effStart              = effectiveStartDate(start)
    val (gateDates, dueDates) = loop(contents, effStart, effStart, List.empty, List.empty)
    ContentDates(gateDates.toMap, dueDates.toMap)
  end contentDates

  /** Get the next date in a map of content dates.
    * @param dates
    *   the computed content dates
    * @param now
    *   the current time
    * @return
    *   the next date in the map, or [[None]]
    */
  def nextDate(dates: Map[EdgePath, Instant], now: Instant): Option[Instant] =
    dates.values.foldLeft(Option.empty[Instant]) { case (next, here) =>
      if here.isAfter(now) then Some(next `min` here) else next
    }

  /** The effective start date is considered to be the start date with hours, minutes, seconds and milliseconds rewound
    * to zero.
    */
  private def effectiveStartDate(start: ZonedDateTime): ZonedDateTime =
    start.truncatedTo(DAYS)

  /** Given a piece of content, the nearest inherited gate date (used for offsets) and the maximum inherited gate date
    * (used to determine if a descendant gate date is shadowed), compute the new nearest and maximum gate dates and the
    * actual content gate date.
    */
  private def contentGate(content: CourseContent, baseDate: ZonedDateTime, maxDate: ZonedDateTime): GateTriple =
    val gateDate    = content.overlay.gateDate
      .map(_.atZone(baseDate.getZone))
      .orElse(edgeGateOffset(content).map(dayOffset => baseDate.plus(dayOffset, DAYS)))
    val visibleDate = gateDate.filter(_ >= maxDate)
    GateTriple(gateDate.cata(effectiveStartDate, baseDate), visibleDate | maxDate, visibleDate)

  /** Return the due date of a piece of content, considering the overlay and the authored offset with the gating parent
    * start dates.
    */
  private def contentDue(content: CourseContent, baseDate: ZonedDateTime): Option[ZonedDateTime] =
    /* add one day less one minute to the due date offset, because we expect
     * that "available on Wednesday" means "... Wednesday morning", but
     * "due on Wednesday" means "... Wednesday night". Offset of 0 is same day.
     */
    content.overlay.dueDate
      .map(_.atZone(baseDate.getZone))
      .orElse(dueDateOffset(content).map(dayOffset => baseDate.plus(dayOffset + 1, DAYS).minus(1, MINUTES)))

  /** Get the authored edge gate offset. */
  def edgeGateOffset(content: CourseContent): Option[Long] =
    content.edgeData.get[GateEdgeData].nzMap(_.offset)

  /** Get the authored due gate offset. */
  def dueDateOffset(content: CourseContent): Option[Long] =
    content.edgeData.get[DueDateGateEdgeData].flatMap(_.dueDateDayOffset)
end ContentDateUtils

/** Encapsulates content gate and due dates.
  *
  * @param gateDates
  *   the date a certain piece of content is available to learners, if [[None]] the content is always available
  * @param dueDates
  *   the date a certain piece of content is due, if [[None]] the content is never overdue
  */
final case class ContentDates(gateDates: Map[EdgePath, Instant], dueDates: Map[EdgePath, DueDate]):

  /** Get the gate date for a specific piece of content, or [[None]]. */
  def gateDate(edgePath: EdgePath): Option[Instant] = gateDates.get(edgePath)

  /** Get the due date for a specific piece of content, or [[None]]. */
  def dueDate(edgePath: EdgePath): Option[DueDate] = dueDates.get(edgePath)

object ContentDates:

  /** No content dates. */
  final val empty: ContentDates = ContentDates(Map.empty, Map.empty)

private final case class GateTriple(base: ZonedDateTime, max: ZonedDateTime, date: Option[ZonedDateTime])
