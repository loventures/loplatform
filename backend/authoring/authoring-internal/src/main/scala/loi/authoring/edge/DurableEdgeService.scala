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

package loi.authoring.edge

import cats.syntax.option.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import loi.authoring.branch.Branch
import loi.authoring.edge.DurableEdgeService.ReplaceNames
import loi.authoring.edge.store.DurableEdgeDao2
import loi.authoring.project.ProjectEntity2
import loi.authoring.workspace.AttachedReadWorkspace
import loi.authoring.write.*
import org.hibernate.Session

import java.util.UUID
import scala.collection.mutable
@Service
class DurableEdgeService(
  durableEdgeDao2: DurableEdgeDao2,
  session: => Session
):

  def selectAllNames(ws: AttachedReadWorkspace): Map[DurableEdge.Key, UUID] =
    durableEdgeDao2
      .selectAll(ws.bronchId)
      .view
      .map(e => DurableEdge.Key(e.sourceName, e.targetName, Group.withName(e.group)) -> e.name)
      .toMap

  def selectNames(keys: Iterable[DurableEdge.Key], ws: AttachedReadWorkspace): Map[DurableEdge.Key, UUID] =
    if keys.isEmpty then Map.empty
    else durableEdgeDao2.selectNames(keys, ws.bronchId)

  /** Copies all the durableedge rows for `srcBranch`. The new rows will use `tgtBranch` as branch_id */
  def copy(srcBranch: Branch, tgtBranch: Branch): Unit =
    val srcProjectId = srcBranch.id // how it is in laird (branch is fake)
    val tgtProjectId = tgtBranch.id
    val tgtProject   = session.ref[ProjectEntity2](tgtProjectId)

    durableEdgeDao2
      .selectAll(srcProjectId)
      .foreach(e => durableEdgeDao2.create(e, tgtProject))

  // two things this does:
  //   * replace AddEdge name with durable name and cascade durable name to rest of ops
  //   * replace AddEdge name with name from the first occurrence of the durable key even if there
  //     is not a durable name and cascade the first name to rest of ops
  //
  // illustrating the second point (group elided for brevity):
  //   List(AddEdge(a, b, name = ab), AddEdge(a, b, name = ac), SetEdgeData(ac, ...)) becomes
  //   List(AddEdge(a, b, name = ab), AddEdge(a, b, name = ab), SetEdgeData(ab, ...)
  // even when Key(a, b) is not in authoringdurableedge
  //
  // aside: the corrected list illustrated above is valid in Folding Commit land.
  // the second AddEdge(a, b) attributes overwrite those of the first, only one ab is added.
  def replaceNames(ws: AttachedReadWorkspace, ops: List[WriteOp]): ReplaceNames =

    val keys         = ops.view.collect({ case op: AddEdge => DurableEdge.Key.from(op) }).toSet
    val durableNames = selectNames(keys, ws)

    val firstNames   = mutable.Map.empty[DurableEdge.Key, UUID]
    val replacements = mutable.Map.empty[UUID, UUID]

    val nextOps = ops map {
      case op: AddEdge =>
        val key = DurableEdge.Key.from(op)

        (durableNames.get(key), firstNames.get(key)) match
          case (Some(durName), _) if op.name != durName        => replacements.update(op.name, durName)
          case (None, Some(firstName)) if op.name != firstName => replacements.update(op.name, firstName)
          case (None, None)                                    => firstNames.update(key, op.name)
          case _                                               =>

        namesReplaced(op, replacements)

      case op: SetEdgeData  => namesReplaced(op, replacements)
      case op: SetEdgeOrder => namesReplaced(op, replacements)
      case op: DeleteEdge   => namesReplaced(op, replacements)
      case op               => op
    }

    ReplaceNames(nextOps, replacements.toMap, firstNames.values.toSet)
  end replaceNames

  // These methods aren't on the WriteOp ADT because I consider it spam.

  // `collection.Map` is super type of `collection.immutable.Map` and `collection.mutable.Map`
  private def namesReplaced(op: AddEdge, replacements: collection.Map[UUID, UUID]): AddEdge =
    val partly = replacements.get(op.name).map(r => op.copy(name = r)).getOrElse(op)
    op.position.map(namesReplaced(replacements)).map(r => partly.copy(position = r.some)).getOrElse(partly)

  private def namesReplaced(replacements: collection.Map[UUID, UUID])(pos: Position): Position = pos match
    case p: Position.Before => replacements.get(p.edgeName).map(r => p.copy(edgeName = r)).getOrElse(p)
    case p: Position.After  => replacements.get(p.edgeName).map(r => p.copy(edgeName = r)).getOrElse(p)
    case p                  => p

  private def namesReplaced(op: SetEdgeData, replacements: collection.Map[UUID, UUID]): SetEdgeData =
    replacements.get(op.name).map(r => op.copy(name = r)).getOrElse(op)

  private def namesReplaced(op: SetEdgeOrder, replacements: scala.collection.Map[UUID, UUID]): SetEdgeOrder =
    op.copy(ordering = op.ordering.map(name => replacements.getOrElse(name, name)))

  private def namesReplaced(op: DeleteEdge, replacements: collection.Map[UUID, UUID]): DeleteEdge =
    replacements.get(op.name).map(r => op.copy(name = r)).getOrElse(op)
end DurableEdgeService

object DurableEdgeService:

  /** @param ops
    *   same as input ops but with applicable names replaced with durable names
    * @param replaced
    *   requested name -> durable name
    * @param create
    *   the AddEdge `op.name`s that need to create a durable edge row
    */
  case class ReplaceNames(
    ops: List[WriteOp],
    replaced: Map[UUID, UUID],
    create: Set[UUID],
  )
end DurableEdgeService
