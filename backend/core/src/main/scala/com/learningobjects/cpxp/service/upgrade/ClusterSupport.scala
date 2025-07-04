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

package com.learningobjects.cpxp.service.upgrade

import com.learningobjects.cpxp.util.ManagedUtils
import com.typesafe.config.ConfigFactory

object ClusterSupport:

  /** Get the distinguished cluster systeminfo object. This is used to track the cluster central host and version, among
    * other things.
    */
  def clusterSystemInfo: SystemInfo =
    ManagedUtils.getEntityContext.getEntityManager.find(classOf[SystemInfo], singletonId)

  final val singletonId = 0L

  final val clusterId = ConfigFactory.load().getString("pekko.discovery.aws-api-ec2-tag-based.filters")
end ClusterSupport
