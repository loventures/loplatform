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

package com.learningobjects.cpxp.util

import com.learningobjects.cpxp.scala.util.Misc.*
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.data.Data
import jakarta.persistence.Table
import org.hibernate.cache.spi.DomainDataRegion
import org.hibernate.cache.spi.access.{CollectionDataAccess, EntityDataAccess}
import org.hibernate.engine.spi.{SessionFactoryImplementor, SessionImplementor}
import org.hibernate.internal.SessionImpl
import org.hibernate.metamodel.model.domain.NavigableRole
import org.hibernate.{Hibernate, LockMode, Session, SessionEventListener}
import scalaz.std.string.*
import scaloi.syntax.`class`.*
import scaloi.syntax.boxes.*
import scaloi.syntax.classTag.*
import scaloi.syntax.finiteDuration.*
import scaloi.syntax.option.*

import java.{lang as jl, util as ju}
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions
import scala.reflect.ClassTag

final class HibernateSessionOps(val session: Session) extends AnyVal:
  import HibernateSessionOps.*

  /** A fixed version of `byMultipleIds` that correctly considers the l1/l2 caches. The returned list orders the
    * entities in the same order as the `ids` list, but it may not be the same size:
    *
    * <ul> <li>An id that is not found has no representation in the return value, not even a null where the entity would
    * have been.</li> <li>The return value entities are distinct. The entity for an id that is repeated in `ids` will
    * only appear once in the return value, in the equivalent position as the first occurrence of the id in `ids`. </li>
    * </ul>
    *
    * @param ids
    *   the IDs to load
    * @tparam E
    *   the type of entity to load
    */
  def bulkLoadFromCaches[E: ClassTag](
    ids: Iterable[Long],
  ): List[E] = if ids.isEmpty then List.empty
  else
    val clasz   = classTagClass[E]
    val session = this.session.unwrap(classOf[SessionImpl]) // no secrets among enemies
    val count   = ids.size

    val persister     = session.getSessionFactory.getMappingMetamodel.getEntityDescriptor(clasz)
    val cachedBuilder = Map.newBuilder[Long, E]
    val toLoadBuilder = List.newBuilder[Long]
    val cache         =
      if persister.canReadFromCache then
        persister.getCacheAccessStrategy.getRegion.getEntityDataAccess(persister.getNavigableRole)
      else null

    def getFromSession(id: Long) = l1Get[E](id)
      .tap(entity => cachedBuilder += id -> entity)
      .isDefined

    def getFromL2(id: Long) = (cache ne null) && {
      val ckey = cache.generateCacheKey(id, persister, session.getSessionFactory, null)
      cache.contains(ckey) && {
        val inL2 = cache.get(session, ckey)
        // yes, it can happen, or so I'm told
        (inL2 ne null) && truly {
          cachedBuilder += id -> session.getReference(clasz, id)
        }
      }
    }

    val (_, cacheCheckDur) = Stopwatch.profiled {
      for id <- ids do if !(getFromSession(id) || getFromL2(id)) then toLoadBuilder += id
    }

    val cached = cachedBuilder.result()
    val toLoad = toLoadBuilder.result()

    val (bulkLoaded, bulkLoadDur) = Stopwatch.profiled {
      val entities = session
        .byMultipleIds(clasz)
        .withBatchSize(toLoad.size min 8192)
        .enableSessionCheck(false)
        .enableOrderedReturn(true) // true is default, but this is important
        .multiLoad(toLoad.boxInside().asJava)
        .asScala

      // zip is correct because of enabledOrderedReturn
      // but we choose to omit the null values (i.e. not found ids)
      (toLoad zip entities).toMap.filterNot(_._2 == null)
    }

    logger trace {
      s"""bulkLoadFromCaches:
          | loading $count rows from ${clasz.getSimpleName}
          | of which
          |  - ${cached.size} loaded from cache in ${cacheCheckDur.toHumanString}
          |  - ${bulkLoaded.size} loaded in ${bulkLoadDur.toHumanString}
        """.stripMargin
    }

    val entitiesById = cached ++ bulkLoaded

    // order the entities in the same order as the ids
    ids.toList.distinct.flatMap(entitiesById.get)

  /** Gets the entity from the session if the session contains it. Does not fetch the entity if absent, i.e. does not
    * get from L2 and does not get from DB.
    */
  def l1Get[E: ClassTag](id: Long): Option[E] =
    val clazz       = classTagClass[E]
    val sessionImpl = session.unwrap(classOf[SessionImpl])
    val persister   = sessionImpl.getSessionFactory.getMappingMetamodel.getEntityDescriptor(clazz)
    val key         = sessionImpl.generateEntityKey(id, persister)
    Option(sessionImpl.getEntityUsingInterceptor(key)).map(clazz.cast)

  /** Runs action when the Hibernate session successfully completes its transaction.
    */
  def onTxnComplete(action: () => Unit): Unit =
    val listener = new RunOnceTransactionCompletionListener(action)
    session.addEventListeners(listener)

  /** @return
    *   the next id from the generator we use on all the entities
    */
  def generateId(): Long =
    val implementor = session.unwrap(classOf[SessionImplementor])
    implementor.getSessionFactory.getMappingMetamodel
      .getEntityDescriptor(EntityNameWithGenerator)
      .getEntityMetamodel
      .getIdentifierProperty
      .getIdentifierGenerator
      .generate(implementor, null)
      .asInstanceOf[Long]

  def ref[A: ClassTag](id: Long): A = session.getReference(classTagClass[A], id)

  def getOption[A: ClassTag](id: Long): Option[A] = Option(session.find(classTagClass[A], id))

  /** @return
    *   true if the session contains id or the L2 cache contains id, false otherwise
    */
  def isCached[A: ClassTag](id: Long): Boolean =
    val sessionImpl    = session.unwrap(classOf[SessionImpl])
    val sessionFactory = sessionImpl.getSessionFactory
    val persister      = sessionFactory.getMappingMetamodel.getEntityDescriptor(classTagClass[A])
    val sessionKey     = sessionImpl.generateEntityKey(id, persister)
    val inSession      = sessionImpl.getEntityUsingInterceptor(sessionKey) != null

    lazy val l2Cache = domainDataCache[A]
    lazy val l2Key   = l2Cache.generateCacheKey(id)
    lazy val inL2    = l2Cache.self.contains(l2Key)

    inSession || inL2
  end isCached

  def lockAndRefresh[A <: AnyRef](entity: A): A =
    // because Hibernate skips the entire refresh for an uninitialized entity
    // which is fine for refresh semantics, but we need a DB lock on the row too.
    Hibernate.initialize(entity)

    // Flush required if entity is not inserted yet (i.e. a new row in this session)
    // Flush required if entity is dirty (without flush, `entity` restored to DB state)
    session.flush()
    // Warning: this refresh is super dodgy if the entity has joined relations
    // because Hibernate will *first* refresh and *second* lock, which basically
    // never works.
    session.refresh(entity: AnyRef, LockMode.PESSIMISTIC_WRITE)
    entity

    // no lock timeout because usual statement timeout applies
  end lockAndRefresh

  def domainDataCache[A: ClassTag]: RichEntityDataAccess[A] =
    val access = session.getSessionFactory
      .unwrap(classOf[SessionFactoryImplementor])
      .getCache
      .getRegion(classTagClass[A].getName)
      .asInstanceOf[DomainDataRegion]
      .getEntityDataAccess(new NavigableRole(null, classTagClass[A].getName))

    new RichEntityDataAccess[A](access, session)

  def collectionDataCache[A: ClassTag](collectionName: String): CollectionDataAccess =
    val regionName = classTagClass[A].getName + "." + collectionName
    session.getSessionFactory
      .unwrap(classOf[SessionFactoryImplementor])
      .getCache
      .getRegion(regionName)
      .asInstanceOf[DomainDataRegion]
      .getCollectionDataAccess(new NavigableRole(null, regionName))
