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

package loi.cp.domain;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainFacade;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.relationship.RelationshipConstants;
import com.learningobjects.cpxp.service.relationship.RoleFinder;
import loi.cp.right.RightBinding;
import loi.cp.right.RightDTO;
import loi.cp.right.RightService;
import loi.cp.role.RoleComponent;
import loi.cp.role.RoleService;
import loi.cp.role.impl.RoleParentFacade;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Domain extends AbstractComponent implements DomainComponent {

    @Inject
    private FacadeService _facadeService;

    @Inject
    private RightService _rightService;

    @Inject
    private RoleService _roleService;

    @Instance
    private DomainFacade _instance;

    @Override
    public String getContextType() {
        return "domain";
    }

    @Override
    public void delete() {
        throw new InvalidRequestException("Cannot delete domain contexts");
    }

    @Override
    public Long getId() {
        return _instance.getId();
    }

    @Override
    public String getDomainId() {
        return _instance.getDomainId();
    }

    @Override
    public String getName() {
        return _instance.getName();
    }

    @Override
    public String getShortName() {
        return _instance.getShortName();
    }

    @Override
    public List<RoleComponent> getSupportedRoles() {
        return _roleService.getDomainRoles()
          .stream()
          .map(ComponentSupport.toComponent(RoleComponent.class))
          .filter(role -> !_roleService.isHostingRole(role))
          .collect(Collectors.toList());
    }

    @Override
    public ApiQueryResults<RoleComponent> getKnownRoles(ApiQuery query) {
        HashSet<String> hostingAdminRoles = _roleService.getHostingAdminRoleIds();
        QueryBuilder qb = _facadeService.getFacade(RelationshipConstants.ID_FOLDER_ROLES, RoleParentFacade.class).queryRoles();
        qb.addCondition(RoleFinder.DATA_TYPE_ROLE_ID(), Comparison.notIn, hostingAdminRoles);
        return ApiQuerySupport.query(qb, query, RoleComponent.class);
    }

    @Override
    public List<RightDTO> getRights() {
        List<RightDTO> rights = new LinkedList<>();
        _rightService.getAllRights().forEach(right -> {
            RightBinding binding = _rightService.getRightBinding(right);
            String name = ComponentUtils.i18n(binding.name(), getComponentDescriptor());
            String description = ComponentUtils.i18n(binding.description(), getComponentDescriptor());
            rights.add(new RightDTO(name, description, right.getTypeName(), null, null));
        });
        return rights;
    }

    @Override
    public TimeInfo getTime() {
        return new TimeInfo(_instance.getTimeZone(), Current.getTime());
    }
}
