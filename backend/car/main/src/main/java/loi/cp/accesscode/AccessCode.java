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

package loi.cp.accesscode;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.exception.AccessForbiddenException;
import com.learningobjects.cpxp.service.facade.FacadeService;

import javax.inject.Inject;
import java.util.List;

@Component
public class AccessCode extends AbstractComponent implements AccessCodeComponent {

    @Inject
    private FacadeService _facadeService;

    @Instance
    private AccessCodeFacade _instance;

    @PostCreate
    private void init(Init init) {
        _instance.setRedemptionCount(0L);
        _instance.setAccessCode(init.accessCode);
        _instance.setBatch(init.batch);
    }

    @Override
    public Long getId() {
        return _instance.getId();
    }

    @Override
    public String getAccessCode() {
        return _instance.getAccessCode();
    }

    @Override
    public Long getRedemptionCount() {
        return _instance.getRedemptionCount();
    }

    @Override
    public Long getBatchId() {
        return _instance.getBatchId();
    }

    @Override
    public AccessCodeBatchComponent getBatch() {
        return _instance.getBatch();
    }

    @Override
    public AccessCodeState validate() {
        Long limit = getBatch().getRedemptionLimit();
        if (isRedeemedByCurrentUser()) {
            return AccessCodeState.Redeemed;
        } else if ((limit >= 0) && getRedemptionCount() >= getBatch().getRedemptionLimit()) {
            return AccessCodeState.Invalid;
        }
        return AccessCodeState.Valid;
    }

    @Override
    public RedemptionSuccess redeem() throws AccessForbiddenException {
        if (Current.isAnonymous()) {
            throw new AccessForbiddenException("Not logged in");
        }
        _instance.refresh(true); // Pessimistic lock
        AccessCodeBatchComponent batch = getBatch();
        if (isRedeemedByCurrentUser()) {
            throw new AccessForbiddenException("Already redeemed");
        } else if ((batch == null) || Boolean.TRUE.equals(batch.getDisabled())) {
            throw new AccessForbiddenException("Disabled access code");
        }
        Long limit = getBatch().getRedemptionLimit();
        if ((limit >= 0) && getRedemptionCount() >= limit) {
            throw new AccessForbiddenException("Already redeemed by another user");
        }
        _instance.setRedemptionCount(1L + getRedemptionCount());
        _instance.invalidateParent();
        RedemptionComponent redemption = getRedemptionParent().addRedemption(this);
        return batch.redeemAccessCode(this, redemption);
    }

    @Override
    public void delete() {
        _instance.delete();
    }

    @Override
    public List<RedemptionComponent> getRedemptions() {
        return getRedemptionRoot().findRedemptionsByAccessCode(this);
    }

    private boolean isRedeemedByCurrentUser() {
        return getRedemptionParent().findRedemptionByAccessCode(this).isPresent();
    }

    private RedemptionParentFacade getRedemptionParent() {
        return _facadeService.getFacade(Current.getUser(), RedemptionParentFacade.class);
    }

    private RedemptionRootFacade getRedemptionRoot() {
        return _facadeService.getFacade(Current.getDomain(), RedemptionRootFacade.class);
    }
}
