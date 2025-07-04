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

package com.learningobjects.cpxp.service.integration;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Direction;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.relationship.RelationshipWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.util.StringUtils;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class IntegrationWebServiceBean extends BasicServiceBean implements IntegrationWebService {
    private static final Logger logger = Logger.getLogger(IntegrationWebServiceBean.class.getName());

    @Inject
    private IntegrationService _integrationService;

    @Inject
    private RelationshipWebService _relationshipWebService;

    @Inject
    private FacadeService _facadeService;

    public void addLTIRoleMappings(Long systemId) {
        SystemFacade system = _facadeService.getFacade(systemId, SystemFacade.class);

        for (LTIRoleDescriptor role : __ltiRoles) {
            ExternalRoleFacade externalRole = system.addExternalRole();
            externalRole.setRoleId(role.roleId);
            externalRole.setRoleType(role.roleType);
            externalRole.setName(role.roleName);

            RoleFacade mappedRole = null;
            if (StringUtils.equals("Domain", role.roleType)) {
                mappedRole = findMappedRole(__domainRoleMapping, role.roleId);
            } else if (StringUtils.equals("Course", role.roleType)) {
                mappedRole = findMappedRole(__localRoleMapping, StringUtils.lowerCase(role.roleId));
            }

            if (mappedRole != null) {
                RoleMappingFacade roleMapping = system.addRoleMapping();
                roleMapping.setType(StringUtils.equals(role.roleType, "Domain") ? RoleMappingType.Domain : RoleMappingType.Group);
                roleMapping.setRoleId(role.roleId);
                roleMapping.setMappedRole(mappedRole.getId());
            }
        }
    }

    private static class LTIRoleDescriptor {
        public LTIRoleDescriptor(String roleId, String roleName, String roleType) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.roleType = roleType;
        }
        public String roleId;
        public String roleName;
        public String roleType;
    }

    private static final List<LTIRoleDescriptor> __ltiRoles = new ArrayList<LTIRoleDescriptor>() { {
        // . WebCT
        add(new LTIRoleDescriptor("SysAdmin", "System Administrator", "Domain"));
        add(new LTIRoleDescriptor("SysSupport", "System Support", "Domain"));
        add(new LTIRoleDescriptor("Creator", "Creator", "Domain"));
        add(new LTIRoleDescriptor("AccountAdmin", "Account Administrator", "Domain"));
        add(new LTIRoleDescriptor("User", "User", "Domain"));
        add(new LTIRoleDescriptor("Administrator", "Administrator", "Domain"));
        add(new LTIRoleDescriptor("None", "None", "Domain"));

        add(new LTIRoleDescriptor("Student", "Student", "Course"));
        add(new LTIRoleDescriptor("Faculty", "Faculty", "Course"));
        add(new LTIRoleDescriptor("Member", "Member", "Course"));
        add(new LTIRoleDescriptor("Learner", "Learner", "Course"));
        add(new LTIRoleDescriptor("Instructor", "Instructor", "Course"));
        add(new LTIRoleDescriptor("Mentor", "Mentor", "Course"));
        add(new LTIRoleDescriptor("Staff", "Staff", "Course"));
        add(new LTIRoleDescriptor("Alumni", "Alumni", "Course"));
        add(new LTIRoleDescriptor("ProspectiveStudent", "Prospective Student", "Course"));
        add(new LTIRoleDescriptor("Guest", "Guest", "Course"));
        add(new LTIRoleDescriptor("Other", "Other", "Course"));
        add(new LTIRoleDescriptor("Administrator", "Administrator", "Course"));
        add(new LTIRoleDescriptor("Observer", "Observer", "Course"));
        add(new LTIRoleDescriptor("None", "None", "Course"));

        // moodle
        add(new LTIRoleDescriptor("EditingTeacher", "EditingTeacher", "Course"));

        // canvas
        add(new LTIRoleDescriptor("StudentEnrollment", "StudentEnrollment", "Course"));
        add(new LTIRoleDescriptor("StudentViewEnrollment", "StudentViewEnrollment", "Course"));
        add(new LTIRoleDescriptor("TeacherEnrollment", "TeacherEnrollment", "Course"));
        add(new LTIRoleDescriptor("InstructorEnrollment", "InstructorEnrollment", "Course"));
        add(new LTIRoleDescriptor("TaEnrollment", "TaEnrollment", "Course"));
        add(new LTIRoleDescriptor("DesignerEnrollment", "DesignerEnrollment", "Course"));
        add(new LTIRoleDescriptor("ObserverEnrollment", "ObserverEnrollment", "Course"));
        add(new LTIRoleDescriptor("AccountUser", "AccountUser", "Domain"));
        add(new LTIRoleDescriptor("AccountUser", "AccountUser", "Course"));
    } };

    public SystemFacade getExternalSystem(Long id) {

        Item system = _integrationService.getSystem(id);
        SystemFacade facade = _facadeService.getFacade(system, SystemFacade.class);

        return facade;
    }

    public Long getSystemBySystemId(String systemId) {

        Item folder = _integrationService.getSystemsFolder();
        QueryBuilder qb = queryParent(folder,IntegrationConstants.ITEM_TYPE_SYSTEM);
        qb.addCondition(IntegrationConstants.DATA_TYPE_SYSTEM_ID, "eq", systemId);
        Item system = (Item) qb.getResult();

        return getId(system);
    }

    public SystemFacade getById(String systemId) {

        SystemFacade system = getExternalSystem(getSystemBySystemId(systemId));

        return system;
    }

    public SystemFacade getByKey(String key) {

        Item folder = _integrationService.getSystemsFolder();
        QueryBuilder qb = queryParent(folder,IntegrationConstants.ITEM_TYPE_SYSTEM);
        qb.addCondition(IntegrationConstants.DATA_TYPE_SYSTEM_KEY, "eq", key);
        Item system = (Item) qb.getResult();
        SystemFacade facade = _facadeService.getFacade(system, SystemFacade.class);

        return facade;
    }

    public Long getSystemByComponent(String identifier) {

        Item folder = _integrationService.getSystemsFolder();
        QueryBuilder qb = queryParent(folder,IntegrationConstants.ITEM_TYPE_SYSTEM);
        qb.addCondition(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, "eq", identifier);
        Item system = (Item) qb.getResult();

        return getId(system);
    }

    public List<SystemFacade> getExternalSystems() {

        QueryBuilder qb = queryParent(_integrationService.getSystemsFolder(),
               IntegrationConstants.ITEM_TYPE_SYSTEM);
        qb.setOrder(IntegrationConstants.DATA_TYPE_SYSTEM_NAME, Function.LOWER, Direction.ASC);
        List<SystemFacade> systems = qb.getFacadeList(SystemFacade.class);

        return systems;
    }

    public Long findByUniqueId(Long systemId, String uniqueId, String type) {

        Item item = _integrationService.findByUniqueId(systemId, uniqueId, type);
        Long id = (item == null) ? null : item.getParent().getId();

        return id;
    }

    @Override
    public IntegrationFacade addIntegration(Long itemId) {
        return _facadeService.getFacade(itemId, IntegrationParentFacade.class).addIntegration();
    }

    public List<IntegrationFacade> getIntegrationFacades(Long itemId) {

        return queryParent(_itemService.get(itemId),IntegrationConstants.ITEM_TYPE_INTEGRATION).getFacadeList(IntegrationFacade.class);
    }

    public List<String> getUniqueIds(Long itemId) {
        return queryParent(_itemService.get(itemId),IntegrationConstants.ITEM_TYPE_INTEGRATION).getProjectedResults(IntegrationConstants.DATA_TYPE_UNIQUE_ID);
    }

    // Horrible hacks to set up defaults.. These should be parameterized
    // by the connector type etc....

    private static final Map<String, String> __localRoleMapping = new HashMap<String, String>() { {
            // . WebCT
            put("saud", "auditor");
            put("sdes", "instructionalDesigner");
            put("sins", "instructor");
            put("sstu", "student");
            put("stea", "teachingAssistant");
            put("iadm", "instructor"); // grr
            // . Moodle
            put("admin", "instructor"); // grr
            put("coursecreator", "instructionalDesigner");
            put("editingteacher", "instructor");
            put("teacher", "instructor");
            put("student", "student");

            // guest
            // user
            // . Blackboard
            put("course_builder", "courseBuilder");
            put("grader", "grader");
            put("guest", "guest"); // added because guest is a bona fide course enrollment
            put("instructor", "instructor");
            // put("student", "student"); // dup
            put("teaching_assistant", "teachingAssistant");
            put("organizationbuilder", "organizationBuilder");
            put("leader", "leader");
            put("participant", "member");
            put("member", "member");
            put("organizationgrader", "grader");
            put("organizationguest", "guest"); // added because guest is a bona fide org enrollment
            put("assistant", "assistant");

            // LTI
            put("learner", "student");

            // canvas
            put("studentenrollment", "student");
            put("studentviewenrollment", "student");
            put("teacherenrollment", "instructor");
            put("instructorenrollment", "instructor");
            put("taenrollment", "teachingAssistant");
            put("designerenrollment", "instructionalDesigner");
            put("Observerenrollment", "guest");
            put("AccountUser", "administrator");
        } };

    private static final Map<String, String> __domainRoleMapping = new HashMap<String, String>() { {
            // . WebCT
            put("sins", "faculty");
            put("sdes", "faculty");
            put("sstu", "student");
            // . Moodle
            put("editingteacher", "faculty");
            put("teacher", "faculty");
            put("student", "student");
            // guest
            // user
            // . Blackboard
            put("faculty", "faculty");
            put("student", "student");
            put("staff", "staff");
            put("system_admin", "administrator");


            put("sysadmin", "administrator");
            put("accountadmin", "administrator");
            //put("user", "user");
            put("administrator", "administrator");

            // canvas
            put("accountuser", "administrator");
        } };

    private RoleFacade findMappedRole(Map<String, String> mapping, String id) {
        Long roleFolder = _relationshipWebService.getRoleFolder();

        String mapped = mapping.get(id.toLowerCase());
        return (mapped == null) ? null : _relationshipWebService.getRoleByRoleId(roleFolder, mapped);
    }
}
