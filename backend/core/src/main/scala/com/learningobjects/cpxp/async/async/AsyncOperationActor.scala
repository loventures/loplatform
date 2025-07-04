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

package com.learningobjects.cpxp.async.async

import org.apache.pekko.actor.{Actor, ActorRef, PoisonPill}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.scala.environment.*
import com.learningobjects.cpxp.scala.json.Encode
import com.learningobjects.cpxp.scala.json.JsonCodec.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.user.UserWebService
import com.learningobjects.cpxp.util.Instrumentation
import de.tomcat.juli.LogMeta
import loi.apm.Apm
import org.apache.commons.lang3.exception.ExceptionUtils
import org.log4s.Logger
import scaloi.misc.TimeSource

import scala.language.implicitConversions
import scala.util.control.NonFatal

object AsyncOperationActor:
  object Perform
  object Complete

  case class Failure(th: Throwable)
  case class InProgress[R](done: Long, todo: Long, description: String, operation: AsyncSSEOperation[R])
  case class Warning[R](message: String, operation: AsyncSSEOperation[R])
  case class ErrorResponse(description: String, detail: String, status: String = "asyncError")
  val TASK_ACTOR = "taskActor"
  val TASK_OP    = "taskOperation"

  def tellProgress(done: Long, todo: Long, description: String): Unit =
    tell((operation: AsyncSSEOperation[?]) => InProgress(done, todo, description, operation))

  def tellWarning(message: String): Unit =
    tell((operation: AsyncSSEOperation[?]) => Warning(message, operation))

  private def tell[R](f: (AsyncSSEOperation[R]) => Any): Unit =
    for
      actor     <- Option(Current.get(TASK_ACTOR).asInstanceOf[ActorRef])
      operation <- Option(Current.get(TASK_OP).asInstanceOf[AsyncSSEOperation[R]])
    yield actor ! f(operation)

  val ProgressInterval = 333L

  class AsyncProgress(todo: Long, interval: Long = ProgressInterval):
    private var done = 0L
    private var ts   = System.currentTimeMillis

    def increment(): Unit = done(1)

    def done(amount: Long, msg: String = ""): Unit =
      val now = System.currentTimeMillis
      done += amount
      if now - ts >= interval then
        ts = now
        AsyncOperationActor.tellProgress(
          done,
          todo,
          if msg.isEmpty then s"$done / $todo"
          else msg
        )
    end done

    def warn(msg: String): Unit =
      AsyncOperationActor `tellWarning` msg
  end AsyncProgress

  def withTodo[T](todo: Int)(f: (AsyncProgress) => T): T =
    f(new AsyncProgress(todo.toLong))

  val logger = org.log4s.getLogger
end AsyncOperationActor

class AsyncOperationActor[R](
  operation: AsyncSSEOperation[R],
)(implicit
  mapper: ObjectMapper,
  rEncoder: Encode.Aux[R, JsonNode],
  now: TimeSource,
) extends BaseEnvironment[Unit, R]
    with Actor
    with TransactionEnvironment[Unit, R]
    with DomainEnvironment[Unit, R]
    with UserEnvironment[Unit, R]:
  import AsyncOperationActor.*

  implicit def domainIdEvidence(input: Unit): Long = operation.domainId
  override def user(input: Unit)                   =
    service[UserWebService].getUserDTO(operation.userId)

  def receive = { case Perform =>
    val tracer = Instrumentation.getTracer(operation.getClass.getMethod("perform"), operation, true)
    try
      Apm.setTransactionName("async", operation.origin.replaceAll("/[0-9].*", ""))
      LogMeta.domain(operation.domainId)
      LogMeta.user(operation.userId)
      Current.put(TASK_ACTOR, sender())
      Current.put(TASK_OP, operation)
      val result       = this.performNoParam(operation.perform)
      val jsonBody     = result.encode
      val asyncMessage =
        AsyncEvent(
          guid = operation.guid,
          origin = operation.origin,
          status = "ok",
          body = jsonBody,
          channel = AsyncRouter.op2ChannelId(operation),
          id = 0L,
          timestamp = now.date,
        )
      sender() ! asyncMessage
      sender() ! Complete
      self ! PoisonPill
      tracer.success(result)
    catch
      case NonFatal(th) =>
        logger.warn(th)("Async operation error")
        // TODO: Use ExceptionResolver to produce json
        val body: JsonNode =
          ErrorResponse("Operation error", ExceptionUtils.getRootCauseMessage(th)).encode[JsonNode]
        val errorMessage   =
          AsyncEvent(
            guid = operation.guid,
            origin = operation.origin,
            status = "error",
            body = body,
            channel = AsyncRouter.op2ChannelId(operation),
            id = 0L,
            timestamp = now.date,
          )
        sender() ! errorMessage
        sender() ! Failure(th)
        throw tracer.failure(th)
    finally LogMeta.clear()
    end try
  }

  override def before(unit: Unit): Unit          = super.before(unit)
  override def after[RR <: R](returnVal: RR)     = super.after(returnVal)
  override def onError(th: Throwable): Throwable = super.onError(th)

  override def logger: Logger = AsyncOperationActor.logger
end AsyncOperationActor
