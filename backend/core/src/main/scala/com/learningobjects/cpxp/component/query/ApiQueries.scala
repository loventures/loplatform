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

package com.learningobjects.cpxp.component.query

import com.learningobjects.cpxp.component.{ComponentInterface, DataModel}
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.query.QueryBuilder
import scaloi.syntax.annotation.*
import scaloi.syntax.classTag.*

import java.lang.Boolean as JBoolean
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional
import scala.reflect.ClassTag

/** API query-related miscellania.
  */
object ApiQueries:

  /** Execute an API query over a component with implicit data model evidence.
    *
    * @param qb
    *   the query builder
    * @param query
    *   the API query
    * @tparam T
    *   the expected component interface
    * @return
    *   the query results
    */
  def query[T <: ComponentInterface: ClassTag: DataModel](qb: QueryBuilder, query: ApiQuery): ApiQueryResults[T] =
    ApiQuerySupport.query(qb, query.withPropertyMappings[T], classTagClass[T])

  def queryFinder[T <: Finder: ClassTag: DataModel](
    qb: QueryBuilder,
    query: ApiQuery,
  ): ApiQueryResults[T] =
    ApiQuerySupport.queryFinder(query.withPropertyMappings[T], qb, classTagClass[T])

  def queryBuilder[T <: Finder: ClassTag: DataModel](
    qb: QueryBuilder,
    query: ApiQuery,
  ): QueryBuilder =
    ApiQuerySupport.getQueryBuilder(qb, query.withPropertyMappings[T], true)

  /** Enhancements on API queries.
    *
    * @param self
    *   the API query being enhanced
    */
  implicit class ApiQueryOps(val self: ApiQuery) extends AnyVal:

    /** Return a copy of an API query augmented with property mappings.
      *
      * @tparam T
      *   the component type
      * @return
      *   an augmented API query
      */
    def sublet[T: ClassTag]: ApiQuery =
      new ApiQuery.Builder(self).addPropertyMappings(classTagClass[T]).build

    /** Return a copy of an API query augmented with property mappings (if not already present) and data model
      * information.
      *
      * @tparam T
      *   the data model evidenced component type
      * @return
      *   an augmented API query
      */
    def withPropertyMappings[T: ClassTag: DataModel]: ApiQuery =
      if self.getPropertyMappings.isEmpty then
        new ApiQuery.Builder(self).addPropertyMappings(classTagClass[T]).build.withDataModel[T]
      else self.withDataModel[T]

    /** Return a copy of an API query augmented with data model information.
      *
      * @tparam T
      *   the data model evidenced component type
      * @return
      *   an augmented API query
      */
    def withDataModel[T: DataModel]: ApiQuery =
      val aqb         = new ApiQuery.Builder(self)
      val dataModel   = implicitly[DataModel[T]]
      val itemMapping =
        Option(self.getItemMapping).getOrElse(ItemMappingAnnotation.DEFAULT)
      aqb.setItemMapping(
        itemMapping ++ Map(
          "value"        -> dataModel.itemType,
          "singleton"    -> JBoolean.valueOf(dataModel.singleton),
          "schemaMapped" -> JBoolean.valueOf(dataModel.schemaMapped)
        )
      )
      dataModel.dataTypes foreach { entry =>
        applyDataModel(aqb, entry._1, entry._2, "dataType")
      }
      dataModel.handlers foreach { entry =>
        applyDataModel(aqb, entry._1, entry._2, "handler")
      }
      aqb.build
    end withDataModel

    def withPrefilter(filter: ApiFilter): ApiQuery =
      val aqb = new ApiQuery.Builder(self)
      aqb.addPrefilter(filter)
      aqb.build()

    def partitionFilter(name: String): (Option[ApiFilter], ApiQuery) =
      val aqb = new ApiQuery.Builder(self)
      aqb.removeFilter(name).toScala -> aqb.build

    /** Apply data model information to an API query builder. This augments any existing query annotations about a
      * particular property with information from the underlying data model, such as the underlying data type.
      *
      * @param aqb
      *   the API query builder
      * @param property
      *   the property name
      * @param value
      *   the data model value
      * @param attr
      *   the underlying annotation attribute name
      * @tparam A
      *   the data model type
      */
    private def applyDataModel[A <: AnyRef](aqb: ApiQuery.Builder, property: String, value: A, attr: String): Unit =
      Option(self.getPropertyMapping(property)) foreach { mapping =>
        aqb.addPropertyMapping(property, mapping ++ Map(attr -> value))
      }
  end ApiQueryOps

  implicit class ApiQueryResultsOps[A](apiQueryResults: ApiQueryResults[A]):

    /** Applies a function f to each item in these results, returning a new results object with the modified objects
      *
      * @param f
      * @tparam B
      * @return
      */
    def map[B](f: A => B): ApiQueryResults[B] =
      new ApiQueryResults[B](
        apiQueryResults.asScala.map(f).asJava,
        apiQueryResults.getFilterCount,
        apiQueryResults.getTotalCount
      )
  end ApiQueryResultsOps
end ApiQueries
