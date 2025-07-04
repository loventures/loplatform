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

import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait
import scaloi.syntax.ClassTagOps.classTagClass

import java.lang.{Boolean as JBoolean, Double as JDouble, Integer as JInteger, Long as JLong}
import java.util.{Optional, List as JList}
import scala.annotation.tailrec
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.specialized as sp

class ApiQueryUtils

/** Provides the ability to perform ApiQuery filtering on collections within the JVM. Beware of the negative performance
  * implications of doing this in JVM vs database. In particular, the performance of this implementation will scale
  * linearly in the total collection size where a proper database query can be much more efficient.
  *
  * WARNING: One significant shortcoming of this class is that it blindly assumes that api query parameters map to
  * bean-style method names. It does not consider cases where an aberrant property name is defined in either
  * `@JsonProperty` or `@Queryable`. It is also limited in the operators and types that it supports to those necessary
  * for the task at hand. Extensions are very welcome.
  */
object ApiQueryUtils:
  def query[A](as: JList[A], q: ApiQuery): ApiQueryResults[A] =
    query(as.asScala.toSeq, q)

  def query[A](values: Seq[A], query: ApiQuery): ApiQueryResults[A] =
    // first, prefilter the values
    val prefiltered =
      filter(values, query, query.getPrefilters.asScala.toSeq, "prefilter")
    // then filter the values
    val filtered    =
      filter(prefiltered, query, query.getFilters.asScala.toSeq, "filter")
    // sort the values if an order was specified
    val sorted      =
      if query.getOrders.isEmpty then filtered
      else filtered.sortWith(apiComparator[A](query))
    // page the values if a page was specified
    val paged       =
      if query.getPage.isSet then
        sorted.slice(query.getPage.getOffset, query.getPage.getOffset + query.getPage.getLimit)
      else sorted
    // convert results to expected type
    new ApiQueryResults(paged.asJava, filtered.size.toLong, prefiltered.size.toLong)
  end query

  /** Apply property mapping from a bean-property case class to the API query. */
  def propertyMap[A: ClassTag](aq: ApiQuery): ApiQuery =
    // this should one day be factored out and ApiQueryUtils made better able to apply to case classes
    val aqb = new ApiQuery.Builder(aq)
    for
      m       <- classTagClass[A].getMethods
      property = m.getName.stripPrefix("get")
      q       <- Option(m.getAnnotation(classOf[Queryable]))
    do aqb.addPropertyMapping(property, q)
    aqb.build

  // filter a sequence of values with a set of filters
  private def filter[A](values: Seq[A], query: ApiQuery, filters: Seq[ApiFilter], title: String): Seq[A] =
    // for each filter
    filters.foldLeft(values) { (as: Seq[A], f: ApiFilter) =>
      Option(query.getPropertyMapping(f.getProperty)) match
        // if filter property mapping is not found, fail
        case None                                                 =>
          throw new ValidationException(s"$title#property", f.getProperty, "Unknown property")
        // if property is unfilterable, fail
        case Some(q) if q.traits().contains(Trait.NOT_FILTERABLE) =>
          throw new ValidationException(s"$title#property", f.getProperty, "Unfilterable property")
        // otherwise filter the values
        case Some(q)                                              =>
          as.filter(apiFilterMatch(f, q))
    }

  // given an api filter and its queryable config, return a predicate that filters values
  private def apiFilterMatch[A](f: ApiFilter, q: Queryable): (A) => Boolean = (a) =>
    val caseless = q.traits().contains(Trait.CASE_INSENSITIVE)
    // dereference the property; if null, return false, else whether the filter matches
    // todo: null, non-null, in predicate operators?
    dereference(a, f.getProperty).fold(false) { p =>
      filterMatch(p, f.getOperator, f.getValue, caseless)
    }

  // crappy filter matching
  private def filterMatch(p: Any, op: PredicateOperator, value: String, caseless: Boolean): Boolean = p match
    case s: String if caseless => stringMatch(op, s.toLowerCase, value.toLowerCase)
    case s: String             => stringMatch(op, s, value)
    case b: Boolean            => valueMatch(op, b, value.toBoolean)
    case b: JBoolean           => valueMatch(op, b.booleanValue, value.toBoolean)
    case i: Int                => valueMatch(op, i, value.toInt)
    case i: JInteger           => valueMatch(op, i.intValue, value.toInt)
    case l: Long               => valueMatch(op, l, value.toLong)
    case l: JLong              => valueMatch(op, l.longValue, value.toLong)
    case d: Double             => valueMatch(op, d, value.toDouble)
    case d: JDouble            => valueMatch(op, d.doubleValue, value.toDouble)

  private def stringMatch(op: PredicateOperator, lhs: String, rhs: String): Boolean = op match
    case PredicateOperator.STARTS_WITH => lhs.startsWith(rhs)
    case PredicateOperator.ENDS_WITH   => lhs.endsWith(rhs)
    case PredicateOperator.CONTAINS    => lhs.contains(rhs)
    case _                             => valueMatch(op, lhs, rhs)

  private def valueMatch[@sp A: Ordering](op: PredicateOperator, lhs: A, rhs: A): Boolean = op match
    case PredicateOperator.EQUALS                 => lhs == rhs
    case PredicateOperator.NOT_EQUALS             => lhs != rhs
    case PredicateOperator.LESS_THAN              => Ordering[A].lt(lhs, rhs)
    case PredicateOperator.LESS_THAN_OR_EQUALS    => Ordering[A].lteq(lhs, rhs)
    case PredicateOperator.GREATER_THAN           => Ordering[A].gt(lhs, rhs)
    case PredicateOperator.GREATER_THAN_OR_EQUALS => Ordering[A].gteq(lhs, rhs)
    case _                                        =>
      throw new UnsupportedOperationException(s"Unsupported comparison: $op (on $lhs, $rhs)")

  // given an api query, returns a comparator that orders by the specified filters
  private def apiComparator[A](query: ApiQuery): (A, A) => Boolean =
    // iteratively (tail recursively) applies each order in turn until an inequality is found
    @tailrec
    def comparator(ss: List[Sort])(l: A, r: A): Boolean = ss match
      case Nil       => false
      case s :: tail =>
        val cmp = apiCompare(s, l, r)
        if cmp < 0 then s.ascending
        else if cmp > 0 then !s.ascending
        else comparator(tail)(l, r)
    // transform the api filters to a list of Sort objects and return a comparator operating on this list
    comparator(query.getOrders.asScala.map(Sort(query, _)).toList)
  end apiComparator

  // compares two values by a given sort order
  private def apiCompare[A](s: Sort, l: A, r: A): Int =
    (dereference(l, s.property), dereference(r, s.property)) match
      case (None, None)       => 0
      case (None, Some(_))    => -1
      case (Some(_), None)    => 1
      case (Some(a), Some(b)) =>
        (a, b) match
          case (c: String, d: String) if s.caseless =>
            c.toLowerCase.compareTo(d.toLowerCase)
          case (c: String, d: String)               => c.compareTo(d)
          case (c: Boolean, d: Boolean)             => c.compareTo(d)
          case (c: JBoolean, d: JBoolean)           => c.compareTo(d)
          case (c: Int, d: Int)                     => c.compareTo(d)
          case (c: JInteger, d: JInteger)           => c.compareTo(d)
          case (c: Long, d: Long)                   => c.compareTo(d)
          case (c: JLong, d: JLong)                 => c.compareTo(d)
          case (c: Double, d: Double)               => c.compareTo(d)
          case (c: JDouble, d: JDouble)             => c.compareTo(d)
          case _                                    => 0

  // capture info about a sort order in a tidy way
  case class Sort(property: String, ascending: Boolean, caseless: Boolean)

  object Sort:
    // convert a given ApiOrder in the context of an ApiQuery into a Sort
    def apply(query: ApiQuery, o: ApiOrder): Sort =
      Option(query.getPropertyMapping(o.getProperty)) match
        // fail on unknown sort property
        case None                                               =>
          throw new ValidationException("order#property", o.getProperty, "Unknown property")
        // fail on unsortable property
        case Some(q) if q.traits().contains(Trait.NOT_SORTABLE) =>
          throw new ValidationException("order#property", o.getProperty, "Unsortable property")
        // transform to Sort
        case Some(q)                                            =>
          Sort(o.getProperty, o.getDirection == OrderDirection.ASC, q.traits().contains(Trait.CASE_INSENSITIVE))
  end Sort

  // poor man's dereference. TODO: Bulk me up and make me jackson aware.
  private def dereference[A](a: A, p: String): Option[Any] =
    ComponentUtils.dereference(a, p) match
      case null           => None
      case o: Option[?]   => o
      case o: Optional[?] => o.asScala
      case v              => Some(v)
end ApiQueryUtils
