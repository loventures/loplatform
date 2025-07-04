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

import com.learningobjects.cpxp.entity.{LeafEntity, PeerEntity}
import com.learningobjects.cpxp.service.finder.Finder as Fynder
import com.learningobjects.cpxp.service.item.Item as Eitymn
import com.learningobjects.cpxp.util.PersistenceIdFactory
import org.hibernate.Session
import scaloi.syntax.ClassTagOps.*

import scala.reflect.ClassTag

/** Utilities for [[Fynder]] entities.
  */
object Finder:

  /** Create a finder and its associated [[Item]], by analogy to the item service.
    *
    * @param parent
    *   the parent of this item (sorry; you can't make domains)
    * @param init
    *   a function to initialise the finder. Any non-null constraints need to be satisfied here.
    * @param session
    *   the hibernate session which is responsible for persisting the newly-created entities
    * @param ids
    *   a source of fresh, unused PKs.
    * @tparam T
    *   the type of finder to create
    * @return
    *   the finder
    */
  def create[T <: Fynder: ClassTag](parent: Eitymn)(init: T => Unit)(implicit
    session: Session,
    ids: PersistenceIdFactory,
  ): T =
    val id     = ids.generateId()
    val finder = classTagClass[T].getDeclaredConstructor().newInstance()
    finder.setId(id)
    finder.setRoot(parent.getRoot)
    finder.setNew(true)
    finder match
      case (_: LeafEntity | _: PeerEntity) =>
        finder.setParent(parent)
        finder.setPath(s"${parent.path}$id/")
      case _                               =>
    val item   = new Eitymn(finder)
    finder.setOwner(item)
    item.setNew(true)

    init(finder)

    (finder match
      case _: PeerEntity => item :: finder :: Nil
      case _             => finder :: Nil
    ) foreach session.persist

    finder
  end create

  /** Allow an implicit `() => Session` to provide evidence of an implicit `Session`. Useful in the case of stateless
    * services which cannot DI a vanilla implicit `Session`.
    */
  implicit def unprovideImplicitSession(implicit sessions: () => Session): Session = sessions()
end Finder
