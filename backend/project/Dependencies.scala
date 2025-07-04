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

import sbt.*
import sbt.librarymanagement.CrossVersion

/** Libraries common in all LO SBT projects.
  */
object Dependencies {

  /** Pekko is a toolkit and runtime for building highly concurrent, distributed, and resilient message-driven
    * applications on the JVM. https://github.com/apache/pekko
    */
  object Pekko {
    val pekkoVersion = "1.1.4"

    val actor        = "org.apache.pekko" %% "pekko-actor"              % pekkoVersion
    val cluster      = "org.apache.pekko" %% "pekko-cluster"            % pekkoVersion
    val clusterTools = "org.apache.pekko" %% "pekko-cluster-tools"      % pekkoVersion
    val discovery    = "org.apache.pekko" %% "pekko-discovery"          % pekkoVersion
    val mnTestkit    = "org.apache.pekko" %% "pekko-multi-node-testkit" % pekkoVersion
    val remote       = "org.apache.pekko" %% "pekko-remote"             % pekkoVersion
    val slf4j        = "org.apache.pekko" %% "pekko-slf4j"              % pekkoVersion
    val testkit      = "org.apache.pekko" %% "pekko-testkit"            % pekkoVersion

    /** Provides several custom node downing strategies for Pekko https://github.com/sisioh/pekko-cluster-custom-downing
      */
//    val clusterCustomDowning = "org.sisioh" %% "pekko-cluster-custom-downing" % "0.1.0"

    /** ec2 tag-based discovery https://pekko.apache.org/docs/pekko-management/current/discovery/aws.html
      */
    val discoveryAws     = "org.apache.pekko" %% "pekko-discovery-aws-api"            % "1.1.1"
    val clusterBootstrap = "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % "1.1.1"
  }

  /** Apache Commons is an Apache project focused on all aspects of reusable Java components.
    * https://commons.apache.org/
    */
  object ApacheCommons {

    /** Commons BeanUtils Easy-to-use wrappers around the Java reflection and introspection APIs.
      * https://commons.apache.org/proper/commons-beanutils/
      */
    val beanUtils = "commons-beanutils" % "commons-beanutils" % "1.11.0"

    /** Apache Commons Codec General encoding/decoding algorithms (for example phonetic, base64, URL).
      * https://commons.apache.org/proper/commons-codec/
      */
    val codec = "commons-codec" % "commons-codec" % "1.18.0"

    /** Commons Collections Extends or augments the Java Collections Framework.
      * https://commons.apache.org/proper/commons-collections/
      */
    val collections4 = "org.apache.commons" % "commons-collections4" % "4.5.0"

    /** Apache Commons Compress API for dealing with compression. https://commons.apache.org/proper/commons-compress/
      */
    val compress = "org.apache.commons" % "commons-compress" % "1.27.1"

    /** Apache Commons Exec API for dealing with external process execution and environment management in Java.
      * https://commons.apache.org/proper/commons-exec/
      */
    val exec = "org.apache.commons" % "commons-exec" % "1.5.0"

    /** Commons IO Collection of I/O utilities. https://commons.apache.org/proper/commons-io/
      */
    val io = "commons-io" % "commons-io" % "2.19.0"

    /** Commons Lang Provides extra functionality for classes in java.lang.
      * https://commons.apache.org/proper/commons-lang/
      */
    val lang3 = "org.apache.commons" % "commons-lang3" % "3.17.0"

    /** Commons Math: The Apache Commons Mathematics Library Commons Math is a library of lightweight, self-contained
      * mathematics and statistics components addressing the most common problems not available in the Java programming
      * language or Commons Lang.
      */
    val math3 = "org.apache.commons" % "commons-math3" % "3.6.1"

    /** Commons Net: Apache Commons Net library implements the client side of many basic Internet protocols.
      */
    val net = "commons-net" % "commons-net" % "3.11.1"

    /** Commons Text Provides extra functionality for classes in java.lang.
      * https://commons.apache.org/proper/commons-text/
      */
    val text = "org.apache.commons" % "commons-text" % "1.13.1"

    /** Commons validator: Apache Commons Net validates URLs.
      */
    val urlValidator = "commons-validator" % "commons-validator" % "1.9.0"
  }

