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

import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;
import loi.cp.role.RoleComponent;
import loi.cp.web.challenge.ChallengeGuard;

import javax.annotation.Nullable;
import java.util.List;

@Service
@Controller(value = "accountRequests", root = true)
public interface AccountRequestRootComponent extends ApiRootComponent {

    @Secured(guard = ChallengeGuard.class, allowAnonymous = true)
    @RequestMapping(path = "accountRequests", method = Method.POST)
    void requestAccount(
      @RequestBody AccountRequestComponent request,
      @QueryParam(required = false) String redirect
    ) throws AccountRequestException;

    // Admin

    @Secured(ApproveAccountRequestRight.class)
    @RequestMapping(path = "accountRequests", method = Method.GET)
    ApiQueryResults<AccountRequestComponent> getAccountRequests(@MaxLimit(256) ApiQuery query);

    @Secured(ApproveAccountRequestRight.class)
    @RequestMapping(path = "accountRequests/{id}/accept", method = Method.POST)
    void acceptAccount(
      @PathVariable("id") Long id,
      @Nullable @QueryParam Boolean email,
      @QueryParam(required = false) Long role
    );

    @Secured(ApproveAccountRequestRight.class)
    @RequestMapping(path = "accountRequests/{id}/reject", method = Method.POST)
    void rejectAccount(
      @PathVariable("id") Long id,
      @Nullable @QueryParam Boolean email
    );

    @Secured(ApproveAccountRequestRight.class)
    @RequestMapping(path = "accountRequests/roles", method = Method.GET)
    List<RoleComponent> getAccountRequestRoles();

    // Settings

    Status getAccountRequestStatus();

    enum Status {
        /** Account requests disabled. */
        Disabled,
        /** Manual approval is required. */
        Manual,
        /** Email verification is required. */
        Verify,
        /** Account requests automatically approved. */
        Automatic
    }
}
