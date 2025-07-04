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

package loi.cp.script

import enumeratum.*
import org.apache.commons.lang3.StringUtils

import java.util.regex.Pattern

private[script] case class SqlStatement private (
  commandType: SqlStatement.CommandType,
  args: List[String]
)

private[script] object SqlStatement:
  import SqlCharState.*

  private[script] def statements(script: String): List[SqlStatement] =
    import SqlStatement.CommandType.*

    val s1   = if script != null then script.trim else ""
    val toks = if !StringUtils.isAllBlank(s1) then tokens(s1) else Nil
    toks.map { tok =>
      val (commandType, matcher) = commandMatcher(tok)
      val args                   = commandType match
        case Format        => List(matcher.group(2) + "\n", matcher.group(3))
        case Download      => List(matcher.group(1))
        case Describe      => List(matcher.group(1))
        case ListRelations =>
          val types = List(
            Option(matcher.group(1)).filterNot(_.isEmpty).getOrElse("tsv")
          )
          val rels  = Option(matcher.group(3))
            .map(_.split("\\s+").toList)
            .getOrElse(Nil)
          types ++ rels
        case _             => Nil
      SqlStatement(
        commandType = commandType,
        args = List(tok) ++ args
      )
    }
  end statements

  private[script] def listRelationsQuery(types: String, relNames: Seq[String]) =
    val relKinds = types.toSeq.distinct
      .map {
        case 't' => "'r'"
        case 's' => "'S'"
        case c   => s"'$c'"
      }
      .mkString(", ")

    val relsClause =
      if relNames.nonEmpty then
        s"AND c.relname IN (${ //
          (1 to relNames.size).map(_ => '?').mkString(", ")})"
      else ""

    s"""
       |SELECT
       |  n.nspname                              AS "Schema",
       |  c.relname                              AS "Name",
       |  CASE c.relkind
       |    WHEN 'r' THEN 'table'
       |    WHEN 'v' THEN 'view'
       |    WHEN 'i' THEN 'index'
       |    WHEN 'S' THEN 'sequence'
       |    WHEN 's' THEN 'special'
       |  END                                    AS "Type",
       |  pg_catalog.pg_get_userbyid(c.relowner) AS "Owner"
       |FROM
       |  pg_catalog.pg_class c
       |  LEFT JOIN pg_catalog.pg_namespace n
       |            ON n.oid = c.relnamespace
       |WHERE c.relkind in ($relKinds)
       |  AND n.nspname <> 'pg_catalog'
       |  AND c.relkind NOT IN ('i')
       |  AND n.nspname <> 'information_schema'
       |  AND n.nspname !~ '^pg_toast'
       |  AND pg_catalog.pg_table_is_visible(c.oid)
       |  $relsClause
       |ORDER BY 1,2
    """.stripMargin
  end listRelationsQuery

  private[script] val TableInfoQuery =
    s"""
       |WITH table_id AS
       |  (
       |    SELECT
       |      c.oid
       |    FROM
       |      pg_catalog.pg_class c
       |      LEFT JOIN pg_catalog.pg_namespace n
       |                ON n.oid = c.relnamespace
       |    WHERE c.relname = ?
       |      AND pg_catalog.pg_table_is_visible(c.oid)
       |  )
       |SELECT
       |  a.attname                                       AS "Column",
       |  pg_catalog.format_type(a.atttypid, a.atttypmod) AS "Type",
       |  CASE
       |    WHEN a.attnotnull
       |      THEN 'not null'
       |    ELSE NULL
       |  END                                             AS "Modifiers"
       |FROM
       |  pg_catalog.pg_attribute a
       |  JOIN table_id i
       |       ON a.attrelid = i.oid
       |WHERE a.attnum > 0
       |  AND NOT a.attisdropped
       |ORDER BY a.attnum
     """.stripMargin

  private[script] val IndexQuery =
    s"""
       |SELECT
       |  '  ' || d.indexdef
       |FROM
       |  pg_indexes d
       |  JOIN pg_catalog.pg_class c
       |       ON d.indexname = c.relname
       |WHERE d.tablename = ?
       |ORDER BY c.oid
     """.stripMargin

  private[script] val ForeignKeyQuery =
    s"""
       |SELECT
       |    '  FK("' || tc.constraint_name || '") = ' || tc.table_name || '.' ||
       |    kcu.column_name || ' -> ' || ccu.table_name || '.' ||
       |    ccu.column_name
       |FROM
       |  information_schema.table_constraints AS tc
       |  JOIN information_schema.key_column_usage AS kcu
       |       ON tc.constraint_name = kcu.constraint_name
       |         AND tc.table_schema = kcu.table_schema
       |  JOIN information_schema.constraint_column_usage AS ccu
       |       ON ccu.constraint_name = tc.constraint_name
       |         AND ccu.table_schema = tc.table_schema
       |WHERE constraint_type = 'FOREIGN KEY'
       |  AND tc.table_name = ?
     """.stripMargin

  private[script] val ReferencedByQuery =
    s"""
     |SELECT
     |    '  FK("' || tc.constraint_name || '") = ' || tc.table_name || '.' ||
     |    kcu.column_name || ' -> ' || ccu.table_name || '.' ||
     |    ccu.column_name
     |FROM
     |  information_schema.table_constraints AS tc
     |  JOIN information_schema.key_column_usage AS kcu
     |       ON tc.constraint_name = kcu.constraint_name
     |         AND tc.table_schema = kcu.table_schema
     |  JOIN information_schema.constraint_column_usage AS ccu
     |       ON ccu.constraint_name = tc.constraint_name
     |         AND ccu.table_schema = tc.table_schema
     |WHERE constraint_type = 'FOREIGN KEY'
     |  AND ccu.table_name = ?
     """.stripMargin

  private def commandMatcher(tok: String) =
    CommandType.values.flatMap { cType =>
      val matcher = cType.pattern.matcher(tok)
      if matcher.matches() then Some((cType, matcher))
      else None
    }.head

  private def tokens(script: String) =
    val commentLess = strings(
      str = script,
      start = (prev, curr) => prev == Comment && curr != Comment,
      end = (prev, curr) => prev == PreComment && curr == Comment,
      addIfFinal = _ != Comment,
      endIndex = _ - 1
    ).mkString(" ").replaceAll("\\s*\n\\s*", " ").trim

    // separate by ;
    strings(
      str = commentLess,
      start = (prev, curr) => prev == Separator && curr != Separator,
      end = (prev, curr) => prev != Separator && prev != Start && curr == Separator,
      addIfFinal = _ != Separator
    )
  end tokens

  private def strings(
    str: String,
    start: RangeCond,
    end: RangeCond,
    addIfFinal: SqlCharState => Boolean,
    endIndex: Int => Int = i => i
  ) =
    val acc: RangeAcc = str.iterator.zipWithIndex.foldLeft(RangeAcc()) { case (prev, (c, i)) =>
      val curr = prev.copy(
        state = prev.state.next(c),
        start = if prev.state == Start then i else prev.start
      )
      if start(prev.state, curr.state) then curr.copy(start = i)
      else if end(prev.state, curr.state) then curr.copy(ranges = prev.ranges :+ ((prev.start, endIndex(i))))
      else curr
    }

    val ranges = (
      if addIfFinal(acc.state) then acc.ranges :+ ((acc.start, str.length))
      else acc.ranges
    ).filter { case (s, e) => s > -1 && s < e }

    ranges
      .map { case (s, e) => str.substring(s, e).trim() }
      .filterNot(StringUtils.isAllBlank(_))
      .distinct
  end strings

  private[script] sealed abstract class CommandType(
    override val entryName: String,
    val pattern: Pattern,
    val help: Option[String] = None // None -> not listed by \?
  ) extends EnumEntry

  private[script] object CommandType extends Enum[CommandType]:
    import java.util.regex.Pattern.*
    import scala.collection.immutable.IndexedSeq

    private val ResQuery   = "select|with"
    private val RegexFlags = MULTILINE | DOTALL

    override lazy val values: IndexedSeq[CommandType] = findValues

    case object Help
        extends CommandType(
          entryName = "\\?",
          pattern = compile(raw"^\\(h|\?)\s*$$"),
          help = Some("Lists available commands\n")
        )

    case object Download
        extends CommandType(
          entryName = "\\download [QUERY]",
          pattern = compile(raw"^\\download\s+((?i)($ResQuery).+)$$", RegexFlags),
          help = Some("Redirect row results to file download")
        )

    case object Format
        extends CommandType(
          entryName = "\\fmt [FORMAT] [QUERY]",
          pattern = compile(
            raw"^\\fmt\s+(${'"'}(.+?)${'"'})\s+((?i)($ResQuery).+)$$",
            RegexFlags
          ),
          help = Some(
            s"""Print out results using column format. For example:
               |
               |$pad\\fmt \"%10s | %s\" SELECT id, groupid FROM groupfinder LIMIT 10;
               |
               |${pad}All placeholders must be of type %s\n
             """.stripMargin
          )
        )

    case object Describe
        extends CommandType(
          entryName = "\\d [TABLE]",
          pattern = compile(raw"^\\d\s+(.+)$$", CASE_INSENSITIVE),
          help = Some("Describe table. e.g. \\d userfinder or \\d \"table name\"")
        )

    case object ListRelations
        extends CommandType(
          entryName = "\\d[S+]",
          pattern = compile(raw"^\\d([tsv]*)(\s+(.+))?$$"),
          help = Some(
            s"""List (t)ables, (s)equences, (v)iews. For example:
              |
              |$pad\\d or \\dt or \\dsv or \\dt groupfinder userfinder
             """.stripMargin
          )
        )

    case object Explain
        extends CommandType(
          entryName = "explain",
          pattern = compile("^explain.+$", RegexFlags | CASE_INSENSITIVE)
        )

    case object ResultsQuery
        extends CommandType(
          entryName = "results",
          pattern = compile(s"^($ResQuery).+$$", RegexFlags | CASE_INSENSITIVE)
        )

    case object UpdateQueryReturning
        extends CommandType(
          entryName = "update",
          pattern = compile("^\\s*update\\s[^']*('[^']*'[^']*)*\\sreturning\\s.+$", RegexFlags | CASE_INSENSITIVE)
        )

    case object UpdateQuery
        extends CommandType(
          entryName = "update",
          pattern = compile("^.+$", RegexFlags)
        )

    val EntryColLen      = 26
    private lazy val pad = (1 to EntryColLen).map(_ => ' ').mkString("")
  end CommandType

  private case class RangeAcc(
    ranges: List[(Int, Int)] = Nil,
    state: SqlCharState = Start,
    start: Int = -1
  )

  private type RangeCond = (SqlCharState, SqlCharState) => Boolean
end SqlStatement
