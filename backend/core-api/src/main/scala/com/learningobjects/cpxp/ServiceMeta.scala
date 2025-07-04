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

package com.learningobjects.cpxp

/** Information about the application and the host upon which it is running.
  *
  * This currently also combines infrastructure to maintain the identity of the DAS / central host. That does not belong
  * here at all. That should be fixed.
  */
trait ServiceMeta:
  val CLUSTER_NAME_LOCAL         = "local"
  val CLUSTER_TYPE_LOCAL         = "Local"
  val CLUSTER_TYPE_UNKNOWN       = "Unknown"
  val CLUSTER_TYPE_PRODUCTION    = "Production"
  val CLUSTER_TYPE_PERFORMANCE   = "Perf"
  val CLUSTER_TYPE_CERTIFICATION = "Cert"
  val CLUSTER_TYPE_PATCH         = "Patch" // aka pre-prod

  def getVersion: String

  def getBuild: String

  def getBranch: String

  def getRevision: String

  def getRevisionLink: String

  def getBuildDate: String

  def getBuildNumber: String

  def getJvm: String

  def getJava: String

  def pekkoSingleton: Boolean

  def isDas: Boolean

  def getLocalName: String

  def getLocalHost: String

  def getCentralHost: String

  def getPort: Int

  def getCluster: String

  def getClusterType: String

  /** Is this a local development environment, probably someone's dev machine. */
  def isLocal: Boolean

  /** Is this an actual production environment. Prefer to use {#isProdLike}. */
  def isProduction: Boolean

  /** Is this a production-like environment, meaning the system should behave as if production. */
  def isProdLike: Boolean

  def getStaticHost: String

  def getStaticSuffix: String

  def getNode: String

  def getPonyHerd: String
end ServiceMeta
