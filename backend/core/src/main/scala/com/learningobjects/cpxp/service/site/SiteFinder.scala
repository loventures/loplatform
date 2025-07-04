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

package com.learningobjects.cpxp.service.site

import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import com.learningobjects.cpxp.entity.{IndexType, LeafEntity}
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.Cache as HCache

import java.lang

// This has to be a leaf with parent folder because we do locking create
@Entity
@HCache(usage = READ_WRITE)
class SiteFinder extends LeafEntity:
  @Column(nullable = false)
  @FunctionalIndex(function = IndexType.LCASE, byParent = true, nonDeleted = true)
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  var siteId: String = scala.compiletime.uninitialized

  @Column(nullable = false)
  @FunctionalIndex(function = IndexType.LCASE, byParent = true, nonDeleted = true)
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  var name: String = scala.compiletime.uninitialized

  @Column(nullable = false)
  @Queryable
  var restricted: lang.Boolean = scala.compiletime.uninitialized
end SiteFinder

object SiteFinder:
  final val Type       = "Site"
  final val SiteId     = "Site.siteId"
  final val Name       = "Site.name"
  final val Restricted = "Site.restricted"
