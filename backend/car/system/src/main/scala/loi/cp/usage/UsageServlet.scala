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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.Method.GET
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainFinder as DF
import com.learningobjects.cpxp.service.query.{
  BaseOrder,
  Direction,
  QueryService,
  BaseDataProjection as Proj,
  Comparison as Cmp,
  Function as Fn
}
import com.learningobjects.cpxp.usage.DomainStatsFinder as DSF
import scalaz.syntax.either.*

import java.time.ZonedDateTime
import java.time.temporal.{ChronoField, ChronoUnit}
import scala.collection.SortedMap
import scala.jdk.CollectionConverters.*

@Component
@ServletBinding(system = true, path = UsageServlet.Path)
class UsageServlet(val componentInstance: ComponentInstance, qs: QueryService)
    extends ServletComponent
    with ComponentImplementation
    with ServletDispatcher:
  import ServletDispatcher.*
  import UsageServlet.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(GET, Path, _, _)              =>
      ArgoResponse(
        Map(
          "now"       -> accessCount("1 hour"),
          "daily"     -> accessCount("1 day"),
          "monthly"   -> accessCount("30 days"),
          "quarterly" -> accessCount("91 days")
        )
      ).right
    case RequestMatcher(GET, DomainStatsPath, _, _)   =>
      EntityResponse(pastYearDomainStats).right
    case RequestMatcher(GET, DistinctUsersPath, _, _) =>
      EntityResponse(currentDistinctUsers).right
  }

  private def accessCount(interval: String): Long =
    qs.createNativeQuery(
      "SELECT COUNT(*) FROM UserHistoryFinder WHERE accessTime > now() - INTERVAL '" + interval + "'"
    ).getSingleResult
      .asInstanceOf[Number]
      .longValue

  private def pastYearDomainStats: DomainStats =
    val domains = qs
      .queryAllDomains(DF.ITEM_TYPE_DOMAIN)
      .setDataProjection(
        Proj.ofData(DataTypes.META_DATA_TYPE_ID, DF.DATA_TYPE_DOMAIN_SHORT_NAME)
      )
      .addCondition(DF.DATA_TYPE_DOMAIN_STATE, Cmp.eq, "Normal")
      .addCondition(DataTypes.DATA_TYPE_TYPE, Cmp.eq, "dump")
      .getResultList[Any]
      .asScala
      .collect { case Array(id: Number, name: String) => id.longValue() -> name }
      .toMap

    val stats: ChartData = pastYearStatsQ
      .addCondition(DataTypes.META_DATA_TYPE_ROOT_ID, Cmp.in, domains.keys)
      .addOrder(BaseOrder.byData(DSF.DATA_TYPE_TIME, Direction.ASC))
      .getFacades[DomainStatisticsFacade]
      .flatMap { fac =>
        DomainStatisticsType
          .withNameOption(fac.getType)
          .map { statType =>
            (
              statType.entryName,
              fac.getRootId.longValue(),
              fac.getTime.toEpochMilli / 1000,
              fac.getValue
            )
          }
      }
      .groupBy { case (statsType, _, _, _) => statsType }
      .view
      .mapValues { dd =>
        val domainData =
          dd.groupBy { case (_, dId, _, _) => dId }
            .view
            .mapValues {
              _.map { case (_, _, time, count) => Seq(time, count) }
            }
            .toMap
        // domain with most events come first
        SortedMap.empty[Long, Seq[Seq[Long]]](using
          (i0, i1) =>
            def sum(i: Long) = domainData(i).map(_(1)).sum
            sum(i1).compareTo(sum(i0))
        ) ++ domainData
      }
      .toMap

    DomainStats(domains, stats)
  end pastYearDomainStats

  private def currentDistinctUsers =
    val (min, max) = pastYearStatsQ
      .addCondition(DSF.DATA_TYPE_TYPE, Cmp.eq, TotalDistinctUsers)
      .setDataProjection(
        Array(
          Proj.ofAggregateData(DSF.DATA_TYPE_VALUE, Fn.MIN),
          Proj.ofAggregateData(DSF.DATA_TYPE_VALUE, Fn.MAX),
        )
      )
      .getResultList[Any]
      .asScala
      .collectFirst { case Array(min: Number, max: Number) =>
        (min.longValue, max.longValue)
      }
      // if stats job has not been run, meter will be in the middle
      .getOrElse((Long.MinValue, Long.MaxValue))

    val midnight   = ZonedDateTime.now().`with`(ChronoField.SECOND_OF_DAY, 0).toInstant
    val currentVal = qs
      .createNativeQuery(
        s"""
        |SELECT
        |  COUNT(DISTINCT JSONB_EXTRACT_PATH_TEXT(datajson, 'user', 'id'))
        |FROM analyticfinder
        |WHERE
        |  JSONB_EXTRACT_PATH_TEXT(datajson, 'eventType') = 'PageNavEvent'
        |  AND time >= '$midnight'""".stripMargin
      )
      .getSingleResult
      .asInstanceOf[Number]
      .longValue()
    CurrentUsageStat(min = math.min(min, currentVal), current = currentVal, max = math.max(max, currentVal))
  end currentDistinctUsers

  private def pastYearStatsQ =
    val end   = ZonedDateTime.now().`with`(ChronoField.SECOND_OF_DAY, 0)
    val start = end.minus(1, ChronoUnit.YEARS)

    qs.queryAllDomains(DSF.ITEM_TYPE_DAILY_DOMAIN_STATISTICS)
      .addCondition(DSF.DATA_TYPE_TIME, Cmp.ge, start.toInstant)
      .addCondition(DSF.DATA_TYPE_TIME, Cmp.lt, end.toInstant)
end UsageServlet

object UsageServlet:
  final val Path              = "/sys/usage"
  final val DomainStatsPath   = s"$Path/domain"
  final val DistinctUsersPath = s"$Path/distinctUsers"

  final val TotalDistinctUsers = "TotalDistinctUsers"

  case class DomainStats(
    domains: Map[Long, String],
    stats: ChartData
  )

  case class CurrentUsageStat(min: Long, current: Long, max: Long)

  // Map[StatsType, SortedMap[DomainId, Seq[Seq(UnixTime, Count)]]]
  type ChartData = Map[String, SortedMap[Long, Seq[Seq[Long]]]]
  // <editor-fold defaultstate="collapsed" desc="example output">
  /*
  {
    "domains": { "12345": "Domain1", "67890": "Domain2" },
    "stats": {
      "PageNavs": [
        "12345": [
          [45678988, 9877],
          [45679168, 567899],
          ...
        ]
        "67890": [...]
      ],
      "SessionStarts": [...]
    }
  }
   */
  // </editor-fold>
end UsageServlet
