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

package de.changelog

import cats.effect.Sync
import scalaz.std.list.*
import scalaz.std.string.*
import scaloi.syntax.monadPlus.*

import scala.sys.process.*

private[changelog] object Git:
  private final val logger = org.log4s.getLogger

  def getMerges[F[_]: Sync](from: String, to: String): F[List[String]] =
    Sync[F] delay {
      val cmd = s"git log ^$to $from --no-merges --pretty=%D"
      logger info cmd
      cmd.lazyLines.toList.filterNZ
        .map(_.replaceAll(",.*", "").stripPrefix("origin/"))
    }

  def getMergesFromCommitMessages[F[_]: Sync](from: String, to: String): F[List[String]] =
    Sync[F] delay {
      val cmd   = s"git log ^$to $from --pretty=medium"
      logger info cmd
      val lines = cmd.lazyLines.toList
      MergeBranchRe.findAllMatchIn(lines.mkString("\n")).map(_.group(1)).toList.filter(_ != "master")
    }

  def getTags[F[_]: Sync](branch: String): F[List[String]] =
    Sync[F] delay {
      val cmd = s"git tag --sort=-creatordate --merged $branch"
      logger info cmd
      cmd.lazyLines.toList
    }

  def getPony[F[_]: Sync](branch: String): F[String] =
    Sync[F] delay {
      val cmd  = s"git show $branch:backend/pony.txt"
      logger info cmd
      val pony = cmd.lazyLines.mkString
      logger info s"Pony $pony"
      pony
    }

  private final val MergeBranchRe =
    """Pull request #\d+:.*?\n\s*\n\s*Merge in DE/bfr from (\S+) to \S+""".r
end Git
