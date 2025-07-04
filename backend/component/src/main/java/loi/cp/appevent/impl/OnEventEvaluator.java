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

package loi.cp.appevent.impl;

import com.learningobjects.cpxp.component.eval.AbstractEvaluator;
import loi.cp.appevent.EventRel;
import loi.cp.appevent.EventSrc;
import loi.cp.appevent.facade.AppEventFacade;

import java.util.Date;

/** This is just a stub to co-opt the evaluator machinery. */
class OnEventEvaluator extends AbstractEvaluator {
    public Object eventParameter(AppEventFacade facade) throws Exception {
        if (_subtype == Date.class) {
            return facade.getFired();
        }

        Long id = null;
        if (getAnnotation(EventSrc.class) != null) {
            id = facade.getParentId();
        } else {
            EventRel rel = getAnnotation(EventRel.class);
            if (rel == null) {
                throw new Exception("Unknown event parameter");
            }
            if (rel.value() == 0) {
                id = facade.getRel0();
            } else if (rel.value() == 1) {
                id = facade.getRel1();
            } else {
                throw new Exception("Unknown event relation");
            }
        }
        return getItem(id, null);
    }

    @Override
    public boolean isStateless() {
        /* does not matter */
        return false;
    }
}
