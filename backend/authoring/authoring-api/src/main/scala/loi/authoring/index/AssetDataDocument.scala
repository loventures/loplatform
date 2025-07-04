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

package loi.authoring.index

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.de.web.MediaType
import com.sksamuel.elastic4s.analysis.LanguageAnalyzers
import com.sksamuel.elastic4s.fields.{KeywordField, TextField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import loi.asset.license.License
import scalaz.syntax.std.option.*

/** Indexing document capturing the principal searchable fields of the asset data types. */
final case class AssetDataDocument(
  title: Option[String] = None,
  subtitle: Option[String] = None,
  description: Option[String] = None,
  keywords: Option[String] = None,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  instructions: Option[String] = None,
  fileName: Option[String] = None,
  contentType: Option[MediaType] = None,
  content: Option[String] = None,
  embedded: Option[String] = None,
)

object AssetDataDocument:
  implicit val assetDocumentEncodeJson: EncodeJson[AssetDataDocument] = EncodeJson { a =>
    Json(
      "title"        := a.title,
      "subtitle"     := a.subtitle,
      "description"  := a.description,
      "keywords"     := a.keywords,
      "license"      := a.license,
      "author"       := a.author,
      "attribution"  := a.attribution,
      "instructions" := a.instructions,
      "fileName"     := a.fileName,
      "contentType"  := a.contentType,
      "content"      := a.content,
      "embedded"     := a.embedded
    )
  }

  implicit val mediaTypeEncodeJson: EncodeJson[MediaType] = EncodeJson { a => a.toString.asJson }

  val mappingDefinition: MappingDefinition = MappingDefinition(properties =
    List(
      EnglishField("title"),
      EnglishField("subtitle"),
      EnglishField("description"),
      EnglishField("keywords"),
      KeywordField("license"),
      EnglishField("author"),
      EnglishField("attribution"),
      EnglishField("instructions"),
      TextField("fileName").analyzer("simple"),
      KeywordField("contentType"),
      EnglishField("content"),
      EnglishField("embedded"),
    )
  )

  private def EnglishField(name: String): TextField =
    TextField(
      name,
      analyzer = LanguageAnalyzers.english.some,
      searchAnalyzer = LanguageAnalyzers.english.some,
      // searchQuoteAnalyzer = "english_exact".some, // when this document is embedded, the quote analyzer breaks unquoted search
      fields = List(TextField("exact").analyzer("english_exact"))
    )

  // Some of these fields don't make sense, in the sense that they are not displayed
  // to the user when embedded in a learning activity (keywords, filename, attribution,
  // title (except for pdf)) etc.
  implicit val assetDocumentStrings: Strings[AssetDataDocument] =
    Strings.plaintext(a =>
      List(
        a.title,
        a.subtitle,
        a.description,
        a.keywords,
        a.attribution,
        a.instructions,
        a.fileName,
        a.content,
        a.embedded
      ).flatten
    )
end AssetDataDocument
