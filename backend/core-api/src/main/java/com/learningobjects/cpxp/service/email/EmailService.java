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

package com.learningobjects.cpxp.service.email;

import com.learningobjects.cpxp.service.user.UserFacade;
import scala.Function1;

import javax.annotation.Nullable;
import javax.ejb.Local;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Local
public interface EmailService {
    public void sendTextEmail(UserFacade to, String subject, String body, boolean html) throws Exception;
    public void sendMultipartEmail(String to, InternetAddress from, InternetAddress[] replyTo, String subject, Multipart content) throws Exception;
    public boolean validateEmailAddress(String emailAddress);

    public <T> void sendEmail(Function1<MimeMessage, T> compose) throws Exception;
    public <T> void sendEmail(String messageId, Function1<MimeMessage, T> compose) throws Exception;

    /**
     * Sends a single email, either HTML or plain text, to the provided email address.
     *
     * @param emailAddress The email address of the intended recipient
     * @param subject The subject of the email
     * @param body The body of the email
     * @param html Flag indicating if the email is to be rendered as HTML, true for HTML, false for plain text
     */
    void sendTextEmail(String emailAddress, String subject, String body, boolean html) throws Exception;

    void sendTextEmail(@Nullable String senderEmail, @Nullable String senderName, String recipientEmail, String recipientName, String subject, String body, boolean html) throws Exception;

    String X_SENT_BY = "X-SentBy";
    String DIFFERENCE_ENGINE = "Difference Engine";
}
