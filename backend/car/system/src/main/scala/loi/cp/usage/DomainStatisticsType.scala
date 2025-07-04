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

package loi.cp.usage

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import enumeratum.*

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable

sealed trait DomainStatisticsType extends EnumEntry

object DomainStatisticsType extends Enum[DomainStatisticsType]:

  case object SessionStarts   extends DomainStatisticsType
  case object PageNavigations extends DomainStatisticsType
  case object DistinctUsers   extends DomainStatisticsType

  def counter(dt: DomainStatisticsType): StatCounter = dt match
    case SessionStarts   =>
      SimpleCounter(json =>
        json.at(eventType).textValue() == "SessionEvent" &&
          json.at(actionType).textValue() == "start"
      )
    case PageNavigations => SimpleCounter(isPageNavEvent)
    case DistinctUsers   => DistinctUserCounter()

  def sum(counters: Iterable[StatCounter]): Long =
    counters.headOption match
      case Some(_: DistinctUserCounter) =>
        counters
          .map(_.asInstanceOf[DistinctUserCounter].users)
          .reduce(_ ++ _)
          .size
      case Some(_: SimpleCounter)       =>
        counters.map(_.currentTotal).sum
      case None                         => 0

  override def values: IndexedSeq[DomainStatisticsType] = findValues

  sealed trait StatCounter:
    def process(json: JsonNode): Unit
    def currentTotal: Long

  private case class SimpleCounter(
    private val pred: JsonNode => Boolean
  ) extends StatCounter:
    private var total: Long = 0

    override def process(json: JsonNode): Unit = if pred(json) then total += 1

    override def currentTotal: Long = total

  private case class DistinctUserCounter() extends StatCounter:
    private[DomainStatisticsType] val users: mutable.Set[Long] =
      mutable.Set[Long]()

    override def process(json: JsonNode): Unit =
      if isPageNavEvent(json) then
        userIdPaths
          .collectFirst {
            case p if json.at(p).isIntegralNumber => json.at(p).asLong()
          }
          .foreach(id => users += id)

    override def currentTotal: Long = users.size
  end DistinctUserCounter

  private val eventType   = JsonPointer.valueOf("/eventType")
  private val actionType  = JsonPointer.valueOf("/actionType")
  private val userIdPaths =
    List(JsonPointer.valueOf("/user/id"), JsonPointer.valueOf("/userId"))

  private val isPageNavEvent = (json: JsonNode) => json.at(eventType).textValue() == "PageNavEvent"
end DomainStatisticsType
