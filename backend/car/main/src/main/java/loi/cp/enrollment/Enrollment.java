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

package loi.cp.enrollment;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import loi.cp.context.ContextComponent;
import loi.cp.context.ContextRootComponent;
import loi.cp.role.RoleComponent;
import loi.cp.user.UserComponent;
import scala.Option;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Date;

@Component
public class Enrollment extends AbstractComponent implements EnrollmentComponent {

    @Instance
    private EnrollmentFacade _instance;

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    @PostCreate
    private void init(EnrollmentComponent init) {
        _instance.setGroupId(init.getContextId());
        _instance.setRoleId(init.getRoleId());
        setStartTime(init.getStartTime());
        setStopTime(init.getStopTime());
    }

    @Override
    public Long getId() {
        return _instance.getId();
    }

    @Override
    public Long getUserId() {
        return _instance.getParentId();
    }

    @Override
    public UserComponent getUser() {
        return ComponentSupport.get(getUserId(), UserComponent.class);
    }

    @Override
    public Long getContextId() {
        return _instance.getGroupId();
    }

    @Override
    public ContextComponent getContext() {
        return ComponentSupport.lookupService(ContextRootComponent.class).getContext(getContextId()).orElse(null);
    }

    @Override
    public Long getRoleId() {
        return _instance.getRoleId();
    }

    @Override
    public void setRoleId(Long role) {
        // TODO: check it's a valid role for this context
        _instance.setRoleId(role);
    }

    @Override
    public RoleComponent getRole() {
        return ComponentSupport.get(getRoleId(), RoleComponent.class);
    }

    @Override
    public String getRoleName() {
        return getRole().getName();
    }

    @Override
    public Date getStartTime() {
        return DataSupport.minimalToNull(_instance.getStartTime());
    }

    @Override
    public void setStartTime(Date startTime) {
        _instance.setStartTime(DataSupport.defaultToMinimal(startTime));
    }

    @Override
    public Date getStopTime() {
        return DataSupport.maximalToNull(_instance.getStopTime());
    }

    @Override
    public void setStopTime(Date stopTime) {
        _instance.setStopTime(DataSupport.defaultToMaximal(stopTime));
    }

    @Override
    public boolean isDisabled() {
        return Boolean.TRUE.equals(_instance.getDisabled());
    }

    @Override
    public Instant getCreatedOn() {
        return _instance.getCreatedOn();
    }

    @Override
    public scala.Option<String> getDataSource() {
        return Option.apply(_instance.getDataSource());
    }

    @Override
    public void setDataSource(scala.Option<String> dataSource) {
        _instance.setDataSource(dataSource.getOrElse(() -> null));
    }

    @Override
    public void delete() {
        _enrollmentWebService.invalidateEnrollment(_instance);
        _instance.delete();
    }
}
