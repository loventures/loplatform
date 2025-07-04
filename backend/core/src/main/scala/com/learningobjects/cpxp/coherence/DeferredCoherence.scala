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

package com.learningobjects.cpxp.coherence

import org.apache.pekko.cluster.pubsub.DistributedPubSub
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator.Publish
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.cache.Cache
import com.learningobjects.cpxp.util.tx.AfterTransactionCompletionListener
import de.mon.{DeMonitor, StatisticType}

import java.io.Serializable
import java.lang as jl
import scala.collection.mutable

/** Support for replicating application cache coherence messages across the cluster.
  *
  * This is the mediator between invalidation requests from the application (e.g. calling [Facade#invalidate] or
  * [Cache#remove]) and the pub sub cluster broadcast.
  *
  * If called from within a transaction, the coherence messages are queued up to be replicated after commit otherwise
  * they are replicated immediately.
  */
object DeferredCoherence:
  def invalidateItem(pk: jl.Long): Unit =
    enqueue(Publish(ItemCacheActor.TopicName, pk))

  def removeEntry(cache: Cache[?, ?, ?], key: Serializable): Unit =
    enqueue(Publish(ApplicationCacheActor.topicName(cache), ApplicationCacheActor.Remove(key)))

  def invalidateEntry(cache: Cache[?, ?, ?], name: String): Unit =
    enqueue(Publish(ApplicationCacheActor.topicName(cache), ApplicationCacheActor.Invalidate(name)))

  /** Thread-local storage of in-transaction invalidation messages. */
  private final val inTransaction = new ThreadLocal[DeferredCoherence]()

  /** If in a transaction, enqueue a message until commit, otherwise publish it immediately. * */
  private def enqueue(event: Publish): Unit =
    val existing = inTransaction.get
    if existing ne null then existing.enqueue(event)
    else if EntityContext.inTransaction then
      val coherence = new DeferredCoherence()
      EntityContext.onCompletion(coherence)
      inTransaction.set(coherence)
      coherence.enqueue(event)
    else publish(event)

  private def publish(event: Publish): Unit =
    DeMonitor.recordGlobalStatistic(StatisticType.AppCacheInvalidate, event.topic, 0)
    if !noBroadcast then DistributedPubSub(CpxpActorSystem.system).mediator ! event

  private[coherence] var noBroadcast: Boolean = false
end DeferredCoherence

class DeferredCoherence private extends AfterTransactionCompletionListener:
  import DeferredCoherence.*

  private val queue = mutable.Buffer.empty[Publish]

  private def enqueue(event: Publish): Unit = queue += event

  override def onCommit(): Unit =
    queue foreach publish
    inTransaction.remove()

  override def onRollback(): Unit =
    inTransaction.remove()
end DeferredCoherence