  /** Apache HttpComponents The Apache HttpComponents™ project is responsible for creating and maintaining a toolset of
    * low level Java components focused on HTTP and associated protocols. https://hc.apache.org/
    */
  object ApacheHttpComponents {
    val httpVersion = "4.5.14"

    val httpClient = "org.apache.httpcomponents" % "httpclient" % httpVersion
    val httpMime   = "org.apache.httpcomponents" % "httpmime"   % httpVersion

    val httpCore = "org.apache.httpcomponents" % "httpcore" % "4.4.16" // separately versioned
  }

  /** ASM, A Java byecode engineering library. http://asm.ow2.org/
    */
  object ASM {
    val asmVersion = "9.8"

    val asm  = "org.ow2.asm" % "asm"      % asmVersion
    val util = "org.ow2.asm" % "asm-util" % asmVersion
  }

  /** Various fuzzy and foggy frameworks and libraries.
    */
  object Cloud {

    /** The Java Multi-Cloud Toolkit https://jclouds.apache.org/
      */
    val jcloudsVersion = "2.7.0"

    object Jclouds {
      // see difference between "API" and "Provider" in jclouds lingo at
      // https://jclouds.apache.org/start/concepts/
      val fileSystemApi = "org.apache.jclouds.api"      % "filesystem" % jcloudsVersion
      val awsS3Provider = "org.apache.jclouds.provider" % "aws-s3"     % jcloudsVersion
    }

    /** Amazon Web Service Libraries.
      */
    object AWS {
      // val awsVersion = "2.31.21" // https://github.com/scala-steward-org/scala-steward/issues/3297

      val auth      = "software.amazon.awssdk" % "auth"      % "2.31.63"
      val core      = "software.amazon.awssdk" % "core"      % "2.31.63"
      val ec2       = "software.amazon.awssdk" % "ec2"       % "2.31.63"
      val s3        = "software.amazon.awssdk" % "s3"        % "2.31.63"
      val sqs       = "software.amazon.awssdk" % "sqs"       % "2.31.63"
      val sts       = "software.amazon.awssdk" % "sts"       % "2.31.63"
      val translate = "software.amazon.awssdk" % "translate" % "2.31.63"
    }

  }

  object Cats {
    val effect = "org.typelevel" %% "cats-effect" % "3.6.1"
    val mouse  = "org.typelevel" %% "mouse"       % "1.3.2"
  }

  object Config {

    /** Configuration library for JVM languages https://typesafehub.github.io/config/
      */
    val typesafe = "com.typesafe" % "config" % "1.4.3"
    val ficus    = "com.iheart"  %% "ficus"  % "1.5.2"
  }

  /** Various libraries and frameworks for working with databases.
    */
  object Databases {
    object Doobie {
      val doobieVersion = "1.0.0-RC9"

      val core      = "org.tpolecat" %% "doobie-core"      % doobieVersion
      val postgres  = "org.tpolecat" %% "doobie-postgres"  % doobieVersion
      val hikari    = "org.tpolecat" %% "doobie-hikari"    % doobieVersion
      val scalatest = "org.tpolecat" %% "doobie-scalatest" % doobieVersion
    }

    /** HikariCP is a Connection Pool. https://github.com/brettwooldridge/HikariCP
      */
    val hikaricp = "com.zaxxer" % "HikariCP" % "6.3.0"

    val postgresql = "org.postgresql" % "postgresql" % "42.7.7"
    val hsqldb     = "org.hsqldb"     % "hsqldb"     % "2.7.4"

    /** Blocking, simple Redis driver for Scala.
      *
      * https://github.com/debasishg/scala-redis
      */
    val redis = ("net.debasishg" %% "redisclient" % "3.42")
      .cross(CrossVersion.for3Use2_13)
    // try jedis

    val redshift = "com.amazon.redshift" % "redshift-jdbc42" % "2.1.0.33"
  }

  /** Hamcrest Matchers that can be combined to create flexible expressions of intent http://hamcrest.org/
    */
  object Hamcrest {
    val hamcrestVersion = "3.0"

