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

package loi.cp.scorm
package scalaxb

import scala.xml.{Node, NodeSeq, NamespaceBinding, Elem}
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.namespace.QName
import javax.xml.bind.DatatypeConverter

import scala.language.implicitConversions

object `package`:
  import annotation.implicitNotFound

  @implicitNotFound(msg = "Cannot find XMLFormat type class for ${A}")
  def fromXML[A](seq: NodeSeq, stack: List[ElemName] = Nil)(implicit format: XMLFormat[A]): A =
    format.reads(seq, stack) match
      case Right(a) => a
      case Left(a)  => throw new ParserFailure("Error while parsing %s: %s".format(seq.toString, a))

  @implicitNotFound(msg = "Cannot find XMLFormat type class for ${A}")
  def fromXMLEither[A](seq: NodeSeq, stack: List[ElemName] = Nil)(implicit format: XMLFormat[A]): Either[String, A] =
    format.reads(seq, stack)

  @implicitNotFound(msg = "Cannot find CanWriteXML type class for ${A}")
  def toXML[A](
    obj: A,
    namespace: Option[String],
    elementLabel: Option[String],
    scope: NamespaceBinding,
    typeAttribute: Boolean = false
  )(implicit format: CanWriteXML[A]): NodeSeq =
    format.writes(obj, namespace, elementLabel, scope, typeAttribute)

  @implicitNotFound(msg = "Cannot find CanWriteXML type class for ${A}")
  def toXML[A](obj: A, namespace: Option[String], elementLabel: String, scope: NamespaceBinding)(implicit
    format: CanWriteXML[A]
  ): NodeSeq =
    toXML(obj, namespace, Some(elementLabel), scope, false)

  @implicitNotFound(msg = "Cannot find CanWriteXML type class for ${A}")
  def toXML[A](obj: A, elementLabel: String, scope: NamespaceBinding)(implicit format: CanWriteXML[A]): NodeSeq =
    toXML(obj, None, Some(elementLabel), scope, false)

  /** @return
    *   - maps from prefix to namespace URI.
    */
  def fromScope(scope: NamespaceBinding): List[(Option[String], String)] =
    def doFromScope(s: NamespaceBinding): List[(Option[String], String)] =
      lazy val parentMap: List[(Option[String], String)] = Option[NamespaceBinding](s.parent) map {
        doFromScope
      } getOrElse { Nil }
      scalaxb.Helper.nullOrEmpty(s.uri) map { uri =>
        (scalaxb.Helper.nullOrEmpty(s.prefix) -> uri) :: parentMap
      } getOrElse { parentMap }
    doFromScope(scope).reverse

  /** @param pairs
    *   - pairs of (prefix, namespace URI)
    */
  def toScope(pairs: (Option[String], String)*): NamespaceBinding =
    pairs.reverse.foldLeft[NamespaceBinding](scala.xml.TopScope) { (scope, pair) =>
      scala.xml.NamespaceBinding(pair._1.getOrElse { null }, pair._2, scope)
    }
end `package`

trait XMLFormat[A] extends CanWriteXML[A] with CanReadXML[A]

trait CanReadXML[A]:
  def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, A]

trait CanWriteXML[A]:
  def writes(
    obj: A,
    namespace: Option[String],
    elementLabel: Option[String],
    scope: NamespaceBinding,
    typeAttribute: Boolean
  ): NodeSeq

object XMLStandardTypes extends XMLStandardTypes {}

