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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import loi.cp.web.HttpAcceptedException;

public class ChallengeException extends HttpAcceptedException {
    private final String _challenge;

    @JsonCreator
    public ChallengeException(@JsonProperty("challenge") String challenge) {
        super("Challenge", STATUS_CHALLENGE);
        _challenge = challenge;
    }

    @JsonProperty
    public String getChallenge() {
        return _challenge;
    }
}
