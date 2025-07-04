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

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.util.FormattingUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.ObjectUtils;
import scala.Function1;

import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EmailServiceBean extends BasicServiceBean implements EmailService {
    private static final Logger logger = Logger.getLogger(EmailServiceBean.class.getName());

    /** A crude email address regex. Requires no spaces, an @ and a .
     *  taken from ValidationController
     */
    public static final Pattern EMAIL_ADDRESS_RE = Pattern.compile("(\\S+@\\S+\\.\\S+)?");
    private static final String NO_REPLY = "noreply@";

    @Inject
    private Session _mailSession;

    @Inject
    private FacadeService _facadeService;

    // TODO: Background thread all emails

    @Override
    public void sendTextEmail(final UserFacade user, final String subject, final String body, final boolean html) throws Exception {
        final String userStr = FormattingUtils.userStr(user);
        sendTextEmail(user.getEmailAddress(), userStr, subject, body, html);
    }

    @Override
    public void sendMultipartEmail(String to, InternetAddress from, InternetAddress[] replyTo, String subject, Multipart content) throws Exception {
        MimeMessage message = createEmail(null);
        message.setFrom(from);
        message.addRecipients(Message.RecipientType.TO, to);
        if (null != replyTo && replyTo.length > 0) {
            message.setReplyTo(replyTo);
        }
        message.setSubject(subject);
        message.setContent(content);
        message.setSentDate(new Date());
        sendEmail(message);
    }

    @Override
    public boolean validateEmailAddress(String emailAddress) {
        return EMAIL_ADDRESS_RE.matcher(emailAddress).matches() || emailAddress.endsWith(LocalmailService.AT_LOCALMAIL);
    }

    /**
     * Helper method for sending text emails
     *
     * @param emailAddress The email address of the recepient
     * @param personalName The name of the recipient
     * @param subject The subject of the email
     * @param body The body of the email
     * @param html Flag indicating if the email contains HTML, true if HTML, false if plain text
     * @throws Exception
     *
     * @deprecated backwards compatibility for when we didn't need to set the sender
     */
    @Deprecated
    private void sendTextEmail(final String emailAddress, final String personalName, final String subject, final String body, final boolean html) throws Exception {

        sendTextEmail(null, null, emailAddress, personalName, subject, body, html);

    }

    @Override
    public void sendTextEmail(String emailAddress, String subject, String body, boolean html) throws Exception {
        sendTextEmail(emailAddress, StringUtils.EMPTY, subject, body, html);
    }

    @Override
    public void sendTextEmail(@Nullable String senderEmail, @Nullable String senderName, String recipientEmail, String recipientName, String subject, String body, boolean html) throws Exception {

        if (StringUtils.isEmpty(recipientEmail)) {
            throw new InvalidRequestException("Must specify email address");
        }

        String fSenderEmail = (senderEmail != null) ? senderEmail : NO_REPLY + Current.getDomainDTO().getHostName();
        String fSenderName = (senderName != null) ? senderName : Current.getDomainDTO().getName();
        InternetAddress sender = new InternetAddress(fSenderEmail, fSenderName);

        // recipientEmail is required
        String fRecipientName = (recipientName != null) ? recipientName : StringUtils.EMPTY;
        InternetAddress recipient = new InternetAddress(recipientEmail, fRecipientName);

        MimeMessage message = createEmail(null);
        message.setFrom(sender);
        message.addRecipients(Message.RecipientType.TO, new InternetAddress[] { recipient });
        message.setSubject(subject, CharEncoding.UTF_8);
        String mimeType = html ? MimeUtils.MIME_TYPE_TEXT_HTML : MimeUtils.MIME_TYPE_TEXT_PLAIN;
        message.setContent(body, mimeType + MimeUtils.CHARSET_SUFFIX_UTF_8);

        sendEmail(message);

    }

    @Override
    public <T> void sendEmail(String messageId, Function1<MimeMessage, T> compose) throws Exception {
        MimeMessage message = createEmail(messageId);
        compose.apply(message);
        sendEmail(message);
    }

    @Override
    public <T> void sendEmail(Function1<MimeMessage, T> compose) throws Exception {
        sendEmail(null, compose);
    }

    static final String MessageID = "Message-ID";

    static final String Precedence = "Precedence";
    static final String Bulk = "bulk";

    static final String XAutoResponseSuppress = "X-Auto-Response-Suppress";
    static final String OOF = "OOF"; // out of facility


    private MimeMessage createEmail(final String messageId) {
        return (messageId == null) ? new MimeMessage(_mailSession)
          : new MimeMessage(_mailSession) {
            @Override
            protected void updateMessageID() throws MessagingException {
                setHeader(MessageID, messageId);
            }
        };
    }

    private void sendEmail(MimeMessage message) throws Exception {
        message.saveChanges();
        LocalmailService localmailService = ComponentSupport.lookupService(LocalmailService.class);
        message.setHeader(X_SENT_BY, DIFFERENCE_ENGINE);
        // Setting Return-Path to <> is another thing but our Return-Path is generated by sendgrid and thus out of our control.
        message.setHeader(Precedence, Bulk); // this is arguably questionable
        message.setHeader(XAutoResponseSuppress, OOF);

        Address[] froms = ObjectUtils.defaultIfNull(message.getFrom(), new Address[0]); // sigh
        Address[] recipients = ObjectUtils.defaultIfNull(message.getAllRecipients(), new Address[0]); // sigh
        final String extract = (localmailService != null)
          ? " / " + StringUtils.truncate255AndAppendEllipses(localmailService.htmlContent(message)) : "";
        logger.info("Sending email: " + StringUtils.join(froms) + " -> " + StringUtils.join(recipients) + ": " + message.getSubject() + extract);
        for (Address address : recipients) {
            String to = ((InternetAddress) address).getAddress();
            if (to.contains("@example")) {
                // no sending to example.org
                return;
            } else if (to.contains(LocalmailService.AT_LOCALMAIL)) {
                message.saveChanges();
                localmailService.sendLocalmail((InternetAddress) address, message);
                return;
            }
        }

        Transport transport = _mailSession.getTransport();
        try {
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }
    }
}

