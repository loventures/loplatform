package loi.apm

object Apm:
  def ignoreTransaction(): Unit                                = ()
  def ignoreApdex(): Unit                                      = ()
  def getBrowserTimingHeader: String                           = ""
  def getBrowserTimingFooter: String                           = ""
  def recordMetric(name: String, value: Any): Unit             = ()
  def incrementCounter(name: String): Unit                     = ()
  def addCustomParameter(name: String, value: Any): Unit       = ()
  def noticeError(ex: Throwable): Unit                         = ()
  def setTransactionName(category: String, name: String): Unit = ()

  def startSegment(language: String, signature: String): Segment = NopSegment

trait Segment:
  def end(): Unit

object NopSegment extends Segment:
  override def end(): Unit = ()
