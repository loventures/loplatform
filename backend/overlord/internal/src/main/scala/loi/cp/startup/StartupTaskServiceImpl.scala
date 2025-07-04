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

import cats.effect.unsafe.implicits.global
import cats.instances.list.*
import cats.syntax.parallel.*
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentManager, ComponentSupport}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainFacade, DomainWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.{Direction, QueryService}
import com.learningobjects.cpxp.service.startup.StartupTaskConstants.*
import com.learningobjects.cpxp.startup.*
import com.learningobjects.cpxp.util.EntityContextOps.*
import com.learningobjects.cpxp.util.logging.MinimalFormatter
import com.learningobjects.cpxp.util.logging.ThreadLogs.*
import com.learningobjects.cpxp.util.{EntityContext, ManagedUtils, ThreadFactoryBuilder}
import com.typesafe.config.Config
import de.tomcat.juli.LogMeta
import loi.cp.slack.{SlackAttachment, SlackService}
import scalaz.std.anyVal.booleanInstance
import scalaz.std.iterable.*
import scalaz.std.map.*
import scalaz.std.set.*
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.foldable.*
import scalaz.syntax.semigroup.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.tag.*
import scalaz.{-\/, @@, Monoid, \/, \/-}
import scaloi.misc.Monoids.{FailFast, failFastDisjunctionMonoid}
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.jEnum.*
import scaloi.syntax.finiteDuration.*

import java.util.Date
import java.util.concurrent.{ExecutorService, Executors}
import java.util.logging.Formatter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Using.Releasable
import scala.util.{Random, Using}
import scala.util.control.NoStackTrace

/** Implementation of the startup service.
  * @param cfg
  *   the config
  * @param dws
  *   the domain web service
  * @param ec
  *   the entity context
  * @param fs
  *   the facade service
  * @param ows
  *   the overlörde web service
  * @param qs
  *   the query service
  * @param slack
  *   the slack service
  * @param sm
  *   the service meta
  */
