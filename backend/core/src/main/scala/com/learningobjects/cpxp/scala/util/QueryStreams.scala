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

package com.learningobjects.cpxp.scala.util

import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.dto.Facade
import com.learningobjects.cpxp.service.query.QueryBuilder
import jakarta.persistence.Query

import scala.jdk.CollectionConverters.*
import scala.reflect.*

/** A collection of helpers for building [[Stream]] instances.
  */
object QueryStreams:

  /** Builds a stream from the pagination strategy supplied.
    *
    * <b>getNextPage</b> will be called to fetch the next page of elements until a Seq that is less than <b>limit<b> is
    * returned.
    *
    * Example usage:
    *
    * {{{
    *   queryBuilder.toPagedComponentStream[AssetComponent]()
    *     .take(1000)
    *     .foreach(asset => {
    *       asset
    *         ...
    *     })
    * }}}
    *
    * This will loop through the first 1000 AssetComponents found by queryBuilder (if there are less than 1000, the
    * stream will simply have less). The assets will have been retrieved lazily & paginated according to the supplied
    * parameters.
    *
    * @param limit
    *   the pageSize for elements to return
    * @param start
    *   the offset for where to start this stream
    * @param getNextPage
    *   A function to fetch the next page, where the first parameter is the page number.
    * @return
    *   A stream of instances supplied by getNextPage
    */
  def buildPagedStream[T](limit: Int = 10, start: Int = 0)(getNextPage: Int => Seq[T]): LazyList[T] =
    getNextPage(start) match
      case Nil                       => LazyList.empty
      case seq if seq.length < limit =>
        seq.to(LazyList)
      case seq                       =>
        seq.to(LazyList) #::: buildPagedStream(limit, start + limit)(getNextPage)

  /** Contains helpers for converting [[QueryBuilder]] instances to instances of paginated [[Stream]] s.
    */
  implicit class StreamQueryBuilderOps(qb: QueryBuilder):

    /** Creates a paginated [[Stream]] of instances of [[T]] supplied by this query.
      * @param limit
      *   the page size.
      * @param start
      *   the offset to begin the stream at.
      * @tparam T
      *   the type which to cast results as. If you are selecting multiple columns, this should be Array[Object]
      * @return
      */
    def toPagedResultStream[T: ClassTag](limit: Int = 10, start: Int = 0): LazyList[T] =
      buildPagedStream(limit, start) { from =>
        qb
          .setFirstResult(from)
          .setLimit(limit)
          .getResultList[T]
          .asScala
          .toSeq
      }

    /** Similar to [[toPagedResultStream]], except that you can supply a component type.
      * @tparam T
      *   the component type that this query is for
      * @return
      */
    def toPagedComponentStream[T <: ComponentInterface: ClassTag](limit: Int = 10, start: Int = 0): LazyList[T] =
      buildPagedStream(limit, start) { from =>
        qb
          .setFirstResult(from)
          .setLimit(limit)
          .getComponents[T]
      }

    /** Similar to [[toPagedResultStream]], except that you can supply a facade type.
      * @tparam T
      *   the component type that this query is for
      * @return
      */
    def toPagedFacadeStream[T <: Facade: ClassTag](limit: Int = 10, start: Int = 0): LazyList[T] =
      buildPagedStream(limit, start) { from =>
        qb
          .setFirstResult(from)
          .setLimit(limit)
          .getFacades[T]
      }
  end StreamQueryBuilderOps

  /** Contains helpers for converting [[Query]] instances to instances of paginated [[Stream]] s.
    * @param query
    */
  implicit class StreamQueryOps(query: Query):

    /** Creates a paginated [[Stream]] of instances of [[T]] supplied by this query.
      * @param limit
      *   the page size
      * @param start
      *   the offset to start the stream at
      * @tparam T
      *   the type which to cast results as. If you are selecting multiple columns, this should be Array[Object]
      * @return
      */
    def toPagedStream[T: ClassTag](limit: Int = 10, start: Int = 0): LazyList[T] =
      buildPagedStream(limit, start) { from =>
        getNextPage(limit, from)
      }

    /** Retrieves a page from a query.
      * @param query
      * @param limit
      * @param start
      * @tparam T
      * @return
      */
    def getNextPage[T: ClassTag](limit: Int, start: Int): Seq[T] =
      query
        .setFirstResult(start)
        .setMaxResults(limit)
        .getResultList
        .asInstanceOf[java.util.List[T]]
        .asScala
        .toSeq
  end StreamQueryOps
end QueryStreams
