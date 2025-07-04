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

import java.lang as jl
import argonaut.*

import scala.jdk.CollectionConverters.*

object `package`:

  implicit def aqrCodec[E: EncodeJson: DecodeJson]: CodecJson[ApiQueryResults[E]] =
    final case class Proxy(
      objects: List[E],
      filterCount: Option[jl.Long],
      totalCount: Option[jl.Long],
    )

    val encoder = EncodeJson.derive[Proxy].contramap { (aqr: ApiQueryResults[E]) =>
      Proxy(
        objects = aqr.iterator().asScala.to(List),
        filterCount = Option(aqr.getFilterCount),
        totalCount = Option(aqr.getTotalCount),
      )
    }
    val decoder = DecodeJson.derive[Proxy].map { case Proxy(objects, fc, tc) =>
      new ApiQueryResults[E](objects.toBuffer.asJava, fc.orNull, tc.orNull)
    }

    CodecJson.derived(using encoder, decoder)
  end aqrCodec

  implicit val orderDirectionCodec: CodecJson[OrderDirection]       =
    CodecJson.derived[String].xmap(OrderDirection.byName)(_.toString)
  implicit val predicateOperatorCodec: CodecJson[PredicateOperator] =
    CodecJson.derived[String].xmap(PredicateOperator.byName)(_.toString)
end `package`
