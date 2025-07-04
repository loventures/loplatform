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

package loi.cp.password

import com.learningobjects.cpxp.component.ComponentDescriptor
import com.learningobjects.cpxp.scala.util.I18n.*
import com.learningobjects.cpxp.service.login.LoginWebService.LoginStatus
import com.learningobjects.cpxp.service.user.LoginRecord
import com.learningobjects.cpxp.util.DateUtils
import loi.cp.user.UserComponent
import org.apache.commons.codec.digest.DigestUtils
import scalaz.*
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.validation.*

import java.util.Date
import java.util.regex.Pattern
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.matching.Regex

object PasswordPolicyUtils:

  val LatinPat        = Pattern.compile("\\p{IsLatin}")
  val DigitPat        = Pattern.compile("\\p{IsDigit}")
  val UpperLatinPat   = Pattern.compile("[\\p{IsLatin}&&\\p{Lu}]")
  val LowerLatinPat   = Pattern.compile("[\\p{IsLatin}&&[^\\p{Lu}]]")
  val PunctOrSpacePat = Pattern.compile("[ \\p{Punct}]")

  // TODO: i18n
  def minLength(minLength: Integer)(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if password.length >= minLength =>
      password.successNel
    case _                                                  =>
      minLengthError(minLength).failureNel
  }

  def minLengthError(minLength: Integer)(implicit cd: ComponentDescriptor) =
    i"Password must be at least $minLength characters long."

  /** Evaluates if the password is contained within given number of elements of the a given password history. XXX: Could
    * be generalized to apply a predicate to the password history
    */
  def unique(passwordHistory: LazyList[Password], userId: Long)(
    lookBack: Integer
  )(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password
        if !passwordHistory
          .takeRight(lookBack)
          .contains(encodePassword(userId, password)) =>
      password.successNel
    case _ =>
      uniqueError(lookBack).failureNel
  }

  def uniqueError(lookBack: Integer)(implicit cd: ComponentDescriptor) =
    i"Password may not match one of the previous $lookBack passwords."

  def encodePassword(userId: Long, password: String): String =
    DigestUtils.sha1Hex(userId.toString + "\u0000" + password)

  def regex(regexp: Regex, errorMessage: Error): PasswordValidator = {
    case regexp(validPass) => validPass.successNel
    case _                 => errorMessage.failureNel
  }

  // tests whether the field is nonempty and matches the start of the password
  private def startsWith(password: Password, field: String): Boolean =
    Option(field).fold(false)(s => !s.isEmpty && password.toLowerCase.startsWith(s.toLowerCase))

  // username is often equal to email address so these checks are combined
  def notUserField(user: UserComponent)(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if startsWith(password, user.getEmailAddress) =>
      userFieldError.failureNel
    case password: Password if startsWith(password, user.getUserName)     =>
      userFieldError.failureNel
    case password: Password                                               => password.successNel
  }

  def userFieldError(implicit cd: ComponentDescriptor) =
    i"Password may not match your username or email address."

  def alpha(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if LatinPat.matcher(password).find() =>
      password.successNel
    case _                                                       =>
      alphaError.failureNel
  }

  def alphaError(implicit cd: ComponentDescriptor) =
    i"Password must contain at least one alphabetic character."

  def numeric(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if DigitPat.matcher(password).find() =>
      password.successNel
    case _                                                       =>
      numericError.failureNel
  }

  def numericError(implicit cd: ComponentDescriptor) =
    i"Password must contain at least one numeric character."

  def upper(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if UpperLatinPat.matcher(password).find() =>
      password.successNel
    case _                                                            =>
      upperError.failureNel
  }

  def upperError(implicit cd: ComponentDescriptor) =
    i"Password must contain at least one upper case letter."

  def lower(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if LowerLatinPat.matcher(password).find() =>
      password.successNel
    case _                                                            =>
      lowerError.failureNel
  }

  def lowerError(implicit cd: ComponentDescriptor) =
    i"Password must contain at least one lower case letter."

  def hasNonAlpha(implicit cd: ComponentDescriptor): PasswordValidator = {
    case password: Password if PunctOrSpacePat.matcher(password).find() =>
      password.successNel
    case _                                                              =>
      nonAlphaError.failureNel
  }

  def nonAlphaError(implicit cd: ComponentDescriptor) =
    i"Password must contain at least one symbolic character."

  def alphaNumeric(implicit cd: ComponentDescriptor): PasswordValidator =
    (password: Password) =>
      (alpha(using cd)(password) |@| numeric(using cd)(password)) { (a, b) =>
        password
      } // always return the original password.

  def alphaNumericError(implicit cd: ComponentDescriptor) =
    i"Password must contain at least one alphabetic character and one numeric character."

  /** Evaluates if the a number of failed attempts exceeds a given limit with a given duration.
    */
  def attempts(
    attemptHistory: Iterable[LoginRecord]
  )(lookBack: Int, from: Date, interval: Duration, attempts: Int)(implicit cd: ComponentDescriptor): Error \/ Int =
    val attemptsLeft = remainingAttempts(attemptHistory.take(lookBack), from, interval, attempts)
    if attemptsLeft >= 0 then attemptsLeft.right
    else lockoutErrorMessage(interval).left

  def lockoutErrorMessage(interval: Duration)(implicit cd: ComponentDescriptor) =
    val intervalf = DateUtils.formatDuration(interval.toMillis)
    i"Too many failed login attempts. Please wait $intervalf."

  private def remainingAttempts(
    attemptHistory: Iterable[LoginRecord],
    from: Date,
    interval: Duration,
    attempts: Int
  ): Int =
    attempts - failedLoginAttempts(attemptHistory, from, interval)

  private def failedLoginAttempts(attemptHistory: Iterable[LoginRecord], from: Date, interval: Duration): Int =
    attemptHistory.count(login => isRecentFailure(login, from, interval))

  private def isRecentFailure(login: LoginRecord, from: Date, interval: Duration) =
    (login.status == LoginStatus.InvalidCredentials) && diff(login.timestamp, from) < interval

  def diff(startDate: Date, endDate: Date) =
    (endDate.getTime - startDate.getTime) milliseconds
end PasswordPolicyUtils