end HibernateSessionOps

object HibernateSessionOps extends ToHibernateSessionOps:
  private val logger = org.log4s.getLogger

  // the name of any one of our entities that has the @GeneratedValue annotation on id
  private val EntityNameWithGenerator = classOf[Data].getName

  private[util] def bulkLoadFromCachesJava[E <: AnyRef](
    session: Session,
    ids: jl.Iterable[jl.Long],
    clasz: Class[? <: E]
  ): ju.List[E] =
    session
      .bulkLoadFromCaches[E](ids.unboxInsideTo[Iterable]())(using ClassTag(clasz))
      .asJava

  def entityName[T: ClassTag]: String = classTagClass[T].getName

  def nativeName[T: ClassTag]: String =
    val clаss = classTagClass[T]
    clаss.annotation[Table].map(_.name).filterNZ.getOrElse(clаss.getSimpleName)
end HibernateSessionOps

trait ToHibernateSessionOps:
  implicit def toHibernateSessionOps(session: Session): HibernateSessionOps =
    new HibernateSessionOps(session)

// we commit with Hibernate about 4 million times because Hibernate's stateful
// session can abide that. Some things can't

// using () => Unit instead of a non-strict param because I was debugging once
// and kept triggering evaluation unintentionally
class RunOnceTransactionCompletionListener(action: () => Unit) extends SessionEventListener:

  private var enabled = true

  override def transactionCompletion(successful: Boolean): Unit =
    try
      if enabled && successful then action()
    finally
      enabled = false

class RichEntityDataAccess[A: ClassTag](val self: EntityDataAccess, session: Session):

  def generateCacheKey(id: Long): AnyRef =
    val sessionImpl    = session.unwrap(classOf[SessionImpl])
    val sessionFactory = sessionImpl.getSessionFactory
    val persister      = sessionFactory.getMappingMetamodel.getEntityDescriptor(classTagClass[A])
    self.generateCacheKey(id, persister, sessionFactory, null)

  def contains(id: Long): Boolean =
    val key = generateCacheKey(id)
    self.contains(key)

  def evict(id: Long): Unit =
    val key = generateCacheKey(id)
    self.evict(key)
end RichEntityDataAccess