trait XMLStandardTypes:
  implicit lazy val __NodeXMLFormat: XMLFormat[Node] = new XMLFormat[Node]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Node] = seq match
      case node: Node => Right(node)
      case _          => Left("scala.xml.Node is required.")

    def writes(
      obj: Node,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: NamespaceBinding,
      typeAttribute: Boolean
    ): NodeSeq = Helper.mergeNodeScope(obj, scope)

  implicit lazy val __NodeSeqXMLFormat: XMLFormat[NodeSeq] = new XMLFormat[NodeSeq]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, NodeSeq] = Right(seq)

    def writes(
      obj: NodeSeq,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: NamespaceBinding,
      typeAttribute: Boolean
    ): NodeSeq = Helper.mergeNodeSeqScope(obj, scope)

  implicit lazy val __ElemXMLFormat: XMLFormat[Elem] = new XMLFormat[Elem]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Elem] = seq match
      case elem: Elem => Right(elem)
      case _          => Left("scala.xml.Elem is required.")

    def writes(
      obj: Elem,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: NamespaceBinding,
      typeAttribute: Boolean
    ): NodeSeq = Helper.mergeNodeScope(obj, scope)

  implicit lazy val __StringXMLFormat: XMLFormat[String] = new XMLFormat[String]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, String] = Right(seq.text)

    def writes(
      obj: String,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj, namespace, elementLabel, scope)

  implicit lazy val __IntXMLFormat: XMLFormat[Int] = new XMLFormat[Int]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Int] = try Right(seq.text.toInt)
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Int,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __ByteXMLFormat: XMLFormat[Byte] = new XMLFormat[Byte]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Byte] = try Right(seq.text.toByte)
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Byte,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __ShortXMLFormat: XMLFormat[Short] = new XMLFormat[Short]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Short] = try Right(seq.text.toShort)
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Short,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __LongXMLFormat: XMLFormat[Long] = new XMLFormat[Long]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Long] = try Right(seq.text.toLong)
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Long,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __BigDecimalXMLFormat: XMLFormat[BigDecimal] = new XMLFormat[BigDecimal]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, BigDecimal] = try
      Right(BigDecimal(seq.text))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: BigDecimal,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.bigDecimal.toPlainString, namespace, elementLabel, scope)

  implicit lazy val __BigIntXMLFormat: XMLFormat[BigInt] = new XMLFormat[BigInt]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, BigInt] = try Right(BigInt(seq.text))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: BigInt,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __FloatXMLFormat: XMLFormat[Float] = new XMLFormat[Float]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Float] = try Right(seq.text.toFloat)
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Float,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __DoubleXMLFormat: XMLFormat[Double] = new XMLFormat[Double]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Double] = try Right(seq.text.toDouble)
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Double,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __BooleanXMLFormat: XMLFormat[Boolean] = new XMLFormat[Boolean]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Boolean] =
      seq.text match
        case "1" | "true"  => Right(true)
        case "0" | "false" => Right(false)
        case x             => Left("Invalid boolean: " + x)

    def writes(
      obj: Boolean,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __DurationXMLFormat: XMLFormat[javax.xml.datatype.Duration] =
    new XMLFormat[javax.xml.datatype.Duration]:
      def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, javax.xml.datatype.Duration] =
        try Right(Helper.toDuration(seq.text))
        catch case e: Exception => Left(e.toString)

      def writes(
        obj: javax.xml.datatype.Duration,
        namespace: Option[String],
        elementLabel: Option[String],
        scope: scala.xml.NamespaceBinding,
        typeAttribute: Boolean
      ): scala.xml.NodeSeq =
        Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __CalendarXMLFormat: XMLFormat[XMLGregorianCalendar] = new XMLFormat[XMLGregorianCalendar]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, XMLGregorianCalendar] = try
      Right(XMLCalendar(seq.text))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: XMLGregorianCalendar,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toXMLFormat, namespace, elementLabel, scope)

  implicit lazy val __GregorianCalendarXMLWriter: CanWriteXML[java.util.GregorianCalendar] =
    new CanWriteXML[java.util.GregorianCalendar]:
      def writes(
        obj: java.util.GregorianCalendar,
        namespace: Option[String],
        elementLabel: Option[String],
        scope: scala.xml.NamespaceBinding,
        typeAttribute: Boolean
      ): scala.xml.NodeSeq =
        Helper.stringToXML(Helper.toCalendar(obj).toXMLFormat, namespace, elementLabel, scope)

  def qnameXMLFormat(scope: scala.xml.NamespaceBinding) = new XMLFormat[javax.xml.namespace.QName]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, javax.xml.namespace.QName] =
      seq match
        case node: scala.xml.Node =>
          val (namespace, localPart) = Helper.splitQName(node.text, scope)
          Right(new QName(namespace.orNull, localPart))
        case _                    => Left("scala.xml.Node is required")

    def writes(
      obj: javax.xml.namespace.QName,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __QNameXMLFormat: XMLFormat[javax.xml.namespace.QName] = new XMLFormat[javax.xml.namespace.QName]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, javax.xml.namespace.QName] =
      seq match
        case node: scala.xml.Node => qnameXMLFormat(node.scope).reads(node, stack)
        case _                    => Left("scala.xml.Node is required")

    def writes(
      obj: javax.xml.namespace.QName,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __Base64BinaryXMLFormat: XMLFormat[Base64Binary] = new XMLFormat[Base64Binary]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Base64Binary] = try
      Right(Base64Binary(seq.text))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: Base64Binary,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __HexBinaryXMLFormat: XMLFormat[HexBinary] = new XMLFormat[HexBinary]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, HexBinary] = try Right(HexBinary(seq.text))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: HexBinary,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit lazy val __URIXMLFormat: XMLFormat[java.net.URI] = new XMLFormat[java.net.URI]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, java.net.URI] = try
      Right(Helper.toURI(seq.text))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: java.net.URI,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(obj.toString, namespace, elementLabel, scope)

  implicit def seqXMLFormat[A: XMLFormat]: XMLFormat[Seq[A]] = new XMLFormat[Seq[A]]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, Seq[A]] =
      seq match
        case node: scala.xml.Node =>
          try
            val xs = Helper.splitBySpace(node.text).toSeq
            Right(xs map { x =>
              fromXML[A](
                scala.xml.Elem(node.prefix, node.label, scala.xml.Null, node.scope, true, scala.xml.Text(x)),
                stack
              )
            })
          catch case e: Exception => Left(e.toString)
        case _                    => Left("Node expected: " + seq.toString)

    def writes(
      obj: Seq[A],
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.stringToXML(
        (obj map { x => scalaxb.toXML(x, namespace, elementLabel, scope, typeAttribute).text }).mkString(" "),
        namespace,
        elementLabel,
        scope
      )

  implicit def dataRecordFormat[A: XMLFormat]: XMLFormat[DataRecord[A]] = new XMLFormat[DataRecord[A]]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, DataRecord[A]] = seq match
      case node: Node =>
        try Right(DataRecord(Some(node.namespace), Some(node.label), scalaxb.fromXML[A](node)))
        catch case e: Exception => Left(e.toString)
      case _          => Left("scala.xml.Node is required.")

    def writes(
      obj: DataRecord[A],
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      DataRecord.toXML(obj, namespace, elementLabel, scope, typeAttribute)

  implicit def dataRecordXMLWriter[A]: CanWriteXML[DataRecord[A]] = new CanWriteXML[DataRecord[A]]:
    def writes(
      obj: DataRecord[A],
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      DataRecord.toXML(obj, namespace, elementLabel, scope, typeAttribute)

  implicit def someXMLWriter[A: CanWriteXML]: CanWriteXML[Some[A]] = new CanWriteXML[Some[A]]:
    def writes(
      obj: Some[A],
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      scalaxb.toXML[A](obj.get, namespace, elementLabel, scope, typeAttribute)

  implicit def optionXMLWriter[A: CanWriteXML]: CanWriteXML[Option[A]] = new CanWriteXML[Option[A]]:
    def writes(
      obj: Option[A],
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq = obj match
      case Some(x) => scalaxb.toXML[A](x, namespace, elementLabel, scope, typeAttribute)
      case None    => Helper.nilElem(namespace, elementLabel.get, scope)

  implicit lazy val __NoneXMLWriter: CanWriteXML[None.type] = new CanWriteXML[None.type]:
    def writes(
      obj: None.type,
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      Helper.nilElem(namespace, elementLabel.get, scope)

  implicit def __DataRecordAnyXMLFormat(implicit
    handleNonDefault: scala.xml.Elem => Option[DataRecord[Any]] = _ => None
  ): XMLFormat[DataRecord[Any]] = new XMLFormat[DataRecord[Any]]:
    def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, DataRecord[Any]] = try
      Right(DataRecord.fromAny(seq, handleNonDefault))
    catch case e: Exception => Left(e.toString)

    def writes(
      obj: DataRecord[Any],
      namespace: Option[String],
      elementLabel: Option[String],
      scope: scala.xml.NamespaceBinding,
      typeAttribute: Boolean
    ): scala.xml.NodeSeq =
      DataRecord.toXML(obj, namespace, elementLabel, scope, typeAttribute)

  implicit lazy val __DataRecordOptionAnyXMLFormat: XMLFormat[DataRecord[Option[Any]]] =
    new XMLFormat[DataRecord[Option[Any]]]:
      def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, DataRecord[Option[Any]]] = try
        Right(DataRecord.fromNillableAny(seq))
      catch case e: Exception => Left(e.toString)

      def writes(
        obj: DataRecord[Option[Any]],
        namespace: Option[String],
        elementLabel: Option[String],
        scope: scala.xml.NamespaceBinding,
        typeAttribute: Boolean
      ): scala.xml.NodeSeq =
        DataRecord.toXML(obj, namespace, elementLabel, scope, typeAttribute)

  implicit lazy val __DataRecordMapWriter: CanWriteXML[Map[String, scalaxb.DataRecord[Any]]] =
    new CanWriteXML[Map[String, scalaxb.DataRecord[Any]]]:
      def writes(
        obj: Map[String, scalaxb.DataRecord[Any]],
        namespace: Option[String],
        elementLabel: Option[String],
        scope: scala.xml.NamespaceBinding,
        typeAttribute: Boolean
      ): scala.xml.NodeSeq =
        obj.valuesIterator.toList flatMap { x =>
          scalaxb.toXML[DataRecord[Any]](x, x.namespace, x.key, scope, typeAttribute)
        }
end XMLStandardTypes

trait DataRecord[+A]:
  val namespace: Option[String]
  val key: Option[String]
  val value: A

  def as[B] = value.asInstanceOf[B]

  override def toString: String =
    "DataRecord(" +
      ((namespace, key, value) match
        case (None, Some(k), _)    => k + "," + value.toString
        case (Some(n), Some(k), _) => "{" + n + "}" + k + "," + value.toString
        case _                     => value.toString) + ")"
end DataRecord

object DataRecord extends XMLStandardTypes:
  private case class DataWriter[+A](
    namespace: Option[String],
    key: Option[String],
    xstypeNamespace: Option[String],
    xstypeName: Option[String],
    value: A,
    writer: CanWriteXML[?]
  ) extends DataRecord[A]:
    override def equals(o: Any): Boolean =
      o match
        case that: DataWriter[?] =>
          namespace == that.namespace &&
          key == that.key &&
          value == that.value
        case _                   => false

    override def hashCode: Int =
      var result = 17
      result = result + 31 * namespace.hashCode
      result = result + 31 * key.hashCode
      result = result + 31 * value.hashCode
      result
  end DataWriter
  import Helper.*

  // this is for nil element.
  def apply(namespace: Option[String], key: Option[String], value: None.type): DataRecord[Option[Nothing]] =
    DataWriter(namespace, key, None, None, value, __NoneXMLWriter)

  // this is for choice option: DataRecord(x.namespace, Some(x.name), fromXML[Address](x))
  def apply[A: CanWriteXML](namespace: Option[String], key: Option[String], value: A): DataRecord[A] =
    DataWriter(namespace, key, None, None, value, implicitly[CanWriteXML[A]])

  def apply[A: CanWriteXML](node: Node, value: A): DataRecord[A] = node match
    case elem: Elem =>
      val ns  = scalaxb.Helper.nullOrEmpty(elem.scope.getURI(elem.prefix))
      val key = Some(elem.label)
      DataRecord(ns, key, value)
    case _          => DataRecord(value)

  // this is for long attributes
  def apply[A: CanWriteXML](x: Node, parent: Node, value: A): DataRecord[A] = x match
    case _ => DataRecord(value)

  def apply[A: CanWriteXML](value: A): DataRecord[A] =
    apply(None, None, value)

  def apply[A: CanWriteXML](
    namespace: Option[String],
    key: Option[String],
    xstypeNamespace: Option[String],
    xstypeName: Option[String],
    value: A
  ): DataRecord[A] =
    DataWriter(namespace, key, xstypeNamespace, xstypeName, value, implicitly[CanWriteXML[A]])

  // this is for any.
  def apply(elemName: ElemName)(implicit handleNonDefault: scala.xml.Elem => Option[DataRecord[Any]]): DataRecord[Any] =
    fromAny(elemName.node, handleNonDefault)

  def fromAny(seq: NodeSeq, handleNonDefault: scala.xml.Elem => Option[DataRecord[Any]]): DataRecord[Any] =
    seq match
      case elem: Elem => fromAny(elem, handleNonDefault)
      case _          => DataRecord(None, None, None, None, seq.text)

  def fromAny(elem: Elem, handleNonDefault: scala.xml.Elem => Option[DataRecord[Any]]): DataRecord[Any] =
    val ns  = scalaxb.Helper.nullOrEmpty(elem.scope.getURI(elem.prefix))
    val key = Some(elem.label)
    val XS  = Some(XML_SCHEMA_URI)

    instanceType(elem) match
      case (XS, xstype) =>
        xstype match
          case Some("int")                => DataRecord(ns, key, XS, xstype, fromXML[Int](elem, Nil))
          case Some("byte")               => DataRecord(ns, key, XS, xstype, fromXML[Byte](elem, Nil))
          case Some("short")              => DataRecord(ns, key, XS, xstype, fromXML[Short](elem, Nil))
          case Some("long")               => DataRecord(ns, key, XS, xstype, fromXML[Long](elem, Nil))
          case Some("float")              => DataRecord(ns, key, XS, xstype, fromXML[Float](elem, Nil))
          case Some("double")             => DataRecord(ns, key, XS, xstype, fromXML[Double](elem, Nil))
          case Some("integer")            => DataRecord(ns, key, XS, xstype, fromXML[BigInt](elem, Nil))
          case Some("nonPositiveInteger") => DataRecord(ns, key, XS, xstype, fromXML[BigInt](elem, Nil))
          case Some("negativeInteger")    => DataRecord(ns, key, XS, xstype, fromXML[BigInt](elem, Nil))
          case Some("nonNegativeInteger") => DataRecord(ns, key, XS, xstype, fromXML[BigInt](elem, Nil))
          case Some("positiveInteger")    => DataRecord(ns, key, XS, xstype, fromXML[BigInt](elem, Nil))
          case Some("unsignedLong")       => DataRecord(ns, key, XS, xstype, fromXML[BigInt](elem, Nil))
          case Some("unsignedInt")        => DataRecord(ns, key, XS, xstype, fromXML[Long](elem, Nil))
          case Some("unsignedShort")      => DataRecord(ns, key, XS, xstype, fromXML[Int](elem, Nil))
          case Some("unsignedByte")       => DataRecord(ns, key, XS, xstype, fromXML[Int](elem, Nil))
          case Some("decimal")            => DataRecord(ns, key, XS, xstype, fromXML[BigDecimal](elem, Nil))
          case Some("boolean")            => DataRecord(ns, key, XS, xstype, fromXML[Boolean](elem, Nil))
          case Some("string")             => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("normalizedString")   => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("token")              => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("language")           => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("Name")               => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("NCName")             => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("NMTOKEN")            => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("NMTOKENS")           => DataRecord(ns, key, XS, xstype, fromXML[Seq[String]](elem, Nil))
          case Some("ID")                 => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("IDREF")              => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("IDREFS")             => DataRecord(ns, key, XS, xstype, fromXML[Seq[String]](elem, Nil))
          case Some("ENTITY")             => DataRecord(ns, key, XS, xstype, fromXML[String](elem, Nil))
          case Some("ENTITIES")           => DataRecord(ns, key, XS, xstype, fromXML[Seq[String]](elem, Nil))
          case Some("hexBinary")          => DataRecord(ns, key, XS, xstype, fromXML[HexBinary](elem, Nil))
          case Some("base64Binary")       => DataRecord(ns, key, XS, xstype, fromXML[Base64Binary](elem, Nil))
          case Some("anyURI")             => DataRecord(ns, key, XS, xstype, fromXML[java.net.URI](elem, Nil))
          case Some("QName")              => DataRecord(ns, key, XS, xstype, fromXML[javax.xml.namespace.QName](elem, Nil))
          case Some("NOTATION")           => DataRecord(ns, key, XS, xstype, fromXML[javax.xml.namespace.QName](elem, Nil))
          case Some("duration")           => DataRecord(ns, key, XS, xstype, fromXML[javax.xml.datatype.Duration](elem, Nil))
          case Some("dateTime")           => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))
          case Some("time")               => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))
          case Some("gYearMonth")         => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))
          case Some("gYear")              => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))
          case Some("gMonthDay")          => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))
          case Some("gDay")               => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))
          case Some("gMonth")             => DataRecord(ns, key, XS, xstype, fromXML[XMLGregorianCalendar](elem, Nil))

          case _ => DataRecord(ns, key, XS, xstype, elem)
      case _            =>
        handleNonDefault(elem).getOrElse {
          val (xsns, xstype) = instanceType(elem)
          DataRecord(ns, key, xsns, xstype, elem)
        }
    end match
  end fromAny

  // this is for any.
  def fromNillableAny(seq: NodeSeq): DataRecord[Option[Any]] =
    seq match
      case elem: Elem => fromNillableAny(elem)
      case _          => DataRecord(None, None, None, None, Some(seq.text))

  // this is for any.
  def fromNillableAny(elem: Elem): DataRecord[Option[Any]] =
    val ns  = scalaxb.Helper.nullOrEmpty(elem.scope.getURI(elem.prefix))
    val key = Some(elem.label)
    val XS  = Some(XML_SCHEMA_URI)

    if isNil(elem) then DataRecord(ns, key, None)
    else
      instanceType(elem) match
        case (XS, xstype) =>
          xstype match
            case Some("int")                => DataRecord(ns, key, XS, xstype, Some(fromXML[Int](elem, Nil)))
            case Some("byte")               => DataRecord(ns, key, XS, xstype, Some(fromXML[Byte](elem, Nil)))
            case Some("short")              => DataRecord(ns, key, XS, xstype, Some(fromXML[Short](elem, Nil)))
            case Some("long")               => DataRecord(ns, key, XS, xstype, Some(fromXML[Long](elem, Nil)))
            case Some("float")              => DataRecord(ns, key, XS, xstype, Some(fromXML[Float](elem, Nil)))
            case Some("double")             => DataRecord(ns, key, XS, xstype, Some(fromXML[Double](elem, Nil)))
            case Some("integer")            => DataRecord(ns, key, XS, xstype, Some(fromXML[BigInt](elem, Nil)))
            case Some("nonPositiveInteger") => DataRecord(ns, key, XS, xstype, Some(fromXML[BigInt](elem, Nil)))
            case Some("negativeInteger")    => DataRecord(ns, key, XS, xstype, Some(fromXML[BigInt](elem, Nil)))
            case Some("nonNegativeInteger") => DataRecord(ns, key, XS, xstype, Some(fromXML[BigInt](elem, Nil)))
            case Some("positiveInteger")    => DataRecord(ns, key, XS, xstype, Some(fromXML[BigInt](elem, Nil)))
            case Some("unsignedLong")       => DataRecord(ns, key, XS, xstype, Some(fromXML[BigInt](elem, Nil)))
            case Some("unsignedInt")        => DataRecord(ns, key, XS, xstype, Some(fromXML[Long](elem, Nil)))
            case Some("unsignedShort")      => DataRecord(ns, key, XS, xstype, Some(fromXML[Int](elem, Nil)))
            case Some("unsignedByte")       => DataRecord(ns, key, XS, xstype, Some(fromXML[Int](elem, Nil)))
            case Some("decimal")            => DataRecord(ns, key, XS, xstype, Some(fromXML[BigDecimal](elem, Nil)))
            case Some("boolean")            => DataRecord(ns, key, XS, xstype, Some(fromXML[Boolean](elem, Nil)))
            case Some("string")             => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("normalizedString")   => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("token")              => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("language")           => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("Name")               => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("NCName")             => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("NMTOKEN")            => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("NMTOKENS")           => DataRecord(ns, key, XS, xstype, Some(fromXML[Seq[String]](elem, Nil)))
            case Some("ID")                 => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("IDREF")              => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("IDREFS")             => DataRecord(ns, key, XS, xstype, Some(fromXML[Seq[String]](elem, Nil)))
            case Some("ENTITY")             => DataRecord(ns, key, XS, xstype, Some(fromXML[String](elem, Nil)))
            case Some("ENTITIES")           => DataRecord(ns, key, XS, xstype, Some(fromXML[Seq[String]](elem, Nil)))
            case Some("hexBinary")          => DataRecord(ns, key, XS, xstype, Some(fromXML[HexBinary](elem, Nil)))
            case Some("base64Binary")       => DataRecord(ns, key, XS, xstype, Some(fromXML[Base64Binary](elem, Nil)))
            case Some("anyURI")             => DataRecord(ns, key, XS, xstype, Some(fromXML[java.net.URI](elem, Nil)))
            case Some("QName")              => DataRecord(ns, key, XS, xstype, Some(fromXML[javax.xml.namespace.QName](elem, Nil)))
            case Some("NOTATION")           =>
              DataRecord(ns, key, XS, xstype, Some(fromXML[javax.xml.namespace.QName](elem, Nil)))
            case Some("duration")           =>
              DataRecord(ns, key, XS, xstype, Some(fromXML[javax.xml.datatype.Duration](elem, Nil)))
            case Some("dateTime")           => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))
            case Some("time")               => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))
            case Some("gYearMonth")         => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))
            case Some("gYear")              => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))
            case Some("gMonthDay")          => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))
            case Some("gDay")               => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))
            case Some("gMonth")             => DataRecord(ns, key, XS, xstype, Some(fromXML[XMLGregorianCalendar](elem, Nil)))

            case _ => DataRecord(ns, key, XS, xstype, Some(elem))
        case _            =>
          val (xsns, xstype) = instanceType(elem)
          DataRecord(ns, key, xsns, xstype, Some(elem))
    end if
  end fromNillableAny

  def unapply[A](record: DataRecord[A]): Option[(Option[String], Option[String], A)] =
    Some((record.namespace, record.key, record.value))

  def toXML[A](
    obj: DataRecord[A],
    namespace: Option[String],
    elementLabel: Option[String],
    scope: scala.xml.NamespaceBinding,
    typeAttribute: Boolean
  ): scala.xml.NodeSeq = obj match
    case w: DataWriter[?] =>
      obj.value match
        case seq: NodeSeq =>
          w.writer.asInstanceOf[CanWriteXML[A]].writes(obj.value, namespace, elementLabel, scope, typeAttribute)
        case _            =>
          w.writer.asInstanceOf[CanWriteXML[A]].writes(obj.value, namespace, elementLabel, scope, false) match
            case elem: Elem if (w.xstypeName.isDefined && scope.getPrefix(XSI_URL) != null) =>
              elem % scala.xml.Attribute(
                scope.getPrefix(Helper.XSI_URL),
                "type",
                Helper.prefixedName(w.xstypeNamespace, w.xstypeName.get, scope),
                scala.xml.Null
              )
            case x                                                                          => x
    case _                => sys.error("unknown DataRecord.")
