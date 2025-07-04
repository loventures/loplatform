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

import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.item.Item;
import scala.Function1;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Handler for a facade method that adds a child.
 */
class ChildAddHandler implements FacadeMethodHandler, UserDefinedMethodHandler {
    private final Method _method;
    private final Class<?> _declaringClass;
    private final String _itemType;
    private final Class<? extends Facade> _facadeClass;
    private final GeneralAddChildSupport _addChildSupporter;
    private final boolean _isUpload, _isF1, _isConsumer;

    public ChildAddHandler(Method method, FacadeChild facadeChild, Class<? extends Facade> facade) {
        _method = method;
        _declaringClass = method.getDeclaringClass();
        _itemType = FacadeUtils.getChildType(facadeChild, facade);
        Class<?> resultType = method.getReturnType();
        _facadeClass = (Class<? extends Facade>) resultType;
        _addChildSupporter = new GeneralAddChildSupport(facadeChild, getMethod(), _itemType);
        Class<?> paramType = (method.getParameterTypes().length == 1) ? method.getParameterTypes()[0] : null;
        _isUpload = (paramType != null) && UploadInfo.class.equals(paramType);
        _isF1 = (paramType != null) && Function1.class.isAssignableFrom(paramType);
        _isConsumer = (paramType != null) && Consumer.class.isAssignableFrom(paramType);
    }

    @Override
    public String getMethodName() {
        return _method.getName();
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) {
        Item item;
        if (_isUpload) {
            UploadInfo upload = (UploadInfo) args[0];
            Long id = null;
            if ((upload != null) && (upload != UploadInfo.REMOVE)) {
                id = handler.getContext().getService(AttachmentWebService.class).createAttachment(handler.getId(), upload);
            }
            item = handler.getContext().getItemService().get(id);
        } else {
            final Consumer init;
            if (_isF1) {
                init = ((Function1) args[0])::apply;
            } else {
                init = _isConsumer ? (Consumer) args[0] : a -> {};
            }
            item = _addChildSupporter.addChild(handler, (newItem, newFacade) -> init.accept(newFacade), _facadeClass);
        }
        if (Void.TYPE.equals(_facadeClass)) {
            return null;
        }
        return FacadeFactory.getFacade(_facadeClass, item, handler.getContext());
    }

    @Override
    public String toString() {
        return String.format("[itemType: %1$s, declaringClass: %2$s, method: %3$s]", _itemType, _declaringClass, _method.getName());
    }
}
