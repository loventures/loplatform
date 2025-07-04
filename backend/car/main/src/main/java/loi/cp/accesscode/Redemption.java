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
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.Ids;
import loi.cp.user.UserComponent;

import java.util.Date;

@Component
public class Redemption extends AbstractComponent implements RedemptionComponent {
    @Instance
    private RedemptionFacade _instance;

    @PostCreate
    private void init(AccessCodeComponent code) {
        _instance.setAccessCode(code);
        _instance.setDate(Current.getTime());
    }

    @Override
    public Long getId(){
        return _instance.getId();
    }

    @Override
    public UserComponent getUser() {
        return ComponentSupport.get(Ids.get(_instance.getUser()), UserComponent.class);
    }

    @Override
    public Date getDate() {
        return _instance.getDate();
    }

    @Override
    public AccessCodeComponent getAccessCode() {
        return _instance.getAccessCode();
    }
}