end DataRecord

case class ElemName(namespace: Option[String], name: String):
  var node: scala.xml.Node        = scala.compiletime.uninitialized
  def text                        = node.text
  def nil                         = Helper.isNil(node)
  def nilOption: Option[ElemName] = if nil then None else Some(this)
  def splitBySpace                = Helper.splitBySpace(text)
  override def toString           = namespace match
    case Some(x) => "{%s}%s".format(x, name)
    case _       => name

object ElemName:
  implicit def apply(node: scala.xml.Node): ElemName = node match
    case x: scala.xml.Elem =>
      val elemName = ElemName(scalaxb.Helper.nullOrEmpty(x.scope.getURI(x.prefix)), x.label)
      elemName.node = x
      elemName
    case _                 =>
      val elemName = ElemName(None, "")
      elemName.node = node
      elemName

  implicit def toNodeSeq(elem: ElemName): scala.xml.NodeSeq = elem.node
end ElemName

trait AnyElemNameParser extends scala.util.parsing.combinator.Parsers:
  import scala.collection.mutable.ListBuffer
  import scala.annotation.tailrec
  type Elem = ElemName

  implicit class ReaderExt(reader: scala.util.parsing.input.Reader[ElemName]):

    def erased(from: Int, to: Int): scala.util.parsing.input.Reader[ElemName] =
      reader match
        case r: ElemNameSeqReader =>
          new ElemNameSeqReader(r.seq.take(from) ++ r.seq.drop(to))
        case other                =>
          throw new UnsupportedOperationException("Incompatible reader")

  implicit def parserViewToParserExt[T, P](current: P)(implicit ev: P => Parser[T]): ParserExt[T, P] =
    new ParserExt[T, P](current, ev)
  implicit def parserToParserExt[T](current: Parser[T]): ParserExt[T, Parser[T]]                     =
    new ParserExt[T, Parser[T]](current, identity)
  class ParserExt[+T, P](current: P, ev0: P => Parser[T]):
    implicit val ev: P => Parser[T]                                                                           = ev0
    private def parseIterable[U](source: Input, in: Input, p: Parser[U], res: ParseResult[U]): ParseResult[U] =
      res match
        case s @ Success(v: Option[?], n) if v.isEmpty   =>
          if !n.atEnd then parseIterable(source, in.rest, p, p(in.rest)) else s.copy(next = source)
        case s @ Success(v: Iterable[?], n) if v.isEmpty =>
          if !n.atEnd then parseIterable(source, in.rest, p, p(in.rest)) else s.copy(next = source)
        case s @ Success(v, n)                           =>
          s.copy(next = n.erased(in.pos.column - 1, n.pos.column - 1))
        case other                                       =>
          other

    private def lookupSuccess[U](p: Parser[U], input: Input): ParseResult[U] = p(input) match
      case s @ Success(v, next) =>
        parseIterable(input, input, p, s)
      case n @ Failure(b, next) =>
        if next.atEnd then n
        else lookupSuccess(p, input.rest)
      case n @ Error(b, next)   =>
        if next.atEnd then n
        else lookupSuccess(p, input.rest)

    def ~|~[U](nextParser: => Parser[U]): Parser[~|~[T, U]] =
      Parser { in =>
        lookupSuccess(current.asInstanceOf[Parser[T]], in) match
          case s @ Success(currentResult, afterFirstInput) =>
            lookupSuccess(nextParser, afterFirstInput) match
              case qs @ Success(nextResult, afterSecondInput) =>
                Success(new ~|~(currentResult, nextResult), afterSecondInput)
              case n: NoSuccess                               => n
          case n: NoSuccess                                => n
      }
  end ParserExt

  case class ~|~[+A, +B](a: A, b: B):
    override def toString() = s"$a ~|~ $b"

  // we need this so treat ElemName as NodeSeq for fromXML etc.
  implicit def toNodeSeq(elem: Elem): scala.xml.NodeSeq = elem.node

  def optPhrase[U](p: Parser[U]): Parser[U] = phrase(p ~|~ safeRep(any(_ => true)) ^^ { case p1 ~|~ p2 => p1 })

  def any(f: ElemName => Boolean): Parser[ElemName] =
    accept("any", { case x: ElemName if x.name != "" && f(x) => x })

  def optTextRecord(implicit format: XMLFormat[String]): Parser[Option[DataRecord[Any]]] =
    opt(text ^^ (x => DataRecord(x.node.text)(using format)))

  def text: Parser[ElemName] =
    accept("text", { case x: ElemName if x.name == "" => x })

  def safeRep[T](p: => Parser[T]): Parser[List[T]] = safeRep1(p) | success(List())

  def safeRep1[T](first: => Parser[T], p0: => Parser[T]): Parser[List[T]] = Parser { in =>
    lazy val p = p0 // lazy argument
    val elems  = new ListBuffer[T]

    def continue(in: Input): ParseResult[List[T]] =
      val p0                                                = p // avoid repeatedly re-evaluating by-name parser
      @tailrec def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match
        case Success(x, rest) =>
          if !(in0.pos < rest.pos) || in0.atEnd then Success(elems.toList, in0)
          else
            elems += x
            applyp(rest)
        case e @ Error(_, _)  => e // still have to propagate error
        case _                => Success(elems.toList, in0)
      applyp(in)
    end continue
    first(in) match
      case Success(x, rest) =>
        elems += x
        continue(rest)
      case ns: NoSuccess    => ns
  }

  def safeRep1[T](p: => Parser[T]): Parser[List[T]] = safeRep1(p, p)