    val core    = "org.hamcrest" % "hamcrest-core"    % hamcrestVersion
    val library = "org.hamcrest" % "hamcrest-library" % hamcrestVersion
  }

  /** Hibernate. Everything data. http://hibernate.org/
    */
  object Hibernate {
    val hibernateVersion = "7.0.4.Final"

    val core = "org.hibernate" % "hibernate-core" % hibernateVersion

    // https://github.com/vladmihalcea/hypersistence-utils
    val types = "io.hypersistence" % "hypersistence-utils-hibernate-70" % "3.10.1"
  }

  /** A typeful, purely functional, streaming library for HTTP clients and servers in Scala.
    *
    * http://http4s.org/
    */
  object Http4s {
    val http4sVersion        = "0.23.30"
    val http4sServletVersion = "0.25.0-RC1"
    val http4sBlazeVersion   = "0.23.17"

    val circe       = "org.http4s" %% "http4s-circe"        % http4sVersion
    val client      = "org.http4s" %% "http4s-client"       % http4sVersion
    val dsl         = "org.http4s" %% "http4s-dsl"          % http4sVersion
    val server      = "org.http4s" %% "http4s-server"       % http4sVersion
    val servlet     = "org.http4s" %% "http4s-servlet"      % http4sServletVersion
    // https://github.com/http4s/http4s/releases/tag/v0.23.12
    // post great-schism these are versioned separately
    val blazeClient = "org.http4s" %% "http4s-blaze-client" % http4sBlazeVersion
    val blazeServer = "org.http4s" %% "http4s-blaze-server" % http4sBlazeVersion
  }

  /** Various JavaEE APIs and implementations.
    */
  object JavaEE {

    /** */
    val ejbApi = "javax.ejb" % "ejb-api" % "3.0"

    /** Java Platform, Enterprise Edition (Java EE) 7 http://docs.oracle.com/javaee/7/index.html
      */
    val javaEE7 = "javax" % "javaee-api" % "8.0.1"

    /** JavaMail API https://java.net/projects/javamail/pages/Home
      */
    val mailApi = "javax.mail" % "javax.mail-api" % "1.6.2"

    /** JavaMail implementation
      */
    val mailImpl = "com.sun.mail" % "javax.mail" % "1.6.2"

    /** JAXB
      */
    val jaxbApi  = "javax.xml" % "jaxb-api"  % "2.1"
    val jaxbImpl = "javax.xml" % "jaxb-impl" % "2.1"

    /** Simple JNDI A simple implementation of JNDI. https://github.com/hen/osjava
      */
    val simpleJndi = "simple-jndi" % "simple-jndi" % "0.11.4.1"

    object Tomcat {
      val tomcatVersion = "11.0.8"

      val servletApi = "org.apache.tomcat" % "tomcat-servlet-api" % tomcatVersion

      object Embedded {
        val core      = "org.apache.tomcat.embed" % "tomcat-embed-core"      % tomcatVersion
        val websocket = "org.apache.tomcat.embed" % "tomcat-embed-websocket" % tomcatVersion
      }
    }
  }

  object Jakarta {

    /** Jakarta servlet API.
      */
    val servletApi = "jakarta.servlet" % "jakarta.servlet-api" % "6.1.0" % "provided"
  }

  /** Various JSON parsers.
    */
  object JSON {

    val jsonldJava = "com.github.jsonld-java" % "jsonld-java" % "0.13.6"

    /** Jackson JSON Processor http://wiki.fasterxml.com/JacksonHome
      */
    object Jackson {
      val jacksonVersion = "2.19.1"

      val annotations = "com.fasterxml.jackson.core"       % "jackson-annotations"     % jacksonVersion
      val core        = "com.fasterxml.jackson.core"       % "jackson-core"            % jacksonVersion
      val csv         = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv"  % jacksonVersion
      val databind    = "com.fasterxml.jackson.core"       % "jackson-databind"        % jacksonVersion
      val guava       = "com.fasterxml.jackson.datatype"   % "jackson-datatype-guava"  % jacksonVersion
      val joda        = "com.fasterxml.jackson.datatype"   % "jackson-datatype-joda"   % jacksonVersion
      val jdk8        = "com.fasterxml.jackson.datatype"   % "jackson-datatype-jdk8"   % jacksonVersion
      val jsr310      = "com.fasterxml.jackson.datatype"   % "jackson-datatype-jsr310" % jacksonVersion
      val mrBean      = "com.fasterxml.jackson.module"     % "jackson-module-mrbean"   % jacksonVersion
      val scala       = "com.fasterxml.jackson.module"    %% "jackson-module-scala"    % jacksonVersion
      val xml         = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml"  % jacksonVersion
    }

