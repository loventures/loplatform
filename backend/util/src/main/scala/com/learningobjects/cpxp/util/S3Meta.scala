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

/** Metadata about an S3 operation, for statistical purposes. */
case class S3Meta(repeatable: Boolean, size: Option[Long], retryUnknown: Boolean = false)

object S3Meta:

  /** A repeatable request with no content transfer. */
  def repeatableEmpty: S3Meta =
    S3Meta(repeatable = true, size = Some(0L), retryUnknown = true) // retry unknown because of random S3 failures
  /** A repeatable request with bounded content transfer. */
  def repeatableBounded(size: Long): S3Meta =
    S3Meta(repeatable = true, Some(size), retryUnknown = true) // retry unknown because of random S3 failures
  /** An unrepeatable request with bounded content transfer. */
  def unrepeatableBounded(size: Long): S3Meta = S3Meta(repeatable = false, Some(size))
end S3Meta
