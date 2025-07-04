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

package com.learningobjects.cpxp

import com.learningobjects.cpxp.util.NumberUtils
import org.hibernate.cache.internal.DefaultCacheKeysFactory

package object locache:
  private final val jvmId = NumberUtils.getSecureRandom.nextLong

  /** A cache element eviction broadcast message. Captures the identifier of the sending JVM so local removal messages
    * can be ignored.
    */
  final case class Evict(jvm: Long, id: AnyRef):

    /** Whether this event was sent by the local JVM. */
    def isLocal: Boolean = jvm == jvmId

  object Evict:
    def apply(id: AnyRef): Evict = Evict(jvmId, id)

  /** A whole cache eviction broadcast message. Captures the identifier of the sending JVM so local removal messages can
    * be ignored.
    */
  final case class EvictAll(jvm: Long):

    /** Whether this event was sent by the local JVM. */
    def isLocal: Boolean = jvm == jvmId

  object EvictAll:
    def instance: EvictAll = EvictAll(jvmId)

  def getEntityId(cacheKey: Any): AnyRef = DefaultCacheKeysFactory.staticGetEntityId(cacheKey)
end locache
