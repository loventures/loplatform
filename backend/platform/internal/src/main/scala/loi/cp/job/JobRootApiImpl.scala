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

import java.util.{Date, TimeZone}

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.ApiQueryOps
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.{
  ComponentEnvironment,
  ComponentImplementation,
  ComponentInstance,
  ComponentSupport
}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, QueryService}
import com.learningobjects.cpxp.util.ClassUtils
import loi.cp.component.ComponentRootApi
import loi.cp.job.JobDataModels.*
import loi.cp.job.JobRootApi.CronDTO
import org.quartz.CronExpression

import scala.jdk.CollectionConverters.*
import scala.compat.java8.OptionConverters.*
import scalaz.\/
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.any.*
import scaloi.syntax.option.*

@Component
class JobRootApiImpl(
  val componentInstance: ComponentInstance,
  componentEnvironment: ComponentEnvironment,
  ts: TimeSource,
  domain: DomainDTO,
)(implicit fs: FacadeService, qs: QueryService)
    extends JobRootApi
    with ComponentImplementation:
  import JobRootApiImpl.*

  override def get(q: ApiQuery): ApiQueryResults[Job[?]] =
    ApiQuerySupport.query(parent.queryJobs, q.withDataModel[Job[?]], classOf[Job[?]], filterJobTypes)

  override def get(id: Long): Option[Job[?]] =
    parent.getJob(id)

  override def create[T <: Job[?]](job: T): T =
    parent.addComponent[Job[?], T](job)

  override def components: ComponentRootApi =
    ComponentSupport.createSingletonComponent(classOf[ComponentRootApi], classOf[Job[?]])

  override def validateCron(cronDTO: CronDTO): ErrorResponse \/ Option[Date] =
    val schedule = cronDTO.schedule
    for _ <- isValidCron(schedule) \/> ErrorResponse.validationError("schedule", schedule)("Invalid schedule.")
    yield Option(schedule)
      .filter(sch => sch != Job.Manual)
      .flatMap(sch => Option(cronExp(sch).getNextValidTimeAfter(ts.date)))

  private def isValidCron(schedule: String) =
    Option(schedule).filter(str => str == Job.Manual || CronExpression.isValidExpression(str))

  private def parent: JobParentFacade =
    domain.facade[JobRootFacade].getFolderByType(JobFolderType)

  // This filters for only supported types. The API could filter for connector type from the UI.
  private def filterJobTypes(qb: QueryBuilder, apiQuery: ApiQuery): QueryBuilder =
    ApiQuerySupport
      .getQueryBuilder(qb, apiQuery)
      .addCondition(
        ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER,
        Comparison.in,
        componentEnvironment.getComponents(classOf[Job[?]]).asScala flatMap { j =>
          // Use both identifier and schema name for backwards compatibility for now.
          List(j.getIdentifier) ++ ClassUtils.findAnnotation(j.getCategory, classOf[Schema]).asScala.map(_.value)
        }
      )

  private def cronExp(schedule: String) =
    new CronExpression(schedule) <| { exp => exp.setTimeZone(TimeZone.getTimeZone(domainTimeZone)) }

  private def domainTimeZone = OptionNZ(domain.timeZone) getOrElse "US/Eastern"
end JobRootApiImpl

object JobRootApiImpl:
  val JobFolderType = "job"
