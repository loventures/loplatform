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

package com.learningobjects.cpxp.util

import java.util.function.Supplier
import org.jclouds.aws.AWSResponseException

import scala.util.Random

/** Utilities for performing operations on blob stores that may fail with a retryable error.
  */
object BlobStoreUtils:
  private[util] final val logger = org.log4s.getLogger

  final val AWS_INTERNAL_ERROR_CODE  = "InternalError"
  final val AWS_SLOW_DOWN_ERROR_CODE = "SlowDown"
  final val MAX_BLOB_STORE_RETRIES   = 4

  /** Attempt a blob store operation.
    *
    * @param supplier
    *   the operation
    * @param meta
    *   information about the operation
    * @param stats
    *   where to record operation statistics
    * @tparam T
    *   the operation return type
    * @return
    *   the operation result
    */
  def attemptBlobStoreOperation[T](supplier: Supplier[T], meta: S3Meta, stats: S3Statistics): T =
    repeatBlobStoreOperation(supplier, meta, stats, 1)

  private def repeatBlobStoreOperation[T](
    supplier: Supplier[T],
    meta: S3Meta,
    stats: S3Statistics,
    retryCount: Int
  ): T =
    val s3 = stats.begin(meta.size)

    def retryStoreOp(): T =
      // backoff 64-128, 256-512, 1024-2048 ms so at most about 2.5 seconds
      val min = 16 << (retryCount * 2)
      val max = 32 << (retryCount * 2)
      val rnd = min + Random.nextLong(max - min)
      logger.info("Retry backoff #" + retryCount + " for " + rnd + "ms")
      Thread.sleep(rnd)
      repeatBlobStoreOperation(supplier, meta, stats, 1 + retryCount)

    try
      val result = supplier.get
      s3.succeeded()
      result
    catch
      case NestedAWSException(ex)
          if retryableErrorCodes.contains(
            ex.getError.getCode
          ) && meta.repeatable && retryCount < MAX_BLOB_STORE_RETRIES =>
        logger.warn(ex)("Retryable AWS error during blob store operation")
        s3.failed()
        retryStoreOp()

      case e @ NestedAWSException(awse) =>
        logger.warn(awse)("Non-retryable AWS error during blob store operation: " + awse.getError.getCode)
        s3.failed()
        throw e

      case th: Throwable =>
        logger.warn(th)("Unexpected error during blob store operation: " + th.getClass.getName)
        // attributing all errors to s3 is questionable but it does UnknownHostException sometimes
        s3.failed()
        if meta.retryUnknown && retryCount < MAX_BLOB_STORE_RETRIES then retryStoreOp()
        else throw th
    end try
  end repeatBlobStoreOperation

  private final val retryableErrorCodes = Set(AWS_INTERNAL_ERROR_CODE, AWS_SLOW_DOWN_ERROR_CODE)

  /** Match an AWS exception, even if nested within a distant cause. */
  object NestedAWSException:
    def unapply(th: Throwable): Option[AWSResponseException] = th match
      case aws: AWSResponseException => Some(aws)
      case t                         => Option(t.getCause).flatMap(unapply)
end BlobStoreUtils
