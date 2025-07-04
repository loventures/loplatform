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

package loi.cp.aws

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.aws.AwsService
import loi.cp.config.ConfigurationService

@Service
class AwsServiceImpl(implicit cf: ConfigurationService) extends AwsService:

  /** Should S3 failover to the replica bucket, if configured. */
  override def s3Failover: Boolean = AwsConfiguration.getSystem.s3Failover

  /** Should cloudfront CDN be disabled. */
  override def cfDisabled: Boolean = AwsConfiguration.getSystem.cfDisabled