    /** Argonaut - Purely Functional JSON in Scala Argonaut is a JSON library for Scala, providing a rich library for
      * parsing, printing and manipulation as well as convenient codecs for translation to and from scala data types.
      *
      * https://github.com/argonaut-io/argonaut
      */
    object Argonaut {
      val argonautVersion = "6.3.11"

      val argonaut       = "io.github.argonaut-io" %% "argonaut"        % argonautVersion
      val argonautCats   = "io.github.argonaut-io" %% "argonaut-cats"   % argonautVersion
      val argonautScalaz = "io.argonaut"           %% "argonaut-scalaz" % argonautVersion
//      val argonautShapeless = "com.github.alexarchambault" %% "argonaut-shapeless_6.3" % "1.3.1"
    }

    /** Circe - Argonaut for cats https://github.com/circe/circe
      */
    object Circe {
      val circeVersion = "0.14.14"

      val core    = "io.circe" %% "circe-core"    % circeVersion
      val generic = "io.circe" %% "circe-generic" % circeVersion
      val jawn    = "io.circe" %% "circe-jawn"    % circeVersion
    }

    /** JWT Scala - Scala support for JSON Web Token (JWT - https://datatracker.ietf.org/doc/html/rfc7519).
      *
      * https://jwt-scala.github.io/jwt-scala/
      */
    object JWT {
      val jwtScalaVersion = "11.0.2"
      val scala           = "com.github.jwt-scala" %% "jwt-core" % jwtScalaVersion
    }

    val jsonDiff = "io.github.deblockt" % "json-diff" % "1.1.0"
  }

  /** Various Logging Frameworks
    */
  object Logging {
    // ALL OF THEM
    val log4jCore = "org.apache.logging.log4j" % "log4j-core" % "2.25.0"
    val log4s     = "org.log4s"               %% "log4s"      % "1.10.0"

    object Slf4j {
      val api   = "org.slf4j" % "slf4j-api"   % "2.0.17"
      val jdk14 = "org.slf4j" % "slf4j-jdk14" % "2.0.17"
    }

  }

  object FS2 {
    val fs2Version = "3.1.1"
    val fs2Core    = "co.fs2" %% "fs2-core" % fs2Version
  }

  /** Other, uncategorized libraries.
    */
  object Misc {

    /** Bytecode buddy https://bytebuddy.net/
      */
    val byteBuddy = "net.bytebuddy" % "byte-buddy" % "1.17.6"

    /*
     * Javassist
     * http://www.javassist.org/
     */
    val javassist = "org.javassist" % "javassist" % "3.30.2-GA"

    /** http://findbugs.sourceforge.net/
      */
    val findbugsJsr305 = "com.google.code.findbugs" % "jsr305" % "3.0.2"

    /** Guava: Google Core Libraries for Java https://github.com/google/guava
      */
    val guava = "com.google.guava" % "guava" % "33.4.8-jre"

    /** IMS Enterprise Specification http://www.imsglobal.org/enterprise/
      */
//    val imsEnterprise = "ims" % "enterpriseServices" % "1.0"

    /** Jericho HTML Parser http://jericho.htmlparser.net/
      */
    val jericho = "net.htmlparser.jericho" % "jericho-html" % "3.4"

    /** Detects file types https://tika.apache.org/
      */
    val apacheTika = "org.apache.tika" % "tika-core" % "3.2.0"

    /** Parse PDFs. https://pdfbox.apache.org/
      */
    val apachePdfBox = "org.apache.pdfbox" % "pdfbox" % "3.0.5"

    /** IMS Global - LTI™ Utilities https://github.com/IMSGlobal/basiclti-util-java
      */
    val ltiUtil = "org.imsglobal" % "basiclti-util" % "1.2.0"

