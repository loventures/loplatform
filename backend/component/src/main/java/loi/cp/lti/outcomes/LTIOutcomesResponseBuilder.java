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

package loi.cp.lti.outcomes;

import org.imsglobal.lti.DeleteResultResponse;
import org.imsglobal.lti.ImsxCodeMajorType;
import org.imsglobal.lti.ImsxGWSVersionValueType;
import org.imsglobal.lti.ImsxPOXBodyType;
import org.imsglobal.lti.ImsxPOXEnvelopeType;
import org.imsglobal.lti.ImsxPOXHeaderType;
import org.imsglobal.lti.ImsxResponseHeaderInfoType;
import org.imsglobal.lti.ImsxSeverityType;
import org.imsglobal.lti.ImsxStatusInfoType;
import org.imsglobal.lti.ObjectFactory;
import org.imsglobal.lti.ReadResultResponse;
import org.imsglobal.lti.ReplaceResultResponse;
import org.imsglobal.lti.ResultType;
import org.imsglobal.lti.TextType;

import javax.xml.bind.JAXBElement;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * LTIOutcomesResponseBuilder is a utility for building LTI outcomes response messages.
 */
public class LTIOutcomesResponseBuilder {
    private static final ObjectFactory objectFactory = new ObjectFactory();
    private static final String DEFAULT_LANGUAGE = "en";

    private String _requestId;

    public LTIOutcomesResponseBuilder(String requestId) {
        _requestId = requestId;
    }

    public JAXBElement<ImsxPOXEnvelopeType> buildReadResultSuccessEnvelope(String grade) {
        ImsxPOXHeaderType header = buildSuccessHeader();
        ImsxPOXBodyType body = buildReadResultBody(grade);
        return buildEnvelope(header, body);
    }

    public JAXBElement<ImsxPOXEnvelopeType> buildReplaceResultSuccessEnvelope() {
        ImsxPOXHeaderType header = buildSuccessHeader();
        ImsxPOXBodyType body = buildReplaceResultBody();
        return buildEnvelope(header, body);
    }

    public JAXBElement<ImsxPOXEnvelopeType> buildDeleteResultSuccessEnvelope() {
        ImsxPOXHeaderType header = buildSuccessHeader();
        ImsxPOXBodyType body = buildDeleteResultBody();
        return buildEnvelope(header, body);
    }

    public JAXBElement<ImsxPOXEnvelopeType> buildFailureEnvelope(String description, Consumer<ImsxPOXBodyType> sideEffect) {
        return buildFailureEnvelope(description, ImsxCodeMajorType.FAILURE, sideEffect);
    }

    public JAXBElement<ImsxPOXEnvelopeType> buildFailureEnvelope(String description, ImsxCodeMajorType codeMajor, Consumer<ImsxPOXBodyType> sideEffect) {
        ImsxPOXHeaderType header = buildHeader(codeMajor, description);
        ImsxPOXBodyType body = new ImsxPOXBodyType();
        sideEffect.accept(body);
        return buildEnvelope(header, body);
    }

    private JAXBElement<ImsxPOXEnvelopeType> buildEnvelope(ImsxPOXHeaderType header, ImsxPOXBodyType body) {
        ImsxPOXEnvelopeType envelope = new ImsxPOXEnvelopeType();
        envelope.setImsxPOXHeader(header);
        envelope.setImsxPOXBody(body);
        return objectFactory.createImsxPOXEnvelopeResponse(envelope);
    }

    private ImsxPOXHeaderType buildSuccessHeader() {
        return buildHeader(ImsxCodeMajorType.SUCCESS);
    }

    private ImsxPOXHeaderType buildFailureHeader(String description) {
        return buildHeader(ImsxCodeMajorType.FAILURE, description);
    }

    private ImsxPOXHeaderType buildHeader(ImsxCodeMajorType codeMajor) {
        return buildHeader(codeMajor, null);
    }

    private ImsxPOXHeaderType buildHeader(ImsxCodeMajorType codeMajor, String description) {
        ImsxStatusInfoType statusInfo = new ImsxStatusInfoType();
        statusInfo.setImsxCodeMajor(codeMajor);
        statusInfo.setImsxMessageRefIdentifier(_requestId);
        statusInfo.setImsxSeverity(ImsxSeverityType.STATUS);
        statusInfo.setImsxDescription(description);

        ImsxResponseHeaderInfoType responseHeaderInfo = new ImsxResponseHeaderInfoType();
        responseHeaderInfo.setImsxMessageIdentifier(UUID.randomUUID().toString());
        responseHeaderInfo.setImsxStatusInfo(statusInfo);
        responseHeaderInfo.setImsxVersion(ImsxGWSVersionValueType.V_1_0);

        ImsxPOXHeaderType header = new ImsxPOXHeaderType();
        header.setImsxPOXResponseHeaderInfo(responseHeaderInfo);
        return header;
    }

    private ImsxPOXBodyType buildReadResultBody(String grade) {
        TextType resultScore = new TextType();
        resultScore.setLanguage(DEFAULT_LANGUAGE);
        resultScore.setTextString(grade);

        ResultType result = new ResultType();
        result.setResultScore(resultScore);

        ReadResultResponse readResultResponse = new ReadResultResponse();
        readResultResponse.setResult(result);

        ImsxPOXBodyType body = new ImsxPOXBodyType();
        body.setReadResultResponse(readResultResponse);
        return body;
    }

    private ImsxPOXBodyType buildReplaceResultBody() {
        ReplaceResultResponse replaceResultResponse = new ReplaceResultResponse();

        ImsxPOXBodyType body = new ImsxPOXBodyType();
        body.setReplaceResultResponse(replaceResultResponse);
        return body;
    }

    private ImsxPOXBodyType buildDeleteResultBody() {
        DeleteResultResponse deleteResultResponse = new DeleteResultResponse();

        ImsxPOXBodyType body = new ImsxPOXBodyType();
        body.setDeleteResultResponse(deleteResultResponse);
        return body;
    }
}
