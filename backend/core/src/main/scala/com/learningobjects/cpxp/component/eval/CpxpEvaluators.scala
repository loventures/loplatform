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

package com.learningobjects.cpxp.component.eval

import com.learningobjects.cpxp.CpxpClasspath
import scalaz.Equal
import scaloi.syntax.OptionOps.*

import java.lang.annotation.Annotation
import scala.jdk.CollectionConverters.*

/** A container for known `Evaluator` types which declare their evaluated annotations.
  */
object CpxpEvaluators:

  private lazy val EvaluatorMap: Map[Class[? <: Annotation], Class[? <: Evaluator]] =
    (for
      annotated <- CpxpClasspath.classGraph.getClassesWithAnnotation(classOf[Evaluates]).loadClasses().asScala
      target    <- annotated.getAnnotation(classOf[Evaluates]).value()
      if classOf[Evaluator].isAssignableFrom(annotated)
    yield target -> annotated.asSubclass(classOf[Evaluator])).toMap

  /** Finds an evaluator for `ann`. */
  def knownEvaluator(ann: Class[? <: Annotation]): Option[Evaluator] =
    Option(ann.getAnnotation(classOf[Evaluate]))
      .flatMap(ev => Some[Class[? <: Evaluator]](ev.value) - classOf[Evaluator])
      .orElse(EvaluatorMap.get(ann))
      .map(_.getDeclaredConstructor().newInstance())

  private implicit def equalClass[C <: Class[?]]: Equal[C] = Equal.equalRef[C]
end CpxpEvaluators
