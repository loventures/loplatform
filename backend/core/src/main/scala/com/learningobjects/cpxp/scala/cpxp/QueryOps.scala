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

package com.learningobjects.cpxp.scala.cpxp

import com.learningobjects.cpxp.component.{ComponentInterface, ComponentService, ComponentSupport, DataModel}
import com.learningobjects.cpxp.dto.Facade as Façade
import com.learningobjects.cpxp.entity.EntityUtils
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.finder.Finder as FinderInterface
import com.learningobjects.cpxp.service.folder.FolderConstants
import com.learningobjects.cpxp.service.item.{ItemWebService, Item as TheItem}
import com.learningobjects.cpxp.service.query.{Comparison, Function, Projection, QueryBuilder, QueryService}
import jakarta.persistence.Query
import scaloi.GetOrCreate
import scaloi.syntax.ClassTagOps.*

import java.{lang as jl, util as ju}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

object QueryOps:

  implicit class QueryBuilderOps(qb: QueryBuilder):
    @inline final def count(): Long = qb.getAggregateResult(Function.COUNT)

    @inline final def nonEmpty(): Boolean = qb.setLimit(1).count() > 0

    def getOrCreate[T <: ComponentInterface: ClassTag: DataModel](init: AnyRef)(implicit
      cs: ComponentService
    ): GetOrCreate[T] =
      cs.getOrCreate(qb, classTagClass[T], DataModel[T], classTagClass[T], init)

    /** Projects to the entity ids, queries and returns the results as a `Seq[Long]`
      *
      * @return
      *   the entity PKs
      */
    def ids(): Seq[Long] =
      qb.setProjection(Projection.ID)
        .getResultList[Number]
        .asScala
        .toSeq
        .map(_.longValue())

    /** Projects to the entity's parent ids, queries and returns the results as a `Seq[Long]`.
      *
      * @return
      *   the entity's parent ids
      */
    def parentIds(): Seq[Long] =
      qb.setProjection(Projection.PARENT_ID)
        .getResultList[Number]
        .asScala
        .toSeq
        .map(_.longValue())
  end QueryBuilderOps

  implicit class PKQueryOps[A](val a: A) extends AnyVal:
    def getFolderById(idStr: String)(implicit PK: PK[A], qs: QueryService): TheItem =
      qs.queryRoot(FolderConstants.ITEM_TYPE_FOLDER) // i am as an axe murderer
        .addCondition(DataTypes.DATA_TYPE_ID, Comparison.eq, idStr)
        .getResult[TheItem]

    def getFolderByType(tpe: String)(implicit PK: PK[A], qs: QueryService): TheItem =
      queryChildren(FolderConstants.ITEM_TYPE_FOLDER)
        .addCondition(DataTypes.DATA_TYPE_TYPE, Comparison.eq, tpe)
        .getResult[TheItem]

    def queryChildren(itemType: String)(implicit PK: PK[A], qs: QueryService): QueryBuilder =
      qs.queryParent(PK.pk(a), itemType)

    def queryChildren[T <: FinderInterface: ClassTag](implicit PK: PK[A], qs: QueryService): QueryBuilder =
      qs.queryParent(PK.pk(a), EntityUtils.getItemType(classTagClass[T]))

    def queryAll[T <: FinderInterface: ClassTag](implicit PK: PK[A], qs: QueryService): QueryBuilder =
      qs.queryRoot(PK.pk(a), EntityUtils.getItemType(classTagClass[T]))

    def invalidateQueries()(implicit PK: PK[A], qs: QueryService): Unit =
      qs.evict(PK.pk(a))
  end PKQueryOps

  implicit class StringQueryBuilderOps(val idStr: String) extends AnyVal:
    def queryChildren(itemType: String)(implicit iws: ItemWebService, qs: QueryService): QueryBuilder =
      iws.getById(idStr).queryChildren(itemType)

  implicit class JpaQueryOps(val q: Query) extends AnyVal:
    def bind(params: (Symbol, Any)*): Query =
      params foreach { case (k, v) =>
        q.setParameter(k.name, v)
      }
      q

    def ids(): Seq[jl.Long] =
      q.getResultList
        .asInstanceOf[ju.List[jl.Number]]
        .asScala
        .toSeq
        .map(_.longValue: jl.Long) // horrid!

    def facades[F <: Façade: ClassTag]()(implicit fs: FacadeService): Seq[F] =
      fs.getFacades(ids().asJava, classTagClass[F]).asScala.toSeq

    def components[C <: ComponentInterface: ClassTag](): Seq[C] =
      ComponentSupport.getById(ids().asJava, classTagClass[C]).asScala.toSeq
  end JpaQueryOps

  implicit class StringQueryOps(val sc: StringContext) extends AnyVal:
    def sql(args: Any*)(implicit qS: QueryService): Query =
      qS.createNativeQuery(heuristMargin(toSql(sc.parts))).bind(toParams(args)*)

    def jpaql(args: Any*)(implicit qs: QueryService): Query =
      qs.createQuery(heuristMargin(toSql(sc.parts))).bind(toParams(args)*)

    private def toSql(parts: Seq[String]): String =
      parts.tail.zipWithIndex.foldLeft(parts.head) { case (pre, (part, index)) =>
        s"$pre:v$index$part"
      }

    private def toParams(args: Seq[Any]): Seq[(Symbol, Any)] =
      args.zipWithIndex map { case (arg, index) =>
        Symbol(s"v$index") -> arg
      }

    private def heuristMargin(sql: String): String =
      if sql
          .split('\n')
          .drop(1)
          .dropRight(1)
          .forall(line => MarginalLine.findFirstIn(line).isDefined)
      then sql.stripMargin
      else sql
  end StringQueryOps

  /* a marginal line is one which starts with spaces, then a single pipe.
   * this matches what .stripMargin would recognize. */
  private final val MarginalLine = """^\s*\|([^\|]|$)""".r
end QueryOps
