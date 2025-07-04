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

package loi.cp.course

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

import com.learningobjects.cpxp.service.Current
import loi.cp.config.ConfigurationService
import scalaz.syntax.std.option.*

import scala.compat.java8.OptionConverters.*

trait CourseDatesImpl extends CourseComponent:
  // I want a self type but Java resists

  protected def configurationService: ConfigurationService
  protected def self: CourseFacade

  override def getStartDate: Option[Instant]                  =
    Option(self.getStartDate).map(_.toInstant)
  override def setStartDate(startDate: Option[Instant]): Unit =
    self.setStartDate(startDate.cata(Date.from, null))

  override def getEndDate: Option[Instant]                =
    Option(self.getEndDate).map(_.toInstant)
  override def setEndDate(endDate: Option[Instant]): Unit =
    self.setEndDate(endDate.cata(Date.from, null))

  override def getConfiguredShutdownDate: Option[Instant] =
    self.getShutdownDate.asScala

  override def getShutdownDate: Option[Instant] =
    getConfiguredShutdownDate.orElse {
      val off = CoursePreferences.getDomain(using configurationService).reviewPeriodOffset
      getEndDate.map(_.plus(off, ChronoUnit.HOURS))
    }

  override def setShutdownDate(shutdownDate: Option[Instant]): Unit =
    self.setShutdownDate(shutdownDate.asJava)

  override def hasCourseStarted: Boolean =
    getStartDate.forall(_ `isBefore` now())

  override def hasCourseEnded: Boolean =
    getEndDate.exists(_ `isBefore` now())

  override def hasCourseShutdown: Boolean =
    getShutdownDate.exists(_ `isBefore` now())

  private def now(): Instant =
    // so I want this to be ts.instant, but the integration tests rely on being
    // able to set the time using the X-Date header, and TimeSource doesn't do
    // that right now. Maybe it should...
    Current.getTime.toInstant
end CourseDatesImpl
