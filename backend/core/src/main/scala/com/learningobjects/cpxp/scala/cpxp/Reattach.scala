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

package com.learningobjects.cpxp.scala.cpxp

import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.service.finder.Finder as CpxpFinder
import com.learningobjects.cpxp.util.EntityContext

/** A wrapper for [[CpxpFinder Finder]] s that can reattach them when the Hibernate session is closed.
  *
  * This functionality is provided by facades' proxy invocation handlers, but when using finders, a clear of Hibernate's
  * L1 cache (such as is done by `flushClearAndCommit()`) detaches the entities from the session, so that modifications
  * aren't tracked.
  */
final class Reattach[T <: CpxpFinder] private (private var _value: T):

  /** Return the wrapped finder, reattaching it if necessary.
    */
  def value(implicit ec: EntityContext, ontology: Ontology): T =
    if !isAttached then _value = ec.getEntityManager.find(entityClass, _value.id)
    _value

  /** Is the entity currently attached to the Hibernate session?
    *
    * This logic is extracted from `FacadeInvocationHandler` with minimal verification of propriety.
    */
  def isAttached(implicit ec: EntityContext, ontology: Ontology): Boolean =
    (                                                                     // attached iff...
      (_item ne null)                                                     // - valid item
        && ec.getEntityManager.isOpen                                     // - within a transaction
        && !(if peered then _item.isNew else _value.isNew)                // - not created in this transaction
        && ec.getEntityManager.contains(if peered then _item else _value) // - known to Hibernate
    )

  private def _item = _value.getOwner

  private def entityClass(implicit ontology: Ontology): Class[T] =
    ontology.getEntityDescriptor(_value.getItemType).getEntityType.asInstanceOf[Class[T]]

  private def peered(implicit ontology: Ontology) =
    ontology.getEntityDescriptor(_item.getItemType).getItemRelation.isPeered
end Reattach

object Reattach:

  def apply[T <: CpxpFinder](value: T): Reattach[T] =
    assert(value ne null, "null value")
    new Reattach[T](value)

  implicit def pk[T <: CpxpFinder]: PK[Reattach[T]] =
    re => re._value.getId

  import language.implicitConversions

  /** Properties (not named `value` or `isAttached`) on finders can be accessed on `Reattach` objects.
    */
  @inline
  implicit def unwrap[T <: CpxpFinder](re: Reattach[T])(implicit ec: EntityContext, ontology: Ontology): T =
    re.value
end Reattach
