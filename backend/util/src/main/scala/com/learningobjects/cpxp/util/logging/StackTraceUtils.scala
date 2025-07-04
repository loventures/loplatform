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

package com.learningobjects.cpxp.util.logging

import java.lang.Thread.currentThread

object StackTraceUtils:

  /** Produce a slightly less prolix traceback from the given stack trace elements.
    *
    * @param trace
    *   the raw stack trace
    * @return
    *   the stack trace, formatted on one line, with some amount of noise excised.
    */
  def toShortStackTrace(trace: Array[StackTraceElement]): String =
    trace
      .to(List)
      .filterNot(e => IgnorePrefixen exists e.getClassName.startsWith)
      .map { case StackTraceElement(cn, mn, fn, ln) =>
        s"$cn.$mn ($fn:$ln)"
      }
      .mkString(" <- ")

  /** Produce a short stack trace for the current thread.
    *
    * @see
    *   `toShortStackTrace`
    * @return
    *   a stack trace, formatted on one line, with some amount of noise excised.
    */
  def shortStackTrace: String =
    toShortStackTrace(currentThread().getStackTrace)

  /** The packages to drop from the short stack trace. */
  private val IgnorePrefixen = Set(
    /* framework crap */
    "com.learningobjects.cpxp.",
    /* java / jvm crap */
    "com.sun.",
    "sun.",
    "java.",
    "javax.",
    /* scala crap */
    "scala.",
    "scalaz.",
    /* misc crap */
    "org."
  )

  /** java.lang.StackTraceElement associate. */
  object StackTraceElement:

    /** Extract a 4-tuple of (class name, method name, file name, line number) from a stack trace element.
      */
    def unapply(ste: StackTraceElement): Option[(String, String, String, Int)] =
      Some((ste.getClassName, ste.getMethodName, ste.getFileName, ste.getLineNumber))
end StackTraceUtils
