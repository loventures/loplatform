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

package loi.cp.i18n

import loi.authoring.edge.Group

import java.util.UUID

object AuthoringBundle extends ResourceBundleLoader("loi.cp.i18n.AuthoringMessages"):

  def noSuchAsset(name: String): BundleMessage = message("noSuchAsset", name)
  def noSuchAsset(id: Long): BundleMessage     = message("noSuchAsset", id.toString)
  def noSuchCommit(id: Long): BundleMessage    = message("commit.noSuchCommit", Long.box(id))

  def projectCopyFailure(errorHead: String, errorTail: String*): BundleMessage =
    message("project.copyFailure", (errorHead +: errorTail).mkString(";"))

  def commitConflict: BundleMessage               = message("project.commitConflict")
  def branchCommit: BundleMessage                 = message("project.branchCommit")
  def notLayered: BundleMessage                   = message("project.notLayered")
  def dependencyCycle: BundleMessage              = message("project.dependencyCycle")
  def noSuchBranch(branchId: Long): BundleMessage = message("project.noSuchBranchId", Long.box(branchId))

  def noSuchDependency(projectId: Long, depProjectId: Long): BundleMessage =
    message("project.noSuchDependency", Long.box(projectId), Long.box(depProjectId))

  def importEdgeSkipped(srcId: String, tgtId: String, grp: Group): BundleMessage =
    message("import.edge.skipped.notFound", srcId, tgtId, grp.entryName)

  def commitValError(msg: String, opIndex: Int): BundleMessage = message("write.commit.valError", msg, Int.box(opIndex))

  def noSuchSynReport(id: Long): BundleMessage = message("project.noSuchSyncReport", Long.box(id))

  def irreversibleCommit: BundleMessage                 = message("write.reverse.irreversibleCommit")
  def irreversibleOpType(opType: String): BundleMessage = message("write.reverse.irreversibleOpType", opType)
  def nodeConflict(name: UUID): BundleMessage           = message("write.reverse.nodeConflict", name.toString)
  def edgeConflict(name: UUID): BundleMessage           = message("write.reverse.edgeConflict", name.toString)
end AuthoringBundle
