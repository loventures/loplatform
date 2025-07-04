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

package de.xlate

import cats.effect.*
import cats.syntax.all.*
import kantan.csv.*
import kantan.csv.ops.*
import scaloi.syntax.map.*
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.translate.TranslateClient
import software.amazon.awssdk.services.translate.model.TranslateTextRequest

import java.io.File

/** Vestigial app to generate Arabic and Chinese translations of the admin and course apps using the AWS translate
  * service.
  */
object App extends IOApp:
  private final val log = org.log4s.getLogger

  private final val CoreEn     = "util/src/main/resources/languages/en.properties"
  private final val AdminEn    = "../frontend/platform/src/i18n/Platform_en.csv"
  private final val CourseEn   = "../frontend/courseware/i18n/Courseware_en.csv"
  private final val BacktickRe = """``([^`]*)``""".r
  private final val XRe        = """X\dX""".r

  override def run(args: List[String]): IO[ExitCode] = gdpr[IO](args).map(_ => ExitCode.Success)

  private def gdpr[F[_]](args: List[String])(implicit F: Async[F]): F[Unit] =
    for
      creds     <- credentials[F]
      translate <- translator[F](creds)
      _         <- process[F](translate, AdminEn, "admin-ar.csv", "ar")
      _         <- process[F](translate, CourseEn, "course-ar.csv", "ar")
      _         <- process[F](translate, AdminEn, "admin-zh.csv", "zh")
      _         <- process[F](translate, CourseEn, "course-zh.csv", "zh")
      _         <- F.delay(log info "OK")
    yield ()

  private def credentials[F[_]: Sync]: F[DefaultCredentialsProvider] =
    Sync[F] delay {
      DefaultCredentialsProvider.builder.build()
    }

  private def translator[F[_]: Sync](creds: DefaultCredentialsProvider): F[TranslateClient] =
    Sync[F] delay {
      TranslateClient.builder.region(Region.US_EAST_1).credentialsProvider(creds).build()
    }

  private final val TokenRe = """%\{[^}]*\}|\{\{[^}]*\}\}|\{[^{}]*\}""".r // %{foo} {{foo}} {foo}

  // I am so sad and non functional but I'm just a PoC

  def process[F[_]: Sync](translate: TranslateClient, inf: String, outf: String, lang: String): F[Unit] =
    Sync[F] delay {
      val rawData       = new File(inf)
      val rawMap        = rawData
        .asCsvReader[List[String]](rfc)
        .toList
        .collect { case Right(key :: value :: _) =>
          key -> value
        }
        .toMap
      val expandedMap   = rawMap.mapValuesEagerly { value =>
        BacktickRe.replaceAllIn(value, m => rawMap(m.group(1)))
      }
      val translatedMap = expandedMap map { case (key, value) =>
        if key.startsWith("format") || key.contains("momentFormat") || key.isEmpty || value.isEmpty then key -> value
        else
          // The translate API seems to happily leave X0X unchanged. Putting an
          // alphabetic character at both ends seems to let it handle quotes
          // correctly so "X0X" becomes «X0X».
          val escapes   = TokenRe
            .findAllMatchIn(value)
            .toList
            .zipWithIndex
            .map { case (m, index) =>
              m.group(0) -> s"X${index}X"
            }
            .toMap
          val unescapes = escapes.map { case (k, v) =>
            v -> k
          }
          val escaped   = TokenRe.replaceAllIn(value, m => escapes(m.group(0)))
          val request   = TranslateTextRequest.builder
            .text(escaped)
            .sourceLanguageCode("en")
            .targetLanguageCode(lang)
          val result    = translate.translateText(request.build)
          val foreign   = result.translatedText
          val unescaped = XRe.replaceAllIn(foreign, m => unescapes(m.group(0)))
          key -> unescaped
      }
      val out           = new File(outf)
      val w             = out.asCsvWriter[List[String]](rfc)
      translatedMap foreach { case (key, value) =>
        w.write(key :: value :: Nil)
      }
      w.close()
    }
end App
