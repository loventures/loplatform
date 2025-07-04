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

package com.learningobjects.cpxp.service.query

import java.lang as jl
import java.util.{List, Map, Set}

import jakarta.persistence.Query
import com.google.common.collect.Multimap
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.{ComponentInterface, DataModel}
import com.learningobjects.cpxp.dto.Facade
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.item.Item

import scala.reflect.ClassTag

/** Implementations provide a means of dynamically building EJB-QL based on assumptions about our data model.
  * Implementations should also participate in application wide caching.
  */
trait QueryBuilder:

  /** Set the root of the query.
    *
    * @param root
    *   the query root
    */
  def setRoot(root: Item): QueryBuilder

  /** Set the implicit root of the query. The root is implicit, by the side-effect of a condition or subselect, and will
    * not be explicitly tested.
    */
  def setImplicitRoot(root: Item): QueryBuilder

  /** Set the parent and note that it should be tested explicitly. The root will implicitly be set from the parent.
    *
    * @param parent
    *   the query parent
    */
  def setParent(parent: Item): QueryBuilder

  /** Set the implicit parent of the query. The parent is implicit, by the side-effect of a condition or subselect, and
    * will not be explicitly tested.
    */
  def setImplicitParent(parent: Item): QueryBuilder

  /** Set the id of the one entity to project against.
    */
  def setId(id: jl.Long): QueryBuilder

  /** Set the single item to project against.
    *
    * @param item
    */
  def setItem(item: Item): QueryBuilder

  /** Add an initial query that will be used to filter initial items to be used in the main query. The root will
    * implicitly be set from the query. If the query is inclusive, this will include items in any initial query. If
    * exclusive then it will include items in no initial query.
    *
    * @param initialQuery
    *   the initial query builder
    */
  def addInitialQuery(initialQuery: QueryBuilder): QueryBuilder

  def createInitialQuery(): QueryBuilder

  /** Add an initial query that will be used to filter initial items to be used in the main query. The root will
    * implicitly be set from the query. If the query is inclusive, this will include items in any initial query. If
    * exclusive then it will include items in no initial query. Instead of using a subselect (WHERE id IN) this will use
    * an exists test (WHERE EXISTS)...
    *
    * @param existsQuery
    *   the exists query builder
    */
  def addExistsQuery(existsQuery: QueryBuilder): QueryBuilder

  /** Add a join query that will be used to filter items from another table joined against this.
    *
    * @param dataType
    *   the data type to join on
    * @param joinQuery
    *   the join query builder
    */
  def addJoinQuery(dataType: String, joinQuery: QueryBuilder): QueryBuilder

  /** Add an arbitrary join to this query. See `Join` and subclasses.
    *
    * @param join
    *   the join to add
    */
  def addJoin(join: Join): QueryBuilder

  /** Add a join query that will be used to filter items from another table joined against this. If an existing join
    * query exists it will be returned.
    *
    * @param dataType
    *   the data type to join on
    * @param itemType
    *   the item type to join against
    * @return
    *   the new query builder, not this query builder
    */
  def getOrCreateJoinQuery(dataType: String, itemType: String): QueryBuilder

  def getOrAddJoinQuery(dataType: String, itemType: String, join: => Join): QueryBuilder

  /** Sets the type of the items being queried.
    *
    * @param type
    *   the item type
    */
  def setItemType(`type`: String): QueryBuilder

  /** Sets the implicit type of the items being queried. The item type is implicit, by the side-effect of a condition or
    * subselect, and will not be explicitly tested.
    *
    * @param type
    *   the item type
    */
  def setImplicitItemType(`type`: String): QueryBuilder

  /** Sets the whether to look for deleted items.
    *
    * @param deleted
    *   whether to look for deleted items
    */
  def setIncludeDeleted(deleted: Boolean): QueryBuilder

  /** Sets the materialized path of the items being queried. <strong>Note: Use with caution. Consider using a condition
    * or a join instead.</strong>
    *
    * @param path
    *   the materialized path
    */
  def setPath(path: String): QueryBuilder

  /** Exclude the entire path (default false). */
  def setExcludePath(exclude: Boolean): QueryBuilder

  def setInclusive(inclusive: Boolean): QueryBuilder

  /** Adds a single sequence of OR'ed conditions. A disjunction: A OR B.
    *
    * NB: An empty list is treated as always true.
    *
    * @param conditions
    *   the sequence of conditions to add
    */
  def addDisjunction0(conditions: List[Condition]): QueryBuilder

  def addDisjunction[C <: Condition](conditions: scala.Iterable[C]): QueryBuilder

  /** Adds a sequence of a sequence of OR'ed conditions. A disjunction of conjunctions: (A AND B) OR (C AND D).
    *
    * NB: An empty list is treated as always true.
    *
    * @param conditions
    *   the sequence of sequences of conditions to add
    */
  def addDisjunction(conditions: List[List[Condition]]): QueryBuilder

  /** Add a simple constraint on the resulting items based an a { DataType}, a comparison operator and a provided value.
    *
    * @param type
    *   used to constrain the type_name column of the data table and to select the correct value column
    * @param cmp
    *   comparison operator to apply
    * @param value
    *   comparison value, may be null; if so the builder will produce a null-correct query
    */
  def addCondition(`type`: String, cmp: String, value: Any): QueryBuilder

  def addCondition(`type`: String, cmp: Comparison, value: Any): QueryBuilder

  def addCondition(`type`: String, cmp: Comparison, value: Any, f: Function): QueryBuilder

  /** Add a simple constraint on the resulting items based an a { DataType}, a comparison operator and a provided value.
    *
    * @param condition
    *   an already assembled condition object
    */
  def addCondition(condition: Condition): QueryBuilder

  def addConjunction[C <: Condition](conditions: scala.Iterable[C]): QueryBuilder

  def addConjunction[C <: Condition](conditions: java.lang.Iterable[C]): QueryBuilder

  /** Group the results by the provided { DataType}. Each call will add another group to the GROUP BY clause
    *
    * @param type
    *   specifies the type_name column and an apropros value column on the data table
    */
  def setGroup(`type`: String): QueryBuilder

  def setGroup(types: String*): QueryBuilder =
    types.foreach(setGroup)
    this

  /** Remove any ordering.
    */
  def clearOrder: QueryBuilder

  /** Ensure ordering of the results, may be called repeatedly to order over multiple { DataType} instances.
    *
    * @param type
    *   specifies the type_name column and an apropros value column on the data table
    * @param order
    *   the ordinality of the sort
    */
  def setOrder(`type`: String, order: Direction): QueryBuilder

  /** Ensure ordering of the results, may be called repeatedly to order over multiple { DataType} instances.
    *
    * @param type
    *   specifies the type_name column and an apropros value column on the data table
    * @param function
    *   function to call before ordering
    * @param order
    *   the ordinality of the sort
    */
  def setOrder(`type`: String, function: Function, order: Direction): QueryBuilder

  /** Ensure ordering of the results by a function of the item, probably {@link Function#COUNT} when combined with
    * {@link #setGroup} .
    *
    * @param function
    *   function to call before ordering
    * @param order
    *   the ordinality of the sort
    */
  def setOrder(function: Function, order: Direction): QueryBuilder

  /** Ensure ordering of the results by a child query.
    *
    * @param query
    *   the child query to order by
    * @param order
    *   the ordinality of the sort
    */
  def setOrder(query: QueryBuilder, order: Direction): QueryBuilder

  def addOrder(order: Order): QueryBuilder

  def addOrders(orders: java.lang.Iterable[Order]): QueryBuilder

  /** Add an order against a property of a joined query builder
    * @param qb
    *   the joined query builder or this query builder
    * @param order
    *   the order to apply to the joined query builder
    * @return
    *   this query builder
    */
  def addJoinOrder(qb: QueryBuilder, order: Order): QueryBuilder

  /** Sets a maximum number of results.
    *
    * @param limit
    *   only return this many or fewer results
    */
  def setLimit(limit: Int): QueryBuilder

  def setLimit(limit: Integer): QueryBuilder

  /** Sets the first result of interest.
    *
    * @param firstResult
    *   results should start at this offset
    */
  def setFirstResult(firstResult: Int): QueryBuilder

  def setFirstResult(firstResult: Integer): QueryBuilder

  /** Set no results.
    */
  def setNoResults(): QueryBuilder

  /** Set the projection.
    *
    * @param projection
    *   the data to project
    * @param parameters
    *   the projection parameters
    */
  def setProjection(projection: Projection, parameters: Array[AnyRef]): QueryBuilder
  def setProjection(projection: Projection, parameter: String): QueryBuilder
  def setProjection(projection: Projection): QueryBuilder

  /** Sets the data to project.
    *
    * @param projection
    *   the data to project
    */
  def setDataProjection(projection: String): QueryBuilder

  /** Sets the data to project.
    *
    * @param projection
    *   the data to project
    */
  def setDataProjection(projection: DataProjection): QueryBuilder
  def setDataProjection(projections: Array[DataProjection]): QueryBuilder

  /** Specifies whether to return only distinct results.
    *
    * In the general case, mixing distinct and order is not valid. With data projection you should group the items by
    * the field and order by the max/min of the order instead. In the case of an item projection where there is a
    * guaranteed single instance of each order value for each item, then you may combine distinct and order.
    *
    * With specificity, SELECT DISTINCT x, y ORDER BY y is incorrect because that will return duplicate x since it is
    * looking for distinct (x, y). That should be expressed as: SELECT field FROM x GROUP BY field ORDER BY MAX(order)
    * DESC. However, if it is the case that x always has a single y then it is a safe and correct query.
    *
    * @param distinct
    *   whether to return only distinct results
    */
  def setDistinct(distinct: Boolean): QueryBuilder

  /** Sets the function to apply to the result. Note that the function is always applied to the distinct results if
    * distinct is also specified.
    *
    * @param function
    *   the function to apply
    */
  def setFunction(function: Function): QueryBuilder

  /** Momentary hack to add a having condition. */
  def having(qb: QueryBuilder, cmp: Comparison, value: Number): QueryBuilder

  /** Specify whether to use the query cache. Defaults to yes if possible. If you specify true here then system-wide
    * queries will be cached.
    *
    * @param cacheQuery
    *   whether to use the query cache
    */
  def setCacheQuery(cacheQuery: Boolean): QueryBuilder

  /** Specify whether to join fetch finders against the item table. This may not improve performance.
    *
    * @param joinFetch
    *   whether to join fetch
    */
  def setJoinFetch(joinFetch: Boolean): QueryBuilder

  /** Specify whether to cache nothing results. Defaults to yes if possible.
    *
    * @param cacheNothing
    *   whether to cache nothing results
    */
  def setCacheNothing(cacheNothing: Boolean): QueryBuilder

  /** Set the cache invalidation key. Will cause global queries to be cached.
    *
    * If ANY keys in the list are invalidated, the query will be invalidated.
    *
    * @param invalidationKey
    *   new invalidation key for the results
    */
  def addInvalidationKey(invalidationKey: String): QueryBuilder

  def getInvalidationKeys: Set[String]

  /** Returns the {@link Projection#ITEM} projection of the query, or, if the query's projection is not of type Item,
    * crashes obscurely and catastophically.
    *
    * @return
    *   the items that match the built query
    */
  def getItems(): List[Item]

  /** Get the sole result of the query.
    *
    * @return
    *   the result of the query, or null
    */
  def getResult[T]: T

  def optionResult[T]: Option[T] = Option(getResult[T])

  /** Get all the results of the query, assuming they are of the type indicated by the T type parameter you invoke this
    * method with. If you guess incorrectly about the querybuilder's data projection, using the results of this method
    * will crash.
    *
    * TODO: parameterize QueryBuilder by return type to enforce correlation of (runtime) data projection result type and
    * (compile time) promised return types
    *
    * @return
    *   the values that match the built query
    */
  def getResultList[T]: List[T]

  def getProjectedResults: List[ProjectionResult]

  def getFacadeList[T <: Facade](facadeClass: Class[T]): List[T]

  def getComponentList[T <: ComponentInterface](componentClass: Class[T]): List[T]

  def getComponentList[T <: ComponentInterface](componentClass: Class[T], itemType: String, singleton: Boolean): List[T]

  // scala support

  def getComponent[T <: ComponentInterface](implicit
    tt: ClassTag[T],
    dm: DataModel[T] = null
  ): scala.Option[T] = getComponents[T].headOption

  def getComponents[T <: ComponentInterface](implicit
    tt: ClassTag[T],
    dm: DataModel[T] = null
  ): scala.Seq[T]

  def getFacades[T <: Facade](implicit tt: ClassTag[T]): scala.Seq[T] = ???

  def getFinders[T <: Finder](implicit tt: ClassTag[T]): scala.Seq[T] = ???

  def getFinder[T <: Finder](implicit tt: ClassTag[T]): scala.Option[T] = getFinders[T](using tt).headOption

  def getValues[T]: scala.Seq[T]

  /** Applies an aggregate function to the {@link Projection#ITEM} projection of the query and returns the numeric
    * result.
    *
    * @param aggregate
    *   supported function to apply to the entities in the selection
    * @return
    *   the single, numeric return from applying the aggregate to the selection
    */
  def getAggregateResult(aggregate: Function): java.lang.Long

  /** Adds a projection to the query, based on the supplied { DataType}.
    *
    * <strong>Note:</strong> Returns Long values for a { DataType} argument with {@link DataFormat#item} .
    *
    * @param type
    *   used to narrow the project from all of the item table down to the virtual data of interest
    * @return
    *   results matching the conditions applied to the builder, narrowed to the values matching the supplied data type.
    *   If multiple types were specified then a list of arrays is returned.
    */
  @SuppressWarnings(Array("unchecked"))
  def getProjectedResults[T](`type`: String): List[T]

  /** When composing a QueryBuilder as the initial condition, this method allows chaining of parameter sets from the
    * composing QueryBuilder instance.
    *
    * @param query
    *   internal JPA query
    * @return
    *   the keys as found in the JPA query and the values that matched
    */
  def setParameters(query: Query): Map[String, AnyRef]

  /** Another method that needs to be implemented for the composition case so that a composed query writes itself
    * correct to the buffer provided by the composing query.
    *
    * @return
    *   a statement, in EJB-QL, that can be embedded in the composing query's statement
    */
  def buildSubquery: String

  /** Another method that needs to be implemented for the composition case so that caching can function.
    *
    * @return
    *   a cache key description of the query
    */
  def getCacheKey: String

  /** Caches the given list of results for the cache key of this query.
    *
    * @param results
    *   the results to cache
    */
  def cacheValues(results: java.util.List[?]): Unit

  /** This method is called on a QueryBuilder representing a query on multiple parents. It performs the query and puts
    * the results in the QueryBuilder cache, separated out by each individual parent and each with the appropriate cache
    * key. This can save performance abundantly in a case where a single bulk query is far faster than many individual
    * queries, as is usually the case with Hibernate.
    *
    * WARNING: This will not work with a data projection other than Item.
    */
  def preloadPartitionedByParent[T <: Id](parents: java.lang.Iterable[T]): Multimap[java.lang.Long, Item]

  /** This method is called on a QueryBuilder representing a query for multiple item, which are in turn, parents to
    * items with data projections. The type case of this is that the items are parents to applied tags, which in turn,
    * project out to taxa. This executes the query with the given data project (overwriting any current projections on
    * the query builder), and stored the result of each individual item to projection on the child in the query cache.
    *
    * @param parents
    *   the list of items that are parents to the query results
    * @param projection
    *   the projection to apply to the query
    * @return
    *   a <code>Map</code> of the parent to the <code>List</code> of all projected values from the child
    */
  def preloadPartitionedByParentToProjection[T <: Id](
    parents: java.lang.Iterable[T],
    projection: DataProjection
  ): Map[Id, java.util.Collection[java.lang.Long]]

  /** This method is called on a QueryBuilder representing a query with an IN (...) condition. It performs the query and
    * puts the results in the QueryBuilder cache, separated out by each individual element in the IN list and each with
    * the appropriate cache key. This can save performance abundantly in a case where a single bulk query is far faster
    * than many individual queries, as is usually the case with Hibernate.
    *
    * WARNING: This will not work with a data projection other than Item.
    */
  def preloadPartitionedByInCondition[T <: Id](
    dataType: String,
    values: java.lang.Iterable[T]
  ): Multimap[java.lang.Long, Item]

  def setLogQuery(log: Boolean): QueryBuilder

  def getParent: java.util.Optional[Item]

  def getConditions: java.util.List[Condition]

  def forceNative(): QueryBuilder

  /** Evict this query from the query cache. */
  def evictQuery(): Unit
end QueryBuilder
