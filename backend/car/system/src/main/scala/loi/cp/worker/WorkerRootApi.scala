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

package loi.cp.worker

import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.{Component, Controller, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}

import scala.jdk.CollectionConverters.*

/* TODO: akkify this to aggregate infos */

@Component
@RequestMapping(path = "workers")
@Controller(value = "workers", root = true)
class WorkerRootApi(val componentInstance: ComponentInstance)(
  // env: ComponentEnvironment,
) extends ApiRootComponent
    with ComponentImplementation:
  import WorkerRootApi.*

  @RequestMapping(method = Method.GET)
  def getWorkerStatus: List[WorkerStatus] =
    workers.map { aw =>
      val qs = aw.queueSnapshot
      WorkerStatus(
        name = aw.getClass.getSimpleName,
        queueSize = qs.values.flatten.size,
        queue = qs,
      )
    }

  /* TODO: this has *got* to be doable betterly */
  private def workers: List[AbstractWorker] =
    env.getComponents.asScala.view
      .map(_.getComponentClass)
      .filter(classOf[AbstractWorker] `isAssignableFrom` _)
      .map(ComponentSupport.lookupService(env, _))
      .map(_.asInstanceOf[AbstractWorker])
      .toList

  private def env = ComponentManager.getComponentEnvironment
end WorkerRootApi

object WorkerRootApi:

  final case class WorkerStatus(
    name: String,
    queueSize: Int,
    queue: Map[Long, List[Long]],
  )