end AnyElemNameParser

trait CanWriteChildNodes[A] extends CanWriteXML[A]:
  def targetNamespace: Option[String]
  def typeName: Option[String]                                                       = None
  def writesAttribute(obj: A, scope: scala.xml.NamespaceBinding): scala.xml.MetaData = scala.xml.Null
  def writesChildNodes(obj: A, scope: scala.xml.NamespaceBinding): Seq[scala.xml.Node]

  def writes(
    obj: A,
    namespace: Option[String],
    elementLabel: Option[String],
    scope: scala.xml.NamespaceBinding,
    typeAttribute: Boolean
  ): scala.xml.NodeSeq =
    val elem = scala.xml.Elem(
      Helper.getPrefix(namespace, scope).orNull,
      elementLabel getOrElse { sys.error("missing element label.") },
      writesAttribute(obj, scope),
      scope,
      true,
      writesChildNodes(obj, scope)*
    )
    if typeAttribute && typeName.isDefined && scope.getPrefix(Helper.XSI_URL) != null then
      elem % scala.xml.Attribute(
        scope.getPrefix(Helper.XSI_URL),
        "type",
        Helper.prefixedName(targetNamespace, typeName.get, scope),
        scala.xml.Null
      )
    else elem
  end writes
end CanWriteChildNodes

