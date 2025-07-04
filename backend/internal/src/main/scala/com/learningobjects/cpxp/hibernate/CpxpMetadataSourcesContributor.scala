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

package com.learningobjects.cpxp.hibernate

import jakarta.persistence.Entity

import com.learningobjects.cpxp.CpxpClasspath
import com.learningobjects.cpxp.service.CpxpOptimizer
import com.learningobjects.cpxp.service.component.misc.{AssetGraphResult, AssetPathResult}
import com.learningobjects.cpxp.service.data.Data
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.replay.Replay
import com.learningobjects.cpxp.service.upgrade.SystemInfo
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.spi.MetadataSourcesContributor

import scala.jdk.CollectionConverters.*

/** Contributes information about out entity types to the hibernate metadata initialization process. We do this because
  * Hibernate will not scan our classpath during local development, only jars?
  */
class CpxpMetadataSourcesContributor extends MetadataSourcesContributor:
  override def contribute(metadataSources: MetadataSources): Unit =
    // add the @GenericGenerator annotation on the package-info for ...cpxp.service
    metadataSources.addPackage(classOf[CpxpOptimizer].getPackage)
    // add our entity classes
    entityClasses foreach metadataSources.addAnnotatedClass

  private def entityClasses =
    coreClasses ++ finderClasses

  private def finderClasses =
    CpxpClasspath.classGraph // barbar
      .getClassesWithAnnotation(classOf[Entity])
      .loadClasses()
      .asScala
      .filter(classOf[Finder].isAssignableFrom)

  /** other JPA entities to register
    */
  private val coreClasses = List(
    classOf[Item],
    classOf[Data],
    classOf[SystemInfo],
    classOf[Replay],
    classOf[AssetGraphResult],
    classOf[AssetPathResult]
  )
end CpxpMetadataSourcesContributor
