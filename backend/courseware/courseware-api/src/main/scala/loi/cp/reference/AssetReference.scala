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

package loi.cp.reference

object AssetReference:

  /** a column containing a name of an asset node, for an unversioned reference */
  final val DATA_TYPE_NODE_NAME = "nodeName"

  /** the column holding the id of the particular commit for the asset reference */
  final val DATA_TYPE_COMMIT_ID = "commitId"

  /** the column holding the context reference */
  final val DATA_TYPE_CONTEXT_ID = "contextId"

  /** the column containing the serialized edge path as a string */
  final val DATA_TYPE_EDGE_PATH = "edgePath"
end AssetReference