trait AttributeGroupFormat[A] extends scalaxb.XMLFormat[A]:
  def writes(
    __obj: A,
    __namespace: Option[String],
    __elementLabel: Option[String],
    __scope: scala.xml.NamespaceBinding,
    __typeAttribute: Boolean
  ): scala.xml.NodeSeq = sys.error("don't call me.")

  def toAttribute(__obj: A, __attr: scala.xml.MetaData, __scope: scala.xml.NamespaceBinding): scala.xml.MetaData
end AttributeGroupFormat

trait ElemNameParser[A] extends AnyElemNameParser with XMLFormat[A] with CanWriteChildNodes[A]:
  def reads(seq: scala.xml.NodeSeq, stack: List[ElemName]): Either[String, A] = seq match
    case node: scala.xml.Node =>
      parse(parser(node, stack), node.child) match
        case x: Success[?] => Right(x.get)
        case x: Failure    => Left(parserErrorMsg(x.msg, x.next, ElemName(node) :: stack))
        case x: Error      => Left(parserErrorMsg(x.msg, node))
    case _                    => Left("seq must be scala.xml.Node")

  private def parserErrorMsg(msg: String, next: scala.util.parsing.input.Reader[Elem], stack: List[ElemName]): String =
    if msg `contains` "parser error " then msg
    else "parser error \"" + msg + "\" while parsing " + stack.reverse.mkString("/", "/", "/") + next.pos.longString

  private def parserErrorMsg(msg: String, node: scala.xml.Node): String =
    if msg `contains` "parser error " then msg
    else "parser error \"" + msg + "\" while parsing " + node.toString

  def parser(node: scala.xml.Node, stack: List[ElemName]): Parser[A]
  def isMixed: Boolean = false

  def parse[B](p: Parser[B], in: Seq[scala.xml.Node]): ParseResult[B] =
    p(new ElemNameSeqReader(elementNames(in)))

  def elementNames(in: Seq[scala.xml.Node]): Seq[ElemName] =
    if isMixed then in map { x => ElemName(x) } else in collect { case x: scala.xml.Elem => ElemName(x) }
