package loi.cp.url

import java.net.spi.URLStreamHandlerProvider
import java.net.{URL, URLStreamHandler}

class ClasspathURLStreamHandlerProvider extends URLStreamHandlerProvider:
  def createURLStreamHandler(protocol: String): URLStreamHandler =
    if "classpath" != protocol then null
    else (u: URL) => ClassLoader.getSystemClassLoader.getResource(u.getPath).openConnection
