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
import enumeratum.*
import scaloi.syntax.CollectionOps.*

sealed abstract class Group(val cardinality: Option[Int] = None) extends EnumEntry with EnumEntry.Uncapitalised:
  final val tag = entryName.##

// The group name should be ASCII due to database ordering issues on different
// platforms. macOS does not have correct collation tables for UTF-8, but Linux does.
// On macOS 10.11.6, /usr/share/locale/en_US.UTF-8/LC_COLLATE is a symlink to
// ../la_LN.US_ASCII/LC_COLLATE. Our search sorts on string representations of
// edges that include the group name and commas, colons and slashes. The difference in
// precedence of those characters in utf-8 vs ascii causes integration test differences
// because we verify ordering in those tests (e.g. pass on macOS, fail on jenkins). So
// we force POSIX collation everywhere, which is ASCII, so that the collation will
// be the same on local macOS boxes and on Linux boxes. Therefore, it is best to only
// use ASCII representable characters in the group name, lest we have more ordering
// instability across platforms.
object Group extends Enum[Group] with ArgonautEnum[Group]:

  case object Assesses             extends Group
  case object Captions             extends Group
  case object CblRubric            extends Group(1.some)
  case object CompetencySets       extends Group
  case object Courses              extends Group
  case object CssResources         extends Group
  case object Criteria             extends Group
  case object Dependencies         extends Group
  case object Elements             extends Group
  case object Gates                extends Group
  case object GradebookCategories  extends Group
  case object GradebookCategory    extends Group(1.some)
  case object Hyperlinks           extends Group
  case object Image                extends Group(1.some)
  case object InSystemResource     extends Group(1.some)
  case object Instructions         extends Group(1.some)
  case object Level1Competencies   extends Group
  case object Level2Competencies   extends Group
  case object Level3Competencies   extends Group
  case object Poster               extends Group(1.some)
  case object Questions            extends Group
  case object RemediationResources extends Group
  case object Resources            extends Group
  case object Scripts              extends Group
  case object Stylesheets          extends Group
  case object Survey               extends Group(1.some)
  case object Teaches              extends Group
  case object TestsOut             extends Group
  case object Transcript           extends Group(1.some)

  case class Unknown(entryName0: String) extends Group(None)

  override def withName(str: String): Group = Group.withNameOption(str).getOrElse(Unknown(str))

  lazy val values: IndexedSeq[Group] = findValues
  lazy val byTag: Map[Int, Group]    = values.groupUniqBy(_.tag)

  lazy val traverseFalse: Set[Group] = Set(Assesses, Gates, GradebookCategory, Teaches)
end Group
