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

package com.learningobjects.cpxp.component.util

import com.fasterxml.jackson.annotation.JsonProperty

/** This trait is used to represent a lazy sequence result that is only computed during serialization. Typically this is
  * used to represent a lazily-computed cached SRS response. This serializes in the standard SRS structural form.
  */
trait LazySeq[A]:

  /** Get the size of the sequence. */
  @JsonProperty
  def getCount: Int

  /** Get the sequence of values. */
  @JsonProperty
  def getObjects: Seq[A]

object LazySeq:

  /** Create a lazy sequence from a generator. */
  def apply[A](f: => Seq[A]) = new LazySeq[A]:
    private lazy val seq: Seq[A] = f

    override def getCount = seq.size

    override def getObjects = seq
