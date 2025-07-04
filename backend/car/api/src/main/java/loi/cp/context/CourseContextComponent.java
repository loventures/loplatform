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

package loi.cp.context;

import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.registry.Bound;
import com.learningobjects.cpxp.component.registry.Decorates;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.de.web.Queryable;
import loi.cp.integration.IntegrationRootOwner;
import loi.cp.user.ContextProfilesApi;

import javax.validation.groups.Default;
import java.util.List;

// TODO: ALL these need to be @Secured by you having a valid role in said course.

@Controller(value="course")
@Schema("course")
@Bound(Decorates.class)
public interface CourseContextComponent extends ContextComponent, IntegrationRootOwner {

    @JsonView(Default.class)
    @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_ID)
    public String getCourseId();

    @JsonView(Default.class)
    @Queryable(dataType = DataTypes.DATA_TYPE_NAME, traits = Queryable.Trait.CASE_INSENSITIVE)
    public String getName();

    @JsonView(Default.class)
    @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_BRANCH)
    public Long getBranch();

    @JsonView(Default.class)
    @Queryable(dataType = DataTypes.DATA_TYPE_URL)
    public String getUrl();

    // Generic listing of user profiles in a course
    @RequestMapping(path = "users", method = Method.Any)
//    @Secured(ProfilesCourseRight.class)
    public ContextProfilesApi getUsers();

    @RequestMapping(path = "lmsIntegrated", method = Method.GET)
    public Boolean isLmsIntegrated();

    // Fetch the current user's rights for the course.
    @RequestMapping(path = "rights", method = Method.GET)
    public List<String> getRights();

}
