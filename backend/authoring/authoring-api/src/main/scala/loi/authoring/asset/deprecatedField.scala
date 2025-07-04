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

package loi.authoring.asset

/** Don't use the annotated field. It does not function as intended and may eventually be removed. It may be set to
  * Option and might be called by legacy APIs. e.g. heavyweight
  *
  * Originally we wanted to use scala.deprecated but it gives a compile time error (by default it gives a warning).
  */
final class deprecatedField extends scala.annotation.StaticAnnotation
