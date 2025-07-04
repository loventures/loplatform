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

import com.learningobjects.cpxp.Id as IdObj
import com.learningobjects.cpxp.service.query.{BaseDataProjection, QueryBuilder}

import scala.jdk.CollectionConverters.*

/** A class of convenience implicits for ids (since we use them A LOT)
  */
object Id:
  implicit class IdOps[T <: IdObj](idObj: T):
    def id: Long = idObj.getId.toLong

  implicit class IdsOps[T <: IdObj](idObjs: Iterable[T]):
    def ids: Seq[Long] =
      idObjs.map(_.id).toSeq

    def byId: Map[Long, T] =
      idObjs.toSeq.distinct.map(obj => obj.id -> obj).toMap

  implicit class IdQueryBuilderOps(queryBuilder: QueryBuilder):

    /** Executes the query, retrieving the given {{keyDataType}} and {{valueDataType}} as a map.
      *
      * @param keyDataType
      *   the data type (column) of the key, this should be of type 'item'
      * @param valueDataType
      *   the data type (column) of the value, this should be of type 'item'
      * @return
      *   a {{Map}} of the results of all rows as a map of the {{keyDataType}} to the {{valueDataType}}
      */
    def queryIdToId(keyDataType: String, valueDataType: String): Map[Long, Long] =
      queryBuilder
        .setDataProjection(BaseDataProjection.ofData(keyDataType, valueDataType))
        .getResultList[Any]
        .asScala
        .collect({ case Array(key: Number, value: Number) =>
          (key.longValue, value.longValue)
        })
        .toMap

    /** Executes the query, retrieving the given {{keyDataType}} and {{valueDataType}} as a multi map.
      *
      * @param keyDataType
      *   the data type (column) of the key, this should be of type 'item'
      * @param valueDataType
      *   the data type (column) of the values, this should be of type 'item'
      * @return
      *   a {{Map}} of the results of all rows as a map of the {{keyDataType}} to all {{valueDataType}}s that have that
      *   key
      */
    def queryIdToIds(keyDataType: String, valueDataType: String): Map[Long, Seq[Long]] =
      queryBuilder
        .setDataProjection(BaseDataProjection.ofData(keyDataType, valueDataType))
        .getResultList[Any]
        .asScala
        .collect({ case Array(key: Number, value: Number) =>
          (key.longValue, value.longValue)
        })
        .groupBy(_._1)
        .map(entry => entry._1 -> entry._2.map(_._2).toSeq)
  end IdQueryBuilderOps
end Id
