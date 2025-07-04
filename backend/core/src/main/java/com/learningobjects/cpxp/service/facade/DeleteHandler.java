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

/**
 * Handler for a facade method that destroys or deletes an item.
 */
class DeleteHandler implements FacadeMethodHandler {
    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) {

        boolean destroy = args != null && args.length >= 1 && (args[0] instanceof Boolean) && ((Boolean) args[0]);

        if (!handler.isDummy()) {
            if (destroy) {
                handler.getContext().getItemService().destroy(handler.getItem());
            } else {
                handler.getContext().getItemService().delete(handler.getItem());
            }
        }
        return null;
    }

    @Override
    public String getMethodName() {
        return "delete";
    }
}