    /** LTI without dependency on ancient cglib, asm, or jackson.
      */
    val ltiUtilNeitherCglibNorJackson = ltiUtil.excludeAll(
      ExclusionRule("cglib", "cglib"),
      ExclusionRule("org.codehaus.jackson"),
    )

    val ldap = "ldapsdk" % "ldapsdk" % "4.1"

    val paranamer = "com.thoughtworks.paranamer" % "paranamer" % "2.8.3"

    /** The QTItools project is developing a number of tools for the handling of QTI v2.1 assessments and items.
      * http://sourceforge.net/projects/qtitools/
      */
//    val qtiTools = "org.qtitools" % "jqti" % "2.0a4"

    /** Quartz Job Scheduling Library http://quartz-scheduler.org/
      */
    val quartz = "org.quartz-scheduler" % "quartz" % "2.5.0"

    /** CSV Reader/Writer for Scala https://github.com/tototoshi/scala-csv
      */
    val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "2.0.0"

    /** Embedded sass compiler. https://github.com/larsgrefer/dart-sass-java
      */
    val sassEmbeddedHost = "de.larsgrefer.sass" % "sass-embedded-host" % "4.2.0"

    /** https://github.com/classgraph/classgraph
      */
    val classGraph = "io.github.classgraph" % "classgraph" % "4.8.180"

    /** */
    val opencsv = "net.sf.opencsv" % "opencsv" % "2.3"

    /** Simple scala command line options parsing https://github.com/scopt/scopt
      */
    val scopt = "com.github.scopt" %% "scopt" % "4.1.0"

    /** Scala/Scala.js library for manipulating Fancy Ansi colored strings https://github.com/lihaoyi/fansi
      */
    val fansi = "com.lihaoyi" %% "fansi" % "0.5.0"

    val newType = "io.estatico" %% "newtype" % "0.4.2"

    /** kantan.csv is a library for CSV parsing and serialisation written in the Scala programming language.
      * https://nrinaudo.github.io/kantan.csv/
      */
    object Kantan {
      val kantanVersion = "0.8.0"

      val csv = ("com.nrinaudo" %% "kantan.csv" % kantanVersion)
        .cross(CrossVersion.for3Use2_13)
    }

    /** Java implementation of CommonMark, a specification of the Markdown format
      * https://github.com/atlassian/commonmark-java
      */
    val commonMark = "com.atlassian.commonmark" % "commonmark" % "0.17.0"

    /** Java port of universalchardet https://github.com/albfernandez/juniversalchardet
      */
    val jUniversalCharDet = "com.github.albfernandez" % "juniversalchardet" % "2.5.0"

    /** Apache POI - the Java API for Microsoft Documents https://poi.apache.org/
      */
    object ApachePOI {
      val poiVersion = "5.4.1"
      val ooxml      = "org.apache.poi" % "poi-ooxml" % poiVersion
    }

    /** Scala ElasticSearch client https://github.com/Philippus/elastic4s
      */
    object Elastic4s {
      val clientEsJava = "nl.gn0s1s" %% "elastic4s-client-esjava" % "9.0.0"
    }

    /** https://github.com/acm19/aws-request-signing-apache-interceptor */
    val awsSigningRequestInterceptor = "io.github.acm19" % "aws-request-signing-apache-interceptor" % "3.0.0"

    /** CSS parsing and manipulation. https://github.com/phax/ph-css
      */
    val phCss = "com.helger" % "ph-css" % "7.0.4"

    /** Nashorn Javascript interpreter. https://github.com/openjdk/nashorn
      */
    val nashorn = "org.openjdk.nashorn" % "nashorn-core" % "15.6"

    /** SPOIWO (Scala POI Wrapping Objects). https://github.com/norbert-radyk/spoiwo */
    val spoiwo = "com.norbitltd" %% "spoiwo" % "2.2.1"

    /** Airframe Surface reads type information.. https://github.com/wvlet/airframe/tree/main/airframe-surface
      *   - compile-time, not runtime
      */
    val airframeSurface = "org.wvlet.airframe" %% "airframe-surface" % "2025.1.6"

