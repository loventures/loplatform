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

package loi.cp.integration;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.integration.IntegrationFacade;
import com.learningobjects.cpxp.service.integration.SystemFacade;

import javax.inject.Inject;

@Component
public class Integration extends AbstractComponent implements IntegrationComponent {

    @Inject
    private FacadeService _facadeService;

    @Instance
    private IntegrationFacade _self;

    @PostCreate
    private void init(IntegrationComponent init) {
        if (init.getSystemId() != null) {
            SystemFacade system =
              _facadeService.getFacade(init.getSystemId(), SystemFacade.class);
            if ((system == null) || Boolean.TRUE.equals(system.getDisabled())) {
                throw new ValidationException("connector_id",String.valueOf(init.getSystemId()), "Invalid connector");
            }
        }
        _self.setUniqueId(init.getUniqueId());
        _self.setExternalSystem(init.getSystemId());
    }

    @Override
    public Long getId() {
        return _self.getId();
    }

    @Override
    public String getUniqueId() {
        return _self.getUniqueId();
    }

    @Override
    public void setUniqueId(String uniqueId) {
        _self.setUniqueId(uniqueId);
    }

    @Override
    public Long getSystemId() {
        return _self.getExternalSystem();
    }

    @Override
    public SystemComponent getSystem() {
        return ComponentSupport.get(getSystemId(), SystemComponent.class);
    }

    @Override
    public void delete() {
        _self.delete();
    }
}
