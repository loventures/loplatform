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

import loi.authoring.AssetType

object TraverseGraph:

  /** Describes loading all edges from `src` sorted by group name then position.
    */
  def fromSource[A](src: A): FromSources[A] = fromSources(src)

  /** Describes loading all edges from `srcs` sorted by source, group name, then position
    */
  def fromSources[A](srcs: A*): FromSources[A] = FromSources(srcs)

// ---------- ---------- ---------- ---------- ---------- ---------- ----------
// These traits help determine which terminators are available for the command
// ---------- ---------- ---------- ---------- ---------- ---------- ----------

/** Describes loading out edges where the target type is unknown at compile time or there is more than one.
  */
sealed trait LoadOutEdgesAnyTargetType[+A]:
  def sources: Seq[A]

/** Like [[LoadOutEdgesAnyTargetType]] but for when we are traversing, not loading direct children. Traversing means
  * that the `.traversedGraph` interpreter is available.
  */
sealed trait TraverseOutEdgesAnyTargetType[+A]

/** Describes loading out edges where all target node types should be `T`. How edges not to `T` are handled is left to
  * the interpreters.
  */
sealed abstract class LoadOutEdges[+A, T: AssetType]

// ---------- ---------- ---------- ---------- ---------- ---------- ----------

/** Describes loading all edges from `sources` sorted by source, group name, then position
  */
case class FromSources[A](sources: Seq[A]) extends LoadOutEdgesAnyTargetType[A]:

  /** Describes loading edges from `sources` whose group name is one of `groups`
    */
  def traverse(groups: Group*): FromSourcesInGroups[A] =
    FromSourcesInGroups(sources, groups)

  /** Describes loading edges from `sources` whose target node type is `T`
    */
  def traverse[T: AssetType]: FromSourcesToType[A, T] =
    FromSourcesToType(sources)
end FromSources

/** Describes loading edges from `sources` whose group name is one of `groups`
  */
case class FromSourcesInGroups[A](sources: Seq[A], groups: Seq[Group]) extends LoadOutEdgesAnyTargetType[A]:

  def traverse(groups: Group*): TraverseFromSourcesAnyTargetType[A] =
    TraverseFromSourcesAnyTargetType(sources, Seq(GroupStep(this.groups), GroupStep(groups)))

  def noFurther: TraverseFromSourcesAnyTargetType[A] =
    TraverseFromSourcesAnyTargetType(sources, Seq(GroupStep(this.groups)))

/** Describes traversing edges from `sources` in multiple steps
  */
case class TraverseFromSourcesAnyTargetType[+A](
  sources: Seq[A],
  steps: Seq[Step]
) extends TraverseOutEdgesAnyTargetType[A]:

  def traverse(groups: Group*): TraverseFromSourcesAnyTargetType[A] = copy(steps = steps :+ GroupStep(groups))

/** Describes loading edges from `sources` whose target node type is `T`
  */
case class FromSourcesToType[A, T: AssetType](sources: Seq[A]) extends LoadOutEdges[A, T]

/** A step describes which edges to traverse
  */
sealed trait Step

/** Describes the edges with these group names
  */
case class GroupStep(groups: Seq[Group]) extends Step
