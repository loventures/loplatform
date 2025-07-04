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

package com.learningobjects.cpxp.startup

import cats.effect.unsafe.implicits.global
import com.learningobjects.cpxp.component.registry.Bound

/** A startup task is run during system startup or domain creation and is responsible for setting up or upgrading data
  * in the database.
  *
  * An implementation of this interface must be annotated with {StartupTaskBinding} to provide metadata about the task.
  */
@Bound(classOf[StartupTaskBinding])
trait StartupTask extends Runnable

trait CatsIOStartupTask extends StartupTask:
  protected def action: cats.effect.IO[Unit]

  override final def run(): Unit = action.unsafeRunSync()
