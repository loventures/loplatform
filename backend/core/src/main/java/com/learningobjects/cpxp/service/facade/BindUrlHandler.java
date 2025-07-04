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

package com.learningobjects.cpxp.service.facade;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.name.NameService;

/**
 * Handler for a facade method that binds an url.
 */
class BindUrlHandler implements FacadeMethodHandler {
    @Override
    public String getMethodName() {
        return "bindUrl";
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) {
        NameService nameService = handler.getContext().getService(NameService.class);
        String path = nameService.getPath(handler.getItem().getParent());
        String pattern;
        if (AttachmentConstants.ITEM_TYPE_ATTACHMENT.equals(handler.getItem().getType())) {
            pattern = BasicServiceBean.getFilenameBindingPattern(path, (String) args[0], handler.getItem().getType());
        } else {
            pattern = BasicServiceBean.getBindingPattern(path, (String) args[0], handler.getItem().getType());
        }
        return nameService.setBindingPattern(handler.getItem(), pattern);
    }

    @Override
    public String toString() {
        return "BindUrl";
    }
}
