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

package com.learningobjects.cpxp.component

import scaloi.{ClassMap, ClassMap0}

import java.lang.annotation.Annotation
import java.lang.reflect.AnnotatedElement

package object eval:

  /** A somewhat-reasonably typed map of annotations on an element. */
  type AnnotationInfo = ClassMap0[Annotation]

  def AnnotationInfo(raw: Array[Annotation]): AnnotationInfo =
    raw.foldLeft(ClassMap.empty0[Annotation]) { (cm, next) =>
      cm + (next.annotationType.asInstanceOf[Class[Annotation]] -> next)
    }

  /** Analyze a java-reflection element to get its annotations. */
  def AnnotationInfo(am: AnnotatedElement): AnnotationInfo =
    AnnotationInfo(am.getAnnotations)
end eval