end ElemNameParser

class ElemNameSeqReader(val seq: Seq[ElemName], override val offset: Int)
    extends scala.util.parsing.input.Reader[ElemName]:
  import scala.util.parsing.input.*

  def this(seq: Seq[ElemName]) = this(seq, 0)

  override def first: ElemName =
    if seq.isDefinedAt(offset) then seq(offset)
    else null

  def rest: ElemNameSeqReader =
    if seq.isDefinedAt(offset) then new ElemNameSeqReader(seq, offset + 1)
    else this

  def pos: Position = new ElemNameSeqPosition(seq, offset)

  def atEnd = !seq.isDefinedAt(offset)

  override def drop(n: Int): ElemNameSeqReader =
    new ElemNameSeqReader(seq, offset + n)
end ElemNameSeqReader

class ElemNameSeqPosition(val source: Seq[ElemName], val offset: Int) extends scala.util.parsing.input.Position:
  protected def lineContents =
    source.mkString

  override def line   = 1
  override def column = offset + 1

class HexBinary(_vector: Vector[Byte]) extends scala.collection.IndexedSeq[Byte]:
  val vector                    = _vector
  def length                    = vector.length
  def apply(idx: Int): Byte     = vector(idx)
  override def toString: String = DatatypeConverter.printHexBinary(vector.toArray)

