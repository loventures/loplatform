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

package loi.cp.job

import java.lang.Long as jLong
import java.text.ParseException
import java.util.{Date, TimeZone}
import scala.annotation.nowarn
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{HtmlResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentSupport}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.job.RunFinder.*
import com.learningobjects.cpxp.service.query.Comparison
import com.learningobjects.cpxp.util.ManagedUtils
import de.tomcat.juli.LogMeta
import loi.apm.Apm
import loi.cp.appevent.{AppEvent, AppEventService, OnEvent}
import loi.cp.job.JobDataModels.*
import org.apache.commons.lang3.exception.ExceptionUtils
import org.quartz.CronExpression
import scalaz.NotNothing
import scalaz.syntax.std.boolean.*
import scaloi.syntax.AnyOps.*
import scaloi.syntax.classTag.classTagClass

import scala.language.dynamics
import scala.reflect.ClassTag
import scala.util.control.NonFatal

abstract class AbstractJob[J <: Job[J]] extends Job[J] with JobObject with ComponentImplementation:
  this: J =>

  val self: JobFacade

  implicit val fs: FacadeService

  protected val logger: org.log4s.Logger

  protected class JobAttributes extends Dynamic:
    def selectDynamic[T: ClassTag: NotNothing](field: String): T = self.getAttribute(field, classTagClass[T])
    def updateDynamic[T](field: String)(value: T): Unit          = self.setAttribute(field, value)

  /** Dynamic access to "attributes" of the `self` facade. Stored as fields in the `json` blob. */
  protected def attributes: JobAttributes = new JobAttributes

  override def delete(): Unit = self.delete()

  override def update(job: J): J =
    try if !Job.Manual.equals(job.getSchedule) then CronExpression.validateExpression(job.getSchedule)
    catch
      case e: ParseException =>
        throw new ValidationException("schedule", job.getSchedule, e.getMessage)
    self.setName(job.getName)
    self.setSchedule(job.getSchedule)
    self.setDisabled(job.isDisabled)

    reschedule()

    this
  end update

  @PostCreate
  private def create(job: J): J =
    update(job)

  override def getId: jLong =
    getComponentInstance.getId // self is null on adminUI calls

  override def getName: String = self.getName

  override def getScheduled: Option[Date] =
    Option(service[AppEventService].getNextEventTime(this, this, classOf[JobEvent]))

  override def getSchedule: String = self.getSchedule

  override def isDisabled: Boolean = self.getDisabled.booleanValue

  override def isManual: Boolean = self.getSchedule == Job.Manual

  override def getCurrentRun: Option[Run] =
    self.queryRuns
      .addCondition(DATA_TYPE_RUN_STOP_TIME, Comparison.eq, null)
      .addCondition(DATA_TYPE_RUN_SUCCESS, Comparison.eq, null)
      .setCacheQuery(false)
      .getComponents[Run]
      .headOption

  override def getRun(id: Long): Option[Run] =
    self.getRun(id)

  override def getRuns(q: ApiQuery): ApiQueryResults[Run] =
    ApiQueries.query[Run](self.queryRuns, q)

  override def execute(): Run =
    getCurrentRun match
      case Some(run) => run <| { job => logger.info(s"Job ${job.getId} is already running") }
      case None      =>
        self.addComponent[Run](classOf[Run], null) <| { run =>
          import argonaut.*
          import Argonaut.*
          ManagedUtils.commit()
          Apm.setTransactionName("job", getComponentInstance.getComponent.getIdentifier)
          Apm.addCustomParameter("jobId", self.getId)
          LogMeta.let("job" := self.getId, "run" := run.getId) {
            try execute(run)
            catch
              case NonFatal(e) =>
                logger.warn(e)("Scheduled job error")
                run.failed(ExceptionUtils.getStackTrace(e));
          }
        }

  override def renderAdminUI(): WebResponse =
    HtmlResponse.apply(this, adminTemplate)

  protected def adminTemplate: String =
    s"${ComponentSupport.getSchemaName(this)}.html"

  /** Execute this job within the specified run context. */
  protected def execute(run: Run): Unit

  /** Invoked by the app event framework. Returns when to next schedule the event. */
  @OnEvent
  private def onJob(@nowarn event: JobEvent): Option[Date] =
    schedulable option {
      execute()
      nextTime
    }

  /** Schedule or unschedule an appevent for this job. */
  private def reschedule(): Unit =
    service[AppEventService].deleteEvents(this, this, classOf[JobEvent])
    if schedulable then service[AppEventService].scheduleEvent(nextTime, this, this, new JobEvent)

  private def schedulable = !isDisabled && !isManual

  /** Determine the next runtime for this job. */
  private def nextTime: Date =
    val cron = new CronExpression(getSchedule)
    cron.setTimeZone(TimeZone.getTimeZone(domainTimeZone))
    cron.getTimeAfter(new Date)

  private def domainTimeZone = Option(Current.getDomainDTO.timeZone)
    .filterNot(_.isEmpty)
    .getOrElse("US/Eastern")
end AbstractJob

class JobEvent extends AppEvent
