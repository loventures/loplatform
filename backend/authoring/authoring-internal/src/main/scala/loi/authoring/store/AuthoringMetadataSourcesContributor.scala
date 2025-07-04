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

package loi.authoring.store

import loi.authoring.copy.store.CopyReceiptEntity
import loi.authoring.edge.store.{DurableEdgeEntity2, EdgeEntity2}
import loi.authoring.exchange.exprt.store.ExportReceiptEntity
import loi.authoring.exchange.imprt.store.ImportReceiptEntity
import loi.authoring.node.store.NodeEntity2
import loi.authoring.project.*
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.spi.MetadataSourcesContributor

class AuthoringMetadataSourcesContributor extends MetadataSourcesContributor:

  override def contribute(metadataSources: MetadataSources): Unit =
    entityClasses foreach metadataSources.addAnnotatedClass

  private val entityClasses = Seq(
    classOf[ImportReceiptEntity],
    classOf[ExportReceiptEntity],
    classOf[CopyReceiptEntity],
    /* laird rebjorn tables: */
    classOf[NodeEntity2],
    classOf[EdgeEntity2],
    classOf[DurableEdgeEntity2],
    classOf[ProjectEntity2],
    classOf[ProjectContributorEntity2],
    classOf[CommitDocEntity],
    classOf[CommitEntity2],
    classOf[SyncReportEntity]
  )
end AuthoringMetadataSourcesContributor