@Service
class StartupTaskServiceImpl(implicit
  cfg: Config,
  dws: DomainWebService,
  ec: () => EntityContext,
  fs: FacadeService,
  ows: OverlordWebService,
  qs: QueryService,
  slack: SlackService,
  sm: ServiceMeta
) extends StartupTaskService:
  import StartupTaskServiceImpl.*
  import StartupTasks.*
  import TaskStates.*

  /** Run all the system start tasks and, if they succeed, then all tho domain startup tasks for all domains.
    */
  override def startup(): Unit =
    tapLogs(startupEverything()) { (result, logs) =>
      // if there was a failure or a task ran then record the logs
      if result.valueOr(_ => true) then recordSystemStartup(result.isRight, logs)
      result.leftTap { case info @ FailureInfo(byIdent, byDomain) =>
        byIdent.values.flatten.find(_.err ne TaskAlreadyFailed) foreach {
          case FailedTask(Some(task), domainName, duration, err) =>
            val message = TaskFailureMessage(task, duration, domainName, info, insult())
            logSlackErrors(task, slack.postMessage(message, SlackAttachment.fromError(err).some))
          case _                                                 =>
        }
      }
    }
    StartupTaskActor.tellIdle()
  end startup

  /** Start up everything.
    * @return
    *   throwable or whether any task ran
    */
  private def startupEverything(): FailureInfo \/ Boolean =
    val (result, duration) = Stopwatch.profiled {
      // if starting up the system succeeds
      startupSystem() flatMap { systemTaskRan =>
        // then start all the domains and return whether any domain
        // failed or anyone ran a startup task
        \/.DisjunctionMonoid[FailureInfo, Boolean]
        (startupDomains(allDomains).toSeq :+ systemTaskRan.right).suml
      }
    }
    logger info s"Startup completed in ${duration.toHumanString}"
    result
  end startupEverything

  /** Record the system startup logs.
    * @param success
    *   whether startup was successful
    * @param logs
    *   the startup logs
    */
  private def recordSystemStartup(success: Boolean, logs: String): Unit =
    overlordDomain.addFacade[StartupTaskFacade] { facade =>
      facade.setIdentifier(SystemTaskIdentifier)
      facade.setVersion(sm.getBranch.hashCode)
      facade.setTimestamp(new Date())
      facade.setState(TaskState(success))
      facade.setLogs(logs)
    }
    ManagedUtils.commit()

  /** Run the startup scripts for a domain, post creation.
    */
  override def startupDomain(id: Long): Throwable \/ Boolean =
    startupDomain(id.facade[DomainFacade], systemStartup = false).leftMap(_.byIdent.values.flatten.head.err)

  /** Return whether the last system startup succeeded.
    */
  override def lastStartupSucceeded: Boolean =
    qs.queryRoot(overlordDomain.getId, ITEM_TYPE_STARTUP_TASK)
      .addCondition(DATA_TYPE_STARTUP_TASK_IDENTIFIER, "eq", SystemTaskIdentifier)
      .setOrder(DATA_TYPE_STARTUP_TASK_TIMESTAMP, Direction.DESC)
      .setDataProjection(DATA_TYPE_STARTUP_TASK_STATE)
      .setLimit(1)
      .optionResult[String]
      .forall(_ == TaskState.Success.entryName)

  /** Run the system startup tasks. If there is no overlord domain then no tasks will run.
    *
    * @return
    *   throwable or whether any task ran
    */
  private def startupSystem(): FailureInfo \/ Boolean =
    startupSystem(overlordDomain)

  /** Run the system startup tasks in the context of the overlord domain.
    *
    * @param domain
    *   the overlord domain
    * @return
    *   throwable or whether any task ran
    */
  private def startupSystem(domain: DomainFacade): FailureInfo \/ Boolean =
    logger info "Running system startup tasks"
    val env    = ComponentManager.getComponentEnvironment
    val tasks  = env.systemStartupTasks
    val result = tasks foldMap considerTask(domain, env, systemStartup = true)
    notifyActors(None, CompletionTask, startup = true)(().right)
    result.unwrap

  /** Run the domain startup tasks over a series of domains. Failure of one domain does not prevent other domains from
    * being processed.
    *
    * @param domains
    *   the domains to process
    */
  private def startupDomains(domains: Iterable[DomainFacade]): Iterable[FailureInfo \/ Boolean] =
    logger info s"${domains.size} domains to startup: ${domains.map(_.getName).mkString(", ")}"
    Using.resource(
      Executors.newFixedThreadPool(StartupThreads, new ThreadFactoryBuilder().pattern("StartupTask", true).finishConfig)
    ) { implicit es =>
      val tasks = domains.toList map { domain =>
        loggedIO {
          import argonaut.*
          import Argonaut.*
          LogMeta.let(LogMeta.Domain := domain.getId) {
            ManagedUtils perform { () =>
              startupDomain(domain, systemStartup = true)
            }
          }
        }
      }
      // fork it into the startup executor
      val ec    = ExecutionContext.fromExecutorService(es)
      tasks.parSequence.evalOn(ec).unsafeRunSync()
    }
  end startupDomains

  /** Run the startup tasks for a domain. This entails initializing the component environment for the domain before
    * selecting and running the tasks.
    *
    * @param domain
    *   the domains to startup
    * @return
    *   throwable or success
    */
  private def startupDomain(domain: DomainFacade, systemStartup: Boolean): FailureInfo \/ Boolean =
    logger info s"Running domain startup tasks: ${domain.getName} (${domain.getDomainId})"
    val env    = dws.setupContext(domain.getId)
    val tasks  = env.domainStartupTasks(overlord = domain.getType == DomainConstants.DOMAIN_TYPE_OVERLORD)
    val result = tasks foldMap considerTask(domain, env, systemStartup)
    notifyActors(Some(domain.getId.longValue), CompletionTask, systemStartup)(().right)
    result.unwrap

  /** Returns a function that considers whether to run a startup task in a particular context based on the persisted
    * task status. If the task has already run successfully or is flagged as skipped then this returns success. If the
    * task has already failed then it returns failure. Otherwise, if the task has not run or has been flagged for retry
    * then it executes the task and returns the result.
    *
    * @param domain
    *   the domain in which the task is being run
    * @param env
    *   the component environment
    * @param systemStartup
    *   is this been run during system startup
    * @return
    *   a function from a startup task to whether any task ran
    */
  private def considerTask(
    domain: DomainFacade,
    env: ComponentEnvironment,
    systemStartup: Boolean,
  ): StartupTaskInfo.Any => (FailureInfo \/ Boolean) @@ FailFast =
    val states = taskStates(domain.getId)
    (task: StartupTaskInfo.Any) =>
      FailFast {
        val identifier  = TaskIdentifier(task)
        val state       = states.get(identifier)
        // notify the startup task actor that we're processing this task
        val domainScope = task.binding.taskScope != StartupTaskScope.System
        val domainId    = domainScope.option(domain.getId).map(_.longValue)
        StartupTaskActor.tellStatus(
          StartupTaskActor.StartupStatus(domainId, identifier, threadId, state, startup = systemStartup)
        )
        state match
          case Some(TaskState.Skip) | Some(TaskState.Success) =>
            false.right
          case Some(TaskState.Failure)                        =>
            taskAlreadyFailed(identifier, domain).left
          case None | Some(TaskState.Retry)                   =>
            runTask(domain, env, task).<| { case Timed(dur, res) =>
              notifyActors(domainId, identifier, startup = systemStartup)(res)
              if dur > cfg.getDuration("loi.cp.startup.too_long").toMillis.millis then
                val domainId = (task.binding.taskScope == StartupTaskScope.System).noption(domain.getDomainId)
                logSlackErrors(identifier, slack.postMessage(tookTooLongMessage(identifier, dur, domainId, insult())))
            } match
              case Timed(dur, res @ -\/(err)) =>
                FailureInfo(identifier, domain, dur, err).left
              case Timed(_, res @ \/-(()))    =>
                true.right
        end match
      }
  end considerTask

  /** Current thread identifier.
    */
  private def threadId = Thread.currentThread.threadId

  /** Broadcast a notification to all listening `StartupSseActor`s.
    *
    * @param domainId
    *   the ID of the domain, or `None` for system tasks
    * @param task
    *   the task identifier
    * @param result
    *   the result of the task (either an error or success)
    * @param startup
    *   is this part of system startup
    */
  private def notifyActors(domainId: Option[Long], task: TaskIdentifier, startup: Boolean)(
    result: Throwable \/ Unit
  ): Unit =
    val newState = result.isRight.fold(TaskState.Success, TaskState.Failure)
    StartupTaskActor.tellStatus(StartupTaskActor.StartupStatus(domainId, task, threadId, Some(newState), startup))

  /** Log errors from the given Slack response.
    *
    * @param identifier
    *   the task identifier the logging of which produced the given response
    * @param response
    *   the given response
    */
  private def logSlackErrors(identifier: TaskIdentifier, response: SlackService.SlackResponse): Unit =
    response match
      case error: SlackService.SendingFailedWithMessage   =>
        logger.warn(s"Failed to notify slack for $identifier. Error=" + error.msg)
      case error: SlackService.SendingFailedWithException =>
        logger.warn(error.wrappedException)(s"Failed to notify slack for $identifier")
      case _                                              => /* not an error; we don't care */
  /** Skips a startup task. Logs and persists this information.
    *
    * @param domain
    *   the domain in which the task is being run
    * @param task
    *   the task to skip
    */
  private def skipTask(domain: DomainFacade, task: StartupTaskInfo.Any): Unit                        =
    tapLogs(logger info s"Skipping upgrade startup task ${task.startupClass.getSimpleName} v${task.binding.version}") {
      (_, logs) =>
        recordTask(domain, task, TaskState.Skip, logs)
    }

  /** Runs a startup task and stores the resulting state in the database. Note that this function will commit the
    * current transaction.
    *
    * @param domain
    *   the domain in which the task is being run
    * @param env
    *   the component environment
    * @param task
    *   the task to run
    * @return
    *   the time that it took, and throwable or success
    */
  private def runTask(
    domain: DomainFacade,
    env: ComponentEnvironment,
    task: StartupTaskInfo.Any
  ): Timed[Throwable \/ Unit] =
    tapLogs(executeTask(env, task)) { (result, logs) =>
      recordTask(domain, task, TaskState(result.value.isRight), logs)
    }

  /** Execute a task and then commit or rollback the current transaction as appropriate.
    *
    * @param env
    *   the component environment
    * @param task
    *   the task to run
    * @return
    *   the time that it took, and throwable or success
    */
  private def executeTask(env: ComponentEnvironment, task: StartupTaskInfo.Any): Timed[Throwable \/ Unit] =
    ec.withTimeout(timeout) {
      val stopwatch    = new Stopwatch
      /* stops the stopwatch only when evaluated */
      lazy val elapsed = stopwatch.elapsed
      val result       = \/.attempt {
        logger info s"Running startup task ${task.startupClass.getSimpleName} v${task.binding.version}"
        ManagedUtils.commit() // commit any previous state in case tx fails
        ComponentSupport.newInstance(env, task.startupClass).run()
        ManagedUtils.commit() // commit this run before claiming success
        logger info s"Completed in ${elapsed.toHumanString}"
      } { e =>
        ManagedUtils.rollback() // rollback what we can
        logger.warn(e)(s"Error running startup task ${task.startupClass}")
        logger info s"Failed in ${elapsed.toHumanString}"
        e
      }
      Timed(elapsed, result)
    }

  /** Record a task execution in the database and then commit the current transaction.
    *
    * @param domain
    *   the domain in which the task was run
    * @param task
    *   the task that was run
    * @param state
    *   the task state
    * @param logs
    *   the logs resulting from the task execution
    */
  private def recordTask(domain: DomainFacade, task: StartupTaskInfo.Any, state: TaskState, logs: String): Unit =
    domain.addFacade[StartupTaskFacade] { facade =>
      facade.setIdentifier(task.startupClass.getName)
      facade.setVersion(task.binding.version.toLong)
      facade.setTimestamp(new Date())
      facade.setState(state)
      facade.setLogs(logs)
    }
    ManagedUtils.commit()

  /** Summon the wisdom of Christian reformers past to elucidate on the precise nature of the present malfeasance.
    *
    * @return
    *   a message tailored to produce an emotional response befitting the crime which is producing a failing startup
    *   task
    */
  private def insult(): String =
    Random.shuffle(cfg.getStringList("loi.cp.startup.insult").asScala).head

  /** The default statement timeout for a startup task.
    */
  private def timeout: FiniteDuration =
    cfg.getDuration("loi.cp.startup.default_timeout").getSeconds.seconds
