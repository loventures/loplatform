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

package loi.cp.lti;

import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.filter.CurrentFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.*;
import loi.cp.bus.*;
import loi.cp.integration.LtiSystemComponent;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.imsglobal.lti.*;
import scala.Some;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LtiResultMessageSenderImpl implements LtiResultMessageSender {
    private static final Logger logger = Logger.getLogger(LtiResultMessageSender.class.getName());

    @Override public DeliveryResult sendLtiScore(LtiSystemComponent system, String defaultOutcomeServiceUrl, String resultSourceDid, Double ltiScore, YieldTx untx) throws Exception {
        ObjectFactory factory = new ObjectFactory();

        ImsxPOXEnvelopeType envelope = factory.createImsxPOXEnvelopeType();

        ImsxPOXHeaderType header = factory.createImsxPOXHeaderType();
        ImsxRequestHeaderInfoType headerInfo = factory.createImsxRequestHeaderInfoType();
        headerInfo.setImsxVersion(ImsxGWSVersionValueType.V_1_0);
        headerInfo.setImsxMessageIdentifier(GuidUtil.longGuid());
        headerInfo.setImsxSendingAgentIdentifier("D-E");
        header.setImsxPOXRequestHeaderInfo(headerInfo);
        envelope.setImsxPOXHeader(header);
        ImsxPOXBodyType body = factory.createImsxPOXBodyType();

        ResultRecordType result = factory.createResultRecordType();
        SourcedGUIDType guid = factory.createSourcedGUIDType();
        guid.setSourcedId(resultSourceDid);
        result.setSourcedGUID(guid);
        if (ltiScore == null) {
            DeleteResultRequest delete = factory.createDeleteResultRequest();
            delete.setResultRecord(result);
            body.setDeleteResultRequest(delete);
        } else {
            double clamped;
            if (Double.isNaN(ltiScore)) {
                logger.warning(String.format("Lti Score for grade %s is NaN, sending as 0", ltiScore));
                clamped = 0d;
            } else {
                clamped = Math.max(0., Math.min(1., ltiScore));
            }

            ReplaceResultRequest replace = factory.createReplaceResultRequest();
            ResultType resultType = factory.createResultType();
            TextType textType = factory.createTextType();
            textType.setLanguage("en");
            textType.setTextString(String.valueOf(clamped));
            resultType.setResultScore(textType);
            result.setResult(resultType);
            replace.setResultRecord(result);
            body.setReplaceResultRequest(replace);
        }

        envelope.setImsxPOXBody(body);

        JAXBElement<ImsxPOXEnvelopeType> document =
            factory.createImsxPOXEnvelopeRequest(envelope);

        JAXBContext jaxbContext =
            JAXBContext.newInstance(factory.getClass().getPackage().getName());
        Marshaller m = jaxbContext.createMarshaller();
        // m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        m.marshal(document, bytes);
        String xml = bytes.toString("UTF-8");

        String systemOutcomeServiceUrl = StringUtils.defaultIfEmpty(defaultOutcomeServiceUrl, system.getOutcomeServiceUrl());

        HttpPost post = new HttpPost();

        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build();
        post.setConfig(requestConfig);
        post.setEntity(new StringEntity(xml, ContentType.create(MimeUtils.MIME_TYPE_APPLICATION_XML, "UTF-8")));

        final String outcomeServiceUrl;

        String xDomain = URI.create(systemOutcomeServiceUrl).getUserInfo();
        if (xDomain != null && xDomain.equals(Current.getDomainDTO().domainId())) {
            outcomeServiceUrl = systemOutcomeServiceUrl.replace(xDomain + "@", "");
            post.setHeader(CurrentFilter.HTTP_HEADER_X_DOMAIN_ID, xDomain);
        } else {
            outcomeServiceUrl = systemOutcomeServiceUrl;
        }

        post.setURI(new URI(outcomeServiceUrl));

        logger.log(Level.INFO, "LTI outcome request: {0} - {1}", new Object[] { outcomeServiceUrl, xml });

        // TODO: FIXME: Should take bytes
        String auth = OAuthUtils.getOAuthHeaderString(
          outcomeServiceUrl, system.getSystemId(), system.getKey(), xml, HttpUtils.HTTP_REQUEST_METHOD_POST
        );
        post.setHeader(new BasicHeader("Authorization", auth));

        try {

            return untx.apply(() -> {
                try {
                    HttpResponse response = HttpUtils.getHttpClient().execute(post);

                    int sc = response.getStatusLine().getStatusCode();

                    String xmlResponse = EntityUtils.toString(response.getEntity(), "UTF-8");

                    logger.log(Level.INFO, "LTI outcome response: {0} - {1}", new Object[]{sc, xmlResponse});

                    // TODO: Parse and check for errors
                    if (sc != 200) {
                        return TransientFailure.apply(FailureInformation.apply(
                          Request.apply(outcomeServiceUrl, xml, "POST"),
                          Response.apply(Some.apply(xmlResponse), response.getFirstHeader("Content-Type").getValue(), sc)
                        ));
                    } else {
                        return Delivered$.MODULE$;
                    }
                } catch (IOException e) {
                    return TransientFailure.apply(FailureInformation.apply(
                      Request.apply(outcomeServiceUrl, xml, "POST"),
                      e
                    ));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            return TransientFailure.apply(ex);
        }
    }
}