    /** Runtime reflection of class files via their tasty files. https://github.com/gzoller/scala-reflection
      *   - slow and crashes on our codebase.
      */
    val scalaReflection = "co.blocke" %% "scala-reflection" % "2.0.12"
  }

  object Prometheus {
    val prometheusVersion    = "0.16.0"
    val simpleclient         = "io.prometheus" % "simpleclient"         % prometheusVersion
    val simpleclient_hotspot = "io.prometheus" % "simpleclient_hotspot" % prometheusVersion
    val simpleclient_common  = "io.prometheus" % "simpleclient_common"  % prometheusVersion
  }

  /** Scala a object-functional programming language. http://scala-lang.org
    */
  object Scala {
    def compiler(version: String) = "org.scala-lang" %% "scala3-compiler" % version
  }

  /** Scala-specific plugins
    */
  object ScalaExtensions {
    val java8Compat       = "org.scala-lang.modules" %% "scala-java8-compat"       % "1.0.2"
    val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0"

    object Enumeratum {
      val enumeratumVersion         = "1.9.0"
      val enumeratumArgonautVersion = "1.9.0"

      val core     = "com.beachape" %% "enumeratum"          % enumeratumVersion
      val argonaut = "com.beachape" %% "enumeratum-argonaut" % enumeratumArgonautVersion
      val circe    = "com.beachape" %% "enumeratum-circe"    % enumeratumVersion
    }

    val xml = "org.scala-lang.modules" %% "scala-xml" % "2.4.0"

    /** Scalameta is a modern metaprogramming library for Scala that supports a wide range of language versions and
      * execution platforms. http://scalameta.org/
      */
    val meta = "org.scalameta" %% "scalameta" % "1.8.0"

    /** Kind Projector - Compiler plugin for making type lambdas (type projections) easier to write
      *
      * One piece of Scala syntactic noise that often trips people up is the use of type projections to implement
      * anonymous, partially-applied types. Many people have wished for a better way to do this. The goal of this plugin
      * is to add a syntax for type lambdas.
      *
      * https://github.com/non/kind-projector
      */
    val kindProjector = "org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.patch

    val acyclic: Seq[ModuleID] = Seq(
      compilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7"),
      "com.lihaoyi" %% "acyclic" % "0.1.7" % "provided",
    )

    val splain = compilerPlugin("io.tryp" % "splain" % "0.5.0" cross CrossVersion.patch)
  }

  /** Scalaz: An extension to the core Scala library for functional programming. https://github.com/scalaz/scalaz
    */
  object ScalaZ {
    val scalazVersion = "7.3.8"

    /** Scalaz: An extension to the core Scala library for functional programming. https://github.com/scalaz/scalaz
      */
    val core = "org.scalaz" %% "scalaz-core" % scalazVersion
  }

  /** Various Testing frameworks and libraries.
    */
  object Testing {
    val easymock = "org.easymock" % "easymock" % "5.6.0"

    val scalaTest               = "org.scalatest"     %% "scalatest"       % "3.2.19"
    val scalaTestPlusEasyMock   = "org.scalatestplus" %% "easymock-4-3"    % "3.2.15.0"
    val scalaTestPlusScalaCheck = "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0"

    /** ScalaCheck is a library written in Scala and used for automated property-based testing of Scala or Java
      * programs. ScalaCheck was originally inspired by the Haskell library QuickCheck, but has also ventured into its
      * own.
      *
      * https://www.scalacheck.org/
      */
    val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.18.1"

    /** JUnit is a simple framework to write repeatable tests. http://junit.org/
      */
    object JUnit {
      val junitVersion = "4.13.2"
      val junit        = "junit" % "junit" % junitVersion
    }

  }

  // These suppress the `dependencyUpdates` command offering crap versions of things

  lazy val crapCommonsIO = moduleFilter(organization = "commons-io", name = "commons-io", revision = "2003*")

  lazy val crapCommonsNet = moduleFilter(organization = "commons-net", name = "commons-net", revision = "2003*")

  lazy val crapCommonsCodec = moduleFilter(organization = "commons-codec", name = "commons-codec", revision = "2004*")
}