end StartupTaskServiceImpl

/** Startup task service implementation companion.
  */
object StartupTaskServiceImpl:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  /** The total system startup identifier. */
  private final val SystemTaskIdentifier = "system"

  /** Dummy task identifier for reporting completion.
    */
  private final val CompletionTask = TaskIdentifier("completion", 0)

  /** Threads to use for startup tasks. */
  private final val StartupThreads = 4

  /** A failure indicating that no overlord domain was found. */
  private object NoOverlordDomain extends Exception("No overlord domain; cannot run startup tasks!")

  /** A failure indicating that a task is in a persisted failed state. */
  private def taskAlreadyFailed(task: TaskIdentifier, domain: DomainFacade) =
    FailureInfo(task, domain, 0.seconds, TaskAlreadyFailed)

  /** The "Task already failed" pseudo-exception. */
  private object TaskAlreadyFailed extends Exception("Task already failed") with NoStackTrace

  /** A log formatter that includes timestamp information. */
  private implicit val logFormatter: Formatter =
    new MinimalFormatter(classOf[StartupTaskServiceImpl], true)

  /** Provide the disjunction for booleans so that it might be summoned for our summing whether any task runned. */
  private implicit val booleanDisjunction: Monoid[Boolean] = booleanInstance.disjunction

  /** Compute the message to be delivered when a task fails. */
  private def TaskFailureMessage(
    taskId: TaskIdentifier,
    duration: FiniteDuration,
    domain: Option[String],
    info: FailureInfo,
    insult: String
  )(implicit sm: ServiceMeta): String =
    import info.*
    import taskId.*

    /* Partition failures based on whether they occurred on the selected task's domain. */
    val onThisDomain   = domain.cata(byDomain, Set.empty)
    val onOtherDomains = domain.cata(byDomain - _, byDomain)

    /* Count the number of domains other than the selected one with failures */
    val failuresOnOtherDomains = onOtherDomains.values.flatten.toSet.size

    /* Describe the startup task. */
    val taskDescription = s"Startup task $identifier (version $version)"

    /* Return the string "and $n others have", if `n` other failures have occurred on the selected task's domain, otherwise "has" */
    val andOthersTasks = if onThisDomain.size > 1 then s"and ${onThisDomain.size - 1} others have" else "has"

    s"""
       |$insult
       |
       |$taskDescription $andOthersTasks failed spectacularly${onDomain(
        domain
      )} in cluster ${sm.getCluster} in ${duration.toHumanString}.
       |${(failuresOnOtherDomains > 0) ?? s"Additionally, $failuresOnOtherDomains ${plural(
          "task",
          failuresOnOtherDomains
        )} failed on ${onOtherDomains.size} other ${plural("domain", onOtherDomains.size)}."}
       """.stripMargin
  end TaskFailureMessage

  /** Compute the message to be delivered when a task takes too long (currently, more than 30 minutes) */
  private def tookTooLongMessage(
    taskId: TaskIdentifier,
    duration: FiniteDuration,
    domain: Option[String],
    insult: String
  )(implicit sm: ServiceMeta): String =
    s"""
       |$insult
       |
       |Startup task ${taskId.identifier} (Version ${taskId.version}) took an astounding ${duration.toHumanString} to run${onDomain(
        domain
      )} in cluster ${sm.getCluster}!
       """.stripMargin

  /** Return the string " on domain $domain", if there is a domain. */
  private def onDomain(domain: Option[String]): String = domain.map(" on domain ".concat).orZero

  /** A timed thing. */
  case class Timed[T](time: FiniteDuration, value: T)

  /** Executor service as resource. */
  implicit val executorServiceReleasable: Releasable[ExecutorService] = _.shutdown()

  /** A domain is identified by a string. */
  type DomainName = String

  /** Information concerning a failed task. */
  case class FailedTask(
    task: Option[TaskIdentifier],
    domain: Option[DomainName],
    time: FiniteDuration,
    err: Throwable
  )
  object FailedTask:
    def apply(task: TaskIdentifier, domain: DomainFacade, time: FiniteDuration, err: Throwable): FailedTask =
      FailedTask(Some(task), Some(domain.getDomainId), time, err)

  case class FailureInfo(byIdent: Map[TaskIdentifier, Set[FailedTask]], byDomain: Map[DomainName, Set[FailedTask]])
  object FailureInfo:
    implicit val FailureInfoMonoid: Monoid[FailureInfo] = Monoid.instance(
      (a, b) => FailureInfo(a.byIdent |+| b.byIdent, a.byDomain |+| b.byDomain),
      FailureInfo(Map.empty, Map.empty)
    )

    val Empty: FailureInfo = FailureInfoMonoid.zero

    def apply(task: TaskIdentifier, domain: DomainFacade, time: FiniteDuration, err: Throwable): FailureInfo =
      FailedTask(task, domain, time, err) |> (ft =>
        FailureInfo(Map(task -> Set(ft)), Map(domain.getDomainId -> Set(ft)))
      )

    /** Combine `FailureInfo`s by keeping plenary information on the leftmost, and reducing all of the others to a
      * count, except favor errors other than "task already failed"./ implicit val FailureInfoSemigroup:
      * Semigroup[FailureInfo] = new Semigroup[FailureInfo] { def append(fst: FailureInfo, snd: => FailureInfo) = { val
      * (keep, other) = if (fst.err ne TaskAlreadyFailed) (fst, snd) else (snd, fst) (keep.task == other.task,
      * keep.domain == other.domain) match { case (true, false) => // } } }
      */
  end FailureInfo

  /** Get the overlord domain.
    *
    * @param ows
    *   the overlord web service
    * @return
    *   the overlord domain
    */
  def overlordDomain(implicit ows: OverlordWebService): DomainFacade =
    Option(ows.findOverlordDomain).getOrElse(throw NoOverlordDomain)

  /** Get all domains in the system.
    *
    * @param ows
    *   the overlord web service
    * @return
    *   all domains, including the overlord domain
    */
  def allDomains(implicit ows: OverlordWebService): Iterable[DomainFacade] =
    List(overlordDomain) ++ ows.getAllDomains.asScala.sortWith(orderByStateThenName)

  /** Order domains by state and then name.
    *
    * @param a
    *   the first domain
    * @param b
    *   the second domain
    * @return
    *   whether the first domain is first
    */
  private[startup] def orderByStateThenName(a: DomainFacade, b: DomainFacade): Boolean =
    // this should be expressed as a composition of ordering monoids but, well.
    (a.getState == b.getState)
      .fold(a.getName < b.getName, a.getState < b.getState)
end StartupTaskServiceImpl
