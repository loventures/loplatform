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

package loi.cp

/** The root package of the DE worker machinery.
  *
  * We have a recurring need to do things in the background, not attached to any Tomcat request handler thread. We also
  * need once-and-only-once semantics, or at least as close to them as we can practically get, so the `Executor` queue
  * is not a good fit. These requirements come up in relation to many and varied product features, such as course
  * provisioning, notifications, and email. The worker machinery is an attempt to bring a small amount of code reuse to
  * bear on this problem.
  *
  * Each worker consists of a pair of threads: a `Poll` thread that periodically polls the database to acquire and
  * enqueue new work and a `Queue` thread that pops work items from the shared queue and executes them. The `Poll`er
  * should atomically update the work items to mark them as "enqueued", and should also periodically resurrect abandoned
  * work. The `Queue` thread periodically marks the work items as still enqueued, to prevent their work from being
  * stolen by another appserver.
  *
  * See the documentation on the abstract methods in `AbstractWorker` for detail on the implementation requirements.
  */
package object worker
