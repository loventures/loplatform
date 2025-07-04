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

package com.learningobjects.cpxp.util

import _root_.scala.util.Random

import scalaz.syntax.std.boolean.*

object Lorem:

  object Ipsum:
    private val rando = new Random

    private def randoWord = words(rando.nextInt(words.length))

    private def randoPunct = rando.nextBoolean().fold(';', '.')

    def please: LazyList[String] =
      val n = (rando.nextGaussian() * 7 + 7).max(0).toInt
      val k = rando.nextInt(words.length - n)
      randoWord.capitalize #:: words
        .slice(k, k + n)
        .to(LazyList) #::: (randoWord + randoPunct) #:: please

    def apply(n: Int): String =
      (please.take(n - 1) :+ randoWord).mkString("", " ", ".")
  end Ipsum

  private val words =
    """lorem ipsum dolor sit amet consectetur adipiscing elit
      |sed do eiusmod tempor incididunt ut labore et dolore magna aliqua
      |ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat
      |duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur
      |excepteur sint occaecat cupidatat non proident
      |sunt in culpa qui officia deserunt mollit anim id est laborum
    """.stripMargin.replace('\n', ' ').split(' ')
end Lorem
