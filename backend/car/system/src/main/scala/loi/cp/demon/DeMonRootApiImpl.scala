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

package loi.cp.demon

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQueryUtils}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.schedule.Scheduled
import de.mon.{GlobalStatistic, GlobalStatistics, StatisticType}

import scala.concurrent.duration.*

/** DE monitoring statistics root API implementation.
  */
@Component
class DeMonRootApiImpl(val componentInstance: ComponentInstance) extends DeMonRootApi with ComponentImplementation:
  import DeMonRootApi.*
  import DeMonRootApiImpl.*

  /** Get the statistics. */
  override def getStatistics(q: ApiQuery): ApiQueryResults[DeMonStat] =
    ApiQueryUtils.query(GlobalStatistics.statistics.toSeq.map(toStat), ApiQueryUtils.propertyMap[DeMonStat](q))

  /** Reset the statistics. */
  override def resetStatistics(): Unit = GlobalStatistics.reset()

  /** Convert data to the Web POJO. */
  private def toStat(tuple: ((StatisticType, String), GlobalStatistic)): DeMonStat = tuple match
    case ((statementType, tableName), GlobalStatistic(count, NsToMs(millis), rate)) =>
      DeMonStat(
        statementType.toString,
        tableName,
        count,
        rate.toDouble / RateInterval,
        millis,
        millis.toDouble / count.toDouble
      )

  @Scheduled("5 minutes")
  def updateRate(): Unit = GlobalStatistics.updateRate()
end DeMonRootApiImpl

object DeMonRootApiImpl:

  private val RateInterval = 5 // minutes between rate updates

  abstract class Timeogrify(from: TimeUnit, to: TimeUnit):
    def unapply(v: Long): Some[Long] = Some(to.convert(v, from))

  /** Convert ns to ms. */
  private object NsToMs extends Timeogrify(NANOSECONDS, MILLISECONDS)
