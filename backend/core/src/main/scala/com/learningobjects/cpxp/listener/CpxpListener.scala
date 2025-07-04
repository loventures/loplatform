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

package com.learningobjects.cpxp.listener

import com.learningobjects.cpxp.service.ServiceContext

import scala.annotation.nowarn

/** A ServiceLoader trait to react to system startup events. */
@nowarn // all parameters are for subclasses to use
trait CpxpListener:

  /** Runs after services, persistence, and pekko have been initialized. */
  def postBootstrap(ctx: ServiceContext): Unit = ()

  /** Runs after the component environment has been loaded. */
  def postComponent(ctx: ServiceContext): Unit = ()

  /** Runs before the component environment is unloaded. */
  def preUncomponent(ctx: ServiceContext): Unit = ()

  /** Runs before services, persistence, and pekko are shut down. */
  def preUnload(ctx: ServiceContext): Unit = ()
end CpxpListener
