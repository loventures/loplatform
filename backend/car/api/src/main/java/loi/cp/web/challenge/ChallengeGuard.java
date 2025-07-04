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

package loi.cp.web.challenge;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.exception.AccessForbiddenException;
import com.learningobjects.cpxp.util.DigestUtils;
import com.learningobjects.cpxp.util.GuidUtil;
import com.learningobjects.de.authorization.SecurityContext;
import com.learningobjects.de.authorization.SecurityGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;

import java.security.MessageDigest;

@Component
public class ChallengeGuard extends AbstractComponent implements SecurityGuard {
    private static final String ATTR_CHALLENGE = "ug:challenge";
    private static final String HEADER_RESPONSE = "X-Challenge-Response";

    public void checkAccess(final WebRequest webRequest, SecurityContext securityContext) {
        if (!Current.isAnonymous()) {
            return;
        }
        HttpServletRequest request = webRequest.getRawRequest();
        String response = request.getHeader(HEADER_RESPONSE);
        byte[] shaChallenge = (byte[]) request.getSession(true).getAttribute(ATTR_CHALLENGE);
        if (response == null) {
            String challenge = GuidUtil.guid();
            request.getSession(true).setAttribute(ATTR_CHALLENGE, DigestUtils.sha(challenge));
            throw new ChallengeException(challenge);
        } else if ((shaChallenge == null) || !MessageDigest.isEqual(Base64.decodeBase64(response), shaChallenge)) {
            throw new AccessForbiddenException("Invalid response");
        }
        request.getSession(true).removeAttribute(ATTR_CHALLENGE);
    }
}
