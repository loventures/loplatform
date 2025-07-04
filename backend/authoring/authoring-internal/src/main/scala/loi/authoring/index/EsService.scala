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

import com.fasterxml.jackson.module.scala.JavaTypeable
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.sksamuel.elastic4s.*
import com.sksamuel.elastic4s.analysis.{Analysis, CustomAnalyzer}
import com.sksamuel.elastic4s.http.{JavaClient, NoOpRequestConfigCallback}
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.HighlightField
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.typesafe.config.Config
import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheInterceptor
import loi.typesafe.config.syntax.config.*
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpHeaders, HttpRequest, HttpRequestInterceptor}
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import scalaz.std.map.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.option.*
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner
import software.amazon.awssdk.regions.Region
import cats.implicits.catsStdInstancesForFuture
import scala.concurrent.ExecutionContext.Implicits.global

import java.nio.charset.StandardCharsets
import java.util.{Base64, UUID}
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.util.control.NonFatal

/** Elastic search service. */
@Service(unique = true) // only one per JVM since it has client as state
trait EsService extends EsSearch:
  def isConfigured: Boolean
  def indexExists: Boolean
  def createIndex(): Unit
  def deleteIndex(): Unit
  def indexDocument(document: AssetNodeDocument): Unit
  def updateDocument(branch: Long, offering: Long, name: UUID, fields: (String, Any)*): Unit
  def deleteDocument(branch: Long, offering: Long, name: UUID): Unit
  def updateByQuery(query: EsQuery, fields: (String, String)*): Unit
  def deleteByQuery(query: EsQuery): Unit
end EsService

@Service
class EsSearchImpl(esService: EsService) extends EsSearch:
  override def search(query: EsQuery): EsResults      = esService.search(query)
  override def searchAll(query: EsQuery): List[EsHit] = esService.searchAll(query)