object HexBinary:
  def apply(xs: Byte*): HexBinary =
    val vector: Vector[Byte] = (xs.toIndexedSeq map { (x: Byte) => x }).to(Vector)
    new HexBinary(vector)

  def apply(value: String): HexBinary =
    val array = DatatypeConverter.parseHexBinary(value)
    apply(array.toSeq*)

  def unapplySeq(x: HexBinary) = Some(x.vector)
end HexBinary

class Base64Binary(_vector: Vector[Byte]) extends scala.collection.IndexedSeq[Byte]:
  val vector                    = _vector
  def length                    = vector.length
  def apply(idx: Int): Byte     = vector(idx)
  override def toString: String = DatatypeConverter.printBase64Binary(vector.toArray)

object Base64Binary:
  def apply(xs: Byte*): Base64Binary =
    val vector: Vector[Byte] = (xs.toIndexedSeq map { (x: Byte) => x }).to(Vector)
    new Base64Binary(vector)

  def apply(value: String): Base64Binary =
    val array = DatatypeConverter.parseBase64Binary(value)
    apply(array.toSeq*)

  def unapplySeq(x: Base64Binary) = Some(x.vector)
end Base64Binary

object XMLCalendar:
  def apply(value: String): XMLGregorianCalendar           = Helper.toCalendar(value)
  def unapply(value: XMLGregorianCalendar): Option[String] = Some(value.toXMLFormat)

object DataTypeFactory extends ThreadLocal[javax.xml.datatype.DatatypeFactory]:
  override def initialValue() = javax.xml.datatype.DatatypeFactory.newInstance()

