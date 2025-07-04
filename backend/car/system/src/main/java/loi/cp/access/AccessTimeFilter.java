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

package loi.cp.access;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentFilter;
import com.learningobjects.cpxp.component.web.FilterBinding;
import com.learningobjects.cpxp.component.web.FilterInvocation;
import com.learningobjects.cpxp.schedule.Scheduled;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.service.user.UserHistoryFacade;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.ManagedUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@FilterBinding(priority = 100)
@SuppressWarnings("unused") // component
public class AccessTimeFilter extends AbstractComponentFilter {
    private static final Map<Long, Date> __accessTimes = new HashMap<>();

    @Inject
    private FacadeService _facadeService;

    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response, FilterInvocation invocation) {
        UserDTO currentUser = Current.getUserDTO();
        DomainDTO domain = Current.getDomainDTO();

        if ((domain != null) && (currentUser != null) && !UserType.Anonymous.equals(currentUser.getUserType())) {
            synchronized (__accessTimes) {
                __accessTimes.put(currentUser.getId(), Current.getTime());
            }
        }

        return true;
    }

    @SuppressWarnings("unused") // scheduled
    @Scheduled("1 minute")
    public void persistAccessTimes() {
        Map<Long, Date> timesToRecord = new HashMap<>();

        synchronized (__accessTimes) {
            timesToRecord.putAll(__accessTimes);
            __accessTimes.clear();
        }

        // to keep things un-stale, always read straight from the database
        ManagedUtils.getEntityContext().setCacheModePutOnly();

        // run this as a series of separate transactions to minimize locking
        for (Map.Entry<Long,Date> entry : timesToRecord.entrySet()) {
            UserFacade user = _facadeService.getFacade(entry.getKey(), UserFacade.class);
            if (user == null) {
                continue;
            }
            UserHistoryFacade history = user.getOrCreateHistory();
            Date accessTime = history.getAccessTime();
            if ((accessTime == null) || accessTime.before(entry.getValue())) {
                history.setAccessTime(entry.getValue());
            }
            EntityContext.flushClearAndCommit();
        }
    }
}