@Service
class EsServiceImpl(
  config: Config,
  domain: => DomainDTO,
  serviceMeta: ServiceMeta,
) extends EsService:
  import EsServiceImpl.*
  import com.sksamuel.elastic4s.ElasticDsl.{
    createIndex as esCreateIndex,
    deleteByQuery as esDeleteByQuery,
    deleteIndex as esDeleteIndex,
    search as esSearch,
    updateByQuerySync as esUpdateByQuery,
    *
  }

  /** Is elastic search configured. */
  override def isConfigured: Boolean = client.isDefined

  /** Test whether the elastic search index for this domain exists. */
  override def indexExists: Boolean =
    implicit val commonRequestOptions: CommonRequestOptions = CommonRequestOptions.defaults // unsupported by getIndex
    execute0(getIndex(indexName)).isSuccess

  /** Create the elastic search index for this domain. */
  override def createIndex(): Unit =
    // Support both stemming and exact search using an english_exact analyzer on ".exact" text subfields.
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/mixing-exact-search-with-stemming.html
    val success = execute(
      esCreateIndex(indexName)
        .mapping(AssetNodeDocument.mappingDefinition)
        .analysis(
          Analysis(
            CustomAnalyzer(
              name = "english_exact",
              tokenizer = "standard",
              tokenFilters = "lowercase" :: Nil
            )
          )
        )
    )
    logger.info(s"Create index: $success")
  end createIndex

  /** Delete the elastic search index for this domain. */
  override def deleteIndex(): Unit =
    val success = execute(esDeleteIndex(indexName))
    logger.info(s"Delete index: $success")

  /** Index a document into the elastic search index. */
  override def indexDocument(document: AssetNodeDocument): Unit =
    ensureIndex()
    val success = execute(
      indexInto(indexName).doc(document).id(documentId(document.branch, document.offering, document.name))
    )
    logger.info(s"Index document: $success")

  /** Update a document into the elastic search index. */
  override def updateDocument(branch: Long, offering: Long, name: UUID, fields: (String, Any)*): Unit =
    ensureIndex()
    val success = execute(updateById(indexName, documentId(branch, offering, name)).doc(fields))
    logger.info(s"Index document: $success")

  /** Delete a specific document in the elastic search index. */
  override def deleteDocument(branch: Long, offering: Long, name: UUID): Unit =
    ensureIndex()
    val success = execute(deleteById(indexName, documentId(branch, offering, name)))
    logger.info(s"Delete documents: $success")

  /** Update all documents matching a query. */
  override def updateByQuery(query: EsQuery, fields: (String, String)*): Unit =
    ensureIndex()
    val success = execute(
      esUpdateByQuery(indexName, query.elastically).script(
        Script(
          fields.map(t => s"ctx._source['${t._1}'] = params['${t._1}'];").mkString("\n"),
          lang = "painless".some,
          paramsRaw = fields.toMap
        )
      )
    )
    logger.info(s"Update documents: $success")
  end updateByQuery

  /** Delete all documents matching a query. */
  override def deleteByQuery(query: EsQuery): Unit =
    ensureIndex()
    val success = execute(esDeleteByQuery(indexName, query.elastically))
    logger.info(s"Delete documents: $success")

  /** Perform a search against the elastic search index, page through all results and return all matches. */
  override def searchAll(query: EsQuery): List[EsHit] =
    def loop(hits: List[EsHit]): List[EsHit] =
      val results  = search(query.copy(from = hits.size.some))
      val combined = hits ::: results.hits
      if combined.size >= results.total then combined else loop(combined)
    loop(Nil)

  /** Perform a search against the elastic search index. */
  override def search(query: EsQuery): EsResults =
    // Convert the query model into an elastic search command
    val command  =
      esSearch(indexName)
        .query(query.elastically)
        .copy(from = query.from)
        .copy(size = query.size)
        .copy(sorts = query.sortBy.map(t => FieldSort(t._1).order(t._2.fold(SortOrder.ASC, SortOrder.DESC))))
        .sourceInclude("project", "branch", "name", "archived")
        .highlighting(HighlightField(field = "*", preTags = "{{{" :: Nil, postTags = "}}}" :: Nil))
        .copy(aggs = query.aggregate.map(field => TermsAggregation(field, field = Some(field))).toList)
    logger.debug(s"Search request: $query -> $command")
    // Execute the command
    val response = execute(command)
    // Map the results
    val results  = EsResults(
      total = response.totalHits,
      hits = response.hits.hits.toList.map(hit =>
        EsHit(
          project = hit.sourceField("project").asInstanceOf[Number].longValue,
          branch = hit.sourceField("branch").asInstanceOf[Number].longValue,
          name = UUID.fromString(hit.sourceField("name").asInstanceOf[String]),
          highlights = hit.highlight.view.filterKeys(_ != "typeId").toMap,
          archived = hit.sourceField("archived").asInstanceOf[Boolean].booleanValue,
        )
      ),
      aggregates = query.aggregate.foldZ(field =>
        response.aggregations.filters(field).aggResults.map(f => f.dataAsMap("key") -> f.docCount).toMap
      )
    )
    logger.debug(s"Search results: $response -> $results")
    results
  end search

  /** Execute an elastic search command and await a successful response value. */
  def execute[T, U](t: T)(implicit handler: Handler[T, U], javaTypeable: JavaTypeable[U]): U =
    try
      val ts                  = new Stopwatch
      def loop(count: Int): U =
        try execute0(t).result
        catch
          case NonFatal(e) =>
            // ElasticSearch sometimes just transiently fails. We don't want this to kill a
            // full branch reindex. So we embed retry here, in addition to the message bus
            // retry processes.
            if (count >= MaxAttempts) || (ts.elapsed >= AttemptTimeout) then throw e // the throw monad
            logger.warn(e)(s"ElasticSearch error retry: $count")
            Thread.sleep(count.seconds.toMillis)
            loop(count + 1)
      loop(1)
    catch case NonFatal(e) => throw EsFailure(t).initCause(e)

  /** Execute an elastic search command, awaiting a response. */
  def execute0[T, U](
    t: T
  )(implicit
    handler: Handler[T, U],
    javaTypeable: JavaTypeable[U],
    commonRequestOptions: CommonRequestOptions
  ): Response[U] =
    logger.trace(s"ES request: $t")
    val response = client.fold(throw new IllegalStateException("ElasticSearch not configured."))(_.execute(t).await)
    logger.trace(s"ES response: $response")
    response
  end execute0

  /** Ensure the domain index exists. */
  private def ensureIndex(): Unit =
    synchronized {
      if knownIndices.add(indexName) && !indexExists then createIndex()
    }

  /** The index name for the current domain. */
  private def indexName = s"${serviceMeta.getCluster.toLowerCase}_${domain.id}"

  /** The identifier of a document. */
  private def documentId(branch: Long, offering: Long, name: UUID): String =
    s"${if offering == 0L then branch else offering}:$name"

  /** The maybe elastic client. */
  private lazy val client =
    for
      url        <- config.getOptionString("opensearch.url")
      interceptor =
        if config.getOptionString("opensearch.username").isDefined then basicInterceptor else signerInterceptor
      httpClient  = JavaClient(ElasticProperties(url), NoOpRequestConfigCallback, interceptor)
    yield ElasticClient(httpClient)

  private def basicInterceptor: HttpClientConfigCallback =
    _.addInterceptorLast(
      new HttpRequestInterceptor:
        val username = config.getString("opensearch.username")
        val password = config.getString("opensearch.password")
        val encoded  = Base64.getEncoder.encodeToString(s"$username:$password".getBytes(StandardCharsets.UTF_8))

        override def process(request: HttpRequest, context: HttpContext): Unit =
          request.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encoded")
    )

  private def signerInterceptor: HttpClientConfigCallback =
    _.addInterceptorLast(awsSigner)

  private def awsSigner: AwsRequestSigningApacheInterceptor =
    val region = config.getString("opensearch.region")
    new AwsRequestSigningApacheInterceptor(
      "es",
      AwsV4HttpSigner.create(),
      credentialsProvider,
      Region.of(region)
    )

  private def credentialsProvider: StaticCredentialsProvider =
    val identity   = config.getString("opensearch.identity")
    val credential = config.getString("opensearch.credential")
    StaticCredentialsProvider.create(AwsBasicCredentials.create(identity, credential))
end EsServiceImpl

object EsServiceImpl:
  private final val logger = org.log4s.getLogger

  private final val knownIndices = mutable.Set.empty[String]

  private final val MaxAttempts    = 4
  private final val AttemptTimeout = 30.seconds
  private final val RequestTimeout = 20.seconds

  private implicit val commonRequestOptions: CommonRequestOptions = CommonRequestOptions(
    timeout = RequestTimeout,
    masterNodeTimeout = 0.seconds // unsupported by some commands..
  )
end EsServiceImpl

/** An error occurred talking to the elastic search servec. */
final case class EsFailure(request: Any) extends Exception
