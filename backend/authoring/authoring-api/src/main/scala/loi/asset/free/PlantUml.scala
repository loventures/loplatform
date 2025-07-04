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

package loi.asset.free

import cats.data.Writer
import cats.~>
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.{Asset, AssetInfo}
import loi.authoring.edge.AssetEdge

import java.util.{Date, UUID}

object PlantUmlExecutor:

  type PlantUml[A] = Writer[List[String], A]

  implicit class PlantUmlOps[A](plantUml: PlantUml[A]):
    def render() =
      println("@startuml")
      plantUml.run._1.foreach(println)
      println("@enduml")

  implicit class UUIDOps(uUID: UUID):
    def n: String = uUID.toString.replace("-", "")

  def interpreter: AssetGraphInstruction ~> PlantUml =
    new (AssetGraphInstruction ~> PlantUml):
      def apply[A](fa: AssetGraphInstruction[A]): PlantUml[A] =
        fa match
          case a @ AddAsset(data)       =>
            val info = randomInfo
            Writer(
              List(s"""usecase ${info.name.n} as "${a.title}\\n--\\n${a.assetType.dataClass.getSimpleName}""""),
              new Asset(info, a.assetType, a.data)
            )
          case AddEdge(s, t, g, ed, tr) =>
            val edge = AssetEdge(2L, UUID.randomUUID(), UUID.randomUUID(), s, t, g, 0L, tr, null, null, null)
            Writer(List(s"(${s.info.name.n}) --> (${t.info.name.n}) : $g"), edge.asInstanceOf[A])
          case PutBlob(_, _, _)         =>
            Writer(Nil, null) // ohmai

  def randomInfo =
    AssetInfo(
      1L,
      UUID.randomUUID(),
      AssetTypeId.Audio,
      new Date(),
      None,
      new Date(),
      archived = false,
    )
end PlantUmlExecutor
