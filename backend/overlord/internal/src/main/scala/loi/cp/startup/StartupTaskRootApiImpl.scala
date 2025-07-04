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

package loi.cp.startup

import org.apache.pekko.actor.{ActorRef, ActorSystem, PoisonPill}
import org.apache.pekko.util.Timeout
import com.learningobjects.cpxp.async.async.AsyncLogHandler
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{HttpContext, NoResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentManager}
import com.learningobjects.cpxp.scala.environment.TransactionEnvironment.tx
import com.learningobjects.cpxp.scala.util.HttpSessionOps.*
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.{QueryBuilder, QueryService}
import com.learningobjects.cpxp.service.startup.StartupTaskConstants
import com.learningobjects.cpxp.startup.{StartupTaskService, TaskIdentifier, TaskState}
import com.learningobjects.cpxp.util.logging.LogCapture
import jakarta.servlet.http.HttpSession
import loi.cp.sse.SseResponse
import scaloi.syntax.OptionOps.*
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import loi.apm.Apm

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

/** Implementation of startup task root API.
  *
  * @param componentInstance
  *   the component instance
  * @param qs
  *   the query service
  * @param ows
  *   the overlord web service
  * @param dws
  *   the domain web service
  * @param ss
  *   the startup task service
  */
@Component
class StartupTaskRootApiImpl(val componentInstance: ComponentInstance)(implicit
  qs: QueryService,
  ows: OverlordWebService,
  dws: DomainWebService,
  ec: ExecutionContext,
  ss: StartupTaskService,
  actorSystem: ActorSystem
) extends StartupTaskRootApi
    with ComponentImplementation:

  import StartupTaskRootApiImpl.*
  import StartupTasks.*
  import TaskStates.*

  /** Re-execute the system startup tasks. */
  override def execute(): Unit =
    LogCapture.captureLogs(new AsyncLogHandler(classOf[StartupTaskRootApiImpl])) {
      ss.startup()
    }

  /** Get the state of the system startup tasks. */
  override def systemTasks: Seq[StartupTaskDto] =
    val env    = ComponentManager.getComponentEnvironment
    val tasks  = env.systemStartupTasks
    val states = taskStates(ows.findOverlordDomainId)
    tasks map { task =>
      StartupTaskDto(task, states.get(TaskIdentifier(task)))
    }

  /** Get the state of a domain's startup tasks. */
  override def tasksByDomain(domain: Long): Seq[StartupTaskDto] =
    val env    = dws.setupContext(domain)
    val tasks  = env.domainStartupTasks(domain == ows.findOverlordDomainId.longValue)
    val states = taskStates(domain)
    tasks map { task =>
      StartupTaskDto(task, states.get(TaskIdentifier(task)))
    }

  /** Get a startup task run receipt. */
  override def receipt(id: Long): Option[StartupTaskReceipt] =
    receipts(ApiQuery.byId(id)).asOption

  /** Get startup task run receipts. */
  override def receipts(q: ApiQuery): ApiQueryResults[StartupTaskReceipt] =
    ApiQueries.query[StartupTaskReceipt](taskQuery, q)

  /** Update the state of a task run. */
  override def updateReceipt(id: Long, state: StartupTaskStateDto): Option[StartupTaskReceipt] =
    receipt(id) <|? {
      _.setState(state.state)
    }

  /** Get an event stream. */
  override def events(http: HttpContext): WebResponse =
    val session = http.request.getSession
    Apm.ignoreTransaction()
    // kill any existing SSE actor
    killSseActor(session)
    // open an async SSE stream to the client
    val stream  = SseResponse.openAsyncStream(http)
    // start a new SSE actor
    session.setAttribute(ActorAttribute, StartupSseActor.create(StartupTaskActor.clusterActor, stream))
    // leave the actor in charge of the response
    NoResponse
  end events

  /** Kill any event stream. */
  override def cancelEvents(http: HttpContext): Unit =
    killSseActor(http.request.getSession)

  /** Get overall status. */
  override def status: Future[String] =
    for status <- StartupTaskActor.clusterActor.askFor[StartupTaskActor.UpdateStatus](StartupTaskActor.GetStatus)
    yield
      if status.status.isDefined then InProgress
      else
        tx {
          TaskState(ss.lastStartupSucceeded).entryName
        }

  /** Delete any open startup actor associated with this session.
    *
    * @param session
    *   the http session
    */
  private def killSseActor(session: HttpSession): Unit =
    session.attrTag[StartupSseActor, ActorRef](ActorAttribute) foreach { actor =>
      session.removeAttribute(ActorAttribute)
      actor ! PoisonPill
    }

  /** Query builder over startup task receipts. */
  private def taskQuery: QueryBuilder =
    qs.queryAllDomains(StartupTaskConstants.ITEM_TYPE_STARTUP_TASK)
end StartupTaskRootApiImpl

/** Startup task root api implementation companion.
  */
object StartupTaskRootApiImpl:

  /** The attribute under which the startup SSE actor is stored. */
  final val ActorAttribute = "ug:startupSseActor"

  private implicit val StartupPekkoTimeout: Timeout = Timeout(10.seconds)

  private final val InProgress = "InProgress"
