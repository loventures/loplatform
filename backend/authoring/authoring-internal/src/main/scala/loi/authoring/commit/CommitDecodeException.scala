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

package loi.authoring.commit

import argonaut.CursorHistory
import com.learningobjects.cpxp.scala.json.ArgoParseException

// thrown by methods on Hibernate Proxies, amongst other places.
// When thrown by a Hibernate proxy, the exception is nested as the `target` of a wrapping `InvocationTargetException`
// When logging these InvocationTargetExceptions, the `target`'s message is elided ?why?
// Our `target`'s message is the most important part, carrying the id of the corrupted authoringcommitdoc.
// Through trial and error, NOT placing a cause on this exception resulted in no elision
// DbTests can't cover this as the exceptions don't show up as an `InvocationTargetException` there /shrug
class CommitDecodeException(id: Long, column: String, argoErr: (String, CursorHistory))
    extends RuntimeException(
      s"authoringcommitdoc.$column failed to decode; id $id; argoMsg: ${ArgoParseException.msg(argoErr._1, argoErr._2)}"
    )

object CommitDecodeException:
  def apply(id: Long, column: String)(argoErr: (String, CursorHistory)) = new CommitDecodeException(id, column, argoErr)
