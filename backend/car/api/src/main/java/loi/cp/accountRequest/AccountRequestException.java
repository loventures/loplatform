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

package loi.cp.accountRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import loi.cp.web.HttpAcceptedException;

import java.util.Collections;
import java.util.List;

public class AccountRequestException extends HttpAcceptedException {
    private Reason _reason;
    private List<String> _messages;

    public AccountRequestException(Reason reason) {
        this(reason, Collections.<String>emptyList());
    }

    @JsonCreator
    public AccountRequestException(@JsonProperty("reason") Reason reason,
                                    @JsonProperty("messages") List<String> messages) {
        super("Account request error: " + reason);
        _reason = reason;
        _messages = messages;
    }

    @JsonProperty
    public Reason getReason() {
        return _reason;
    }

    @JsonProperty
    public List<String> getMessages() {
        return _messages;
    }

    public static enum Reason {
        DuplicateUser,
        InvalidPassword;
    }
}
