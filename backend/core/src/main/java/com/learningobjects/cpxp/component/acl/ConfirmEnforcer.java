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

package com.learningobjects.cpxp.component.acl;

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.UserException;
import com.learningobjects.cpxp.component.annotation.EnforceConfirm;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.Parameter;
import com.learningobjects.cpxp.component.util.ComponentUtils;

public class ConfirmEnforcer implements AccessEnforcer {
    @Infer
    private EnforceConfirm _confirm;

    @Infer
    private ComponentDescriptor _scope;

    @Parameter(name = "ug:confirmed")
    private Boolean _confirmed;

    @Override
    public boolean checkAccess() {
        if (!Boolean.TRUE.equals(_confirmed)) {
            throw new UserException(UserException.STATUS_CONFIRM, i18n(_confirm.title()), i18n(_confirm.message()));
        }
        return true;
    }

    private String i18n(String msg) {
        return ComponentUtils.expandMessage(msg, _scope);
    }
}