object Helper:
  val XML_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema"
  val XSI_URL        = "http://www.w3.org/2001/XMLSchema-instance"
  val XSI_PREFIX     = "xsi"

  def toString(value: QName, scope: NamespaceBinding): String =
    Option[String](scope.getPrefix(value.getNamespaceURI)) map {
      "%s:%s".format(_, value.getLocalPart)
    } getOrElse { value.getLocalPart }

  def toCalendar(value: String): XMLGregorianCalendar =
    DataTypeFactory.get().newXMLGregorianCalendar(value)

  def toCalendar(value: java.util.GregorianCalendar): XMLGregorianCalendar =
    import javax.xml.datatype.*
    import java.util.{GregorianCalendar, Calendar as JCalendar}

    val xmlGregorian = DataTypeFactory.get().newXMLGregorianCalendar()
    if value.getTimeZone != null then xmlGregorian.setTimezone(value.getTimeZone.getRawOffset / 60000)

    if value.isSet(JCalendar.YEAR) then
      xmlGregorian.setYear(
        if value.get(JCalendar.ERA) == GregorianCalendar.AD then value.get(JCalendar.YEAR)
        else -value.get(JCalendar.YEAR)
      )
    if value.isSet(JCalendar.MONTH) then
      xmlGregorian.setMonth(value.get(JCalendar.MONTH) - JCalendar.JANUARY + DatatypeConstants.JANUARY)
    if value.isSet(JCalendar.DAY_OF_MONTH) then xmlGregorian.setDay(value.get(JCalendar.DAY_OF_MONTH))
    if value.isSet(JCalendar.HOUR_OF_DAY) then xmlGregorian.setHour(value.get(JCalendar.HOUR_OF_DAY))
    if value.isSet(JCalendar.MINUTE) then xmlGregorian.setMinute(value.get(JCalendar.MINUTE))
    if value.isSet(JCalendar.SECOND) then xmlGregorian.setSecond(value.get(JCalendar.SECOND))
    if value.isSet(JCalendar.MILLISECOND) && value.get(JCalendar.MILLISECOND) > 0 then
      xmlGregorian.setFractionalSecond(new java.math.BigDecimal(value.get(JCalendar.MILLISECOND)))

    xmlGregorian
  end toCalendar

  def toDuration(value: String) =
    DataTypeFactory.get().newDuration(value)

  def toURI(value: String) =
    java.net.URI.create(value.trim)

  def isNil(node: scala.xml.Node) =
    (node \ ("@{" + XSI_URL + "}nil")).headOption map { case x => x.text == "true" || x.text == "1" } getOrElse {
      false
    }

  def nilElem(namespace: Option[String], elementLabel: String, scope: scala.xml.NamespaceBinding) =
    scala.xml.Elem(
      getPrefix(namespace, scope).orNull,
      elementLabel,
      scala.xml.Attribute(scope.getPrefix(XSI_URL), "nil", "true", scala.xml.Null),
      scope,
      true
    )

  def splitBySpace(text: String) = text.split(' ').filter(_ != "")

  def instanceType(node: scala.xml.Node): (Option[String], Option[String]) =
    val typeName  = (node \ ("@{" + XSI_URL + "}type")).text
    val prefix    =
      if typeName.contains(':') then Some(typeName.dropRight(typeName.length - typeName.indexOf(':')))
      else None
    val namespace = scalaxb.Helper.nullOrEmpty(node.scope.getURI(prefix.orNull))
    val value     =
      if typeName.contains(':') then typeName.drop(typeName.indexOf(':') + 1)
      else typeName
    (namespace, if value == "" then None else Some(value))
  end instanceType

  def splitQName(value: String, scope: scala.xml.NamespaceBinding): (Option[String], String) =
    if value `startsWith` "{" then
      val qname = javax.xml.namespace.QName.valueOf(value)
      (nullOrEmpty(qname.getNamespaceURI), qname.getLocalPart)
    else if value contains ':' then
      val prefix    = value.dropRight(value.length - value.indexOf(':'))
      val localPart = value.drop(value.indexOf(':') + 1)
      (nullOrEmpty(scope.getURI(prefix)), localPart)
    else (nullOrEmpty(scope.getURI(null)), value)

  def nullOrEmpty(value: String): Option[String] =
    value match
      case null | "" => None
      case x         => Some(x)

  def getPrefix(namespace: Option[String], scope: scala.xml.NamespaceBinding) =
    if nullOrEmpty(scope.getURI(null)) == namespace then None
    else nullOrEmpty(scope.getPrefix(namespace.orNull))

  def prefixedName(namespace: Option[String], name: String, scope: scala.xml.NamespaceBinding) =
    getPrefix(namespace, scope) map { """%s:%s""".format(_, name) } getOrElse { name }

  def stringToXML(
    obj: String,
    namespace: Option[String],
    elementLabel: Option[String],
    scope: scala.xml.NamespaceBinding
  ): scala.xml.NodeSeq =
    elementLabel map { label =>
      scala.xml.Elem(
        getPrefix(namespace, scope).orNull,
        label,
        scala.xml.Null,
        scope,
        true,
        scala.xml.Text(obj.toString)
      )
    } getOrElse { scala.xml.Text(obj) }

  // assume outer scope
  def mergeNodeSeqScope(nodeseq: NodeSeq, outer: NamespaceBinding): NodeSeq =
    nodeseq.toSeq flatMap { mergeNodeScope(_, outer) }

  // assume outer scope
  def mergeNodeScope(node: Node, outer: NamespaceBinding): Node =
    node match
      case elem: Elem =>
        withInnerScope(elem.scope, outer) { (innerScope, mapping) =>
          val newPrefix: String = mapping.get(scalaxb.Helper.nullOrEmpty(elem.prefix)) map { _.orNull } getOrElse {
            elem.prefix
          }
          val newChild          = mergeNodeSeqScope(mergeNodeSeqScope(elem.child, outer), innerScope)
          elem.copy(scope = innerScope, prefix = newPrefix, child = newChild)
        }
      case _          => node

  def withInnerScope[A](scope: NamespaceBinding, outer: NamespaceBinding)(
    f: (NamespaceBinding, Map[Option[String], Option[String]]) => A
  ): A =
    val outerList                                                    = fromScope(outer)
    def renamePrefix(prefix: Option[String], n: Int): Option[String] =
      if outerList exists { case (p, n) => p == Some((prefix getOrElse { "ns" }) + n.toString) } then
        renamePrefix(prefix, n + 1)
      else Some((prefix getOrElse { "ns" }) + n.toString)

    val xs: List[((Option[String], String), (Option[String], Option[String]))] = fromScope(scope) flatMap {
      case (prefix, ns) if outerList contains (prefix -> ns)                              => None
      case (prefix, ns) if outerList exists { case (p, n) => p == prefix && p.isDefined } =>
        val renamed = renamePrefix(prefix, 2)
        Some((renamed -> ns, prefix -> renamed))
      case (prefix, ns)                                                                   => Some((prefix -> ns, prefix -> prefix))
    }

    f(toScope(xs map { _._1 }*), Map(xs map { _._2 }*))
  end withInnerScope

  def resolveSoap11Refs(node: Node): Node =
    import scala.xml.transform.*

    def lookupRef(id: String): Seq[Node] =
      node.child flatMap {
        case elem: Elem if (elem \ "@id").text == id =>
          if (elem \ "@{http://schemas.xmlsoap.org/soap/encoding/}root").text == "1" then elem
          else elem.child.toSeq
        case _                                       => Nil
      }
    val rule                             = new RewriteRule:
      override def transform(n: Node): Seq[Node] = n match
        case x @ Elem(prefix, label, attr, scope, _*) if attr exists (p => p.key == "href") =>
          Elem(prefix, label, attr remove ("href"), scope, true, lookupRef((x \ "@href").text.tail)*)
        case x @ Elem(prefix, label, attr, scope, _*) if attr exists (p => p.key == "id")   =>
          Nil
        case other                                                                          => other
    val rt                               = new RuleTransformer(rule)
    var retval: Node                     = node
    while retval != rt(retval) do
      retval = rt(retval)
      // while
    retval
  end resolveSoap11Refs
end Helper

class ParserFailure(message: String) extends RuntimeException(message)
