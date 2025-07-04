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

package com.learningobjects.cpxp.scala.util

import com.learningobjects.cpxp.component.ComponentDescriptor
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.util.InternationalizationUtils

object I18n:

  /** Internationalizes this given string using messages maps in the ComponentRegistry.
    */
  implicit class ComponentMessageInterpolator(val sc: StringContext) extends AnyVal:

    /** Formats message with given key.
      */
    def i18n(args: AnyRef*)(key: String)(implicit componentDescriptor: ComponentDescriptor): String =
      var index     = 0
      def incIndex  =
        val ret = index
        index = index + 1
        ret
      val value     = sc.parts.mkString(s"{$incIndex}")
      val i18nValue =
        ComponentUtils.getMessage(key, value, componentDescriptor)
      InternationalizationUtils.format(i18nValue, args*)
    end i18n

    /** Formats message with message as key.
      */
    def i(args: AnyRef*)(implicit componentDescriptor: ComponentDescriptor): String =
      val key = "label_" + sc.parts
        .mkString(" ")
        .toLowerCase
        .replaceAll("\\s", "_")
      i18n(args*)(key)(using componentDescriptor)
  end ComponentMessageInterpolator
end I18n
