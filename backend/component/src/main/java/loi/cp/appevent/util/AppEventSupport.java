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

package loi.cp.appevent.util;

import java.util.Date;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentSupport;
import loi.cp.appevent.AppEvent;
import loi.cp.appevent.AppEventService;

public class AppEventSupport {
    private AppEventSupport() {
    }

    public static AppEventService lookupService() {
        return ComponentSupport.lookupService(AppEventService.class);
    }

    /**
     * Fires an asynchronous event.
     *
     * @param source
     * @param event
     * @param rels Related Ids. Although it looks unlimited, a maximum of two is supported.
     */
    public static void fireAppEvent(Id source, AppEvent event, Id... rels) {
        lookupService().fireEvent(source, event, rels);
    }

    public static void scheduleAppEvent(Date when, Id source, AppEvent event, Id... rels) {
        lookupService().scheduleEvent(when, source, source, event, rels);
    }

    public static void scheduleAppEvent(Date when, Id source, Id target, AppEvent event, Id... rels) {
        lookupService().scheduleEvent(when, source, target, event, rels);
    }
}
