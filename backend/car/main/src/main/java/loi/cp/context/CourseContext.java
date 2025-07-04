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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentService;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.PredicateOperator;
import com.learningobjects.cpxp.component.registry.Decorates;
import loi.cp.course.CourseAccessService;
import loi.cp.course.CourseComponent;
import loi.cp.course.CourseFacade;
import loi.cp.integration.IntegrationComponent;
import loi.cp.integration.IntegrationRootComponent;
import loi.cp.integration.LmsSystemComponent;
import loi.cp.integration.SystemComponent;
import loi.cp.right.Right;
import loi.cp.user.ContextProfilesApi;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static scala.jdk.javaapi.CollectionConverters.asJava;

@Component
@Decorates(CourseContextComponent.class)
public class CourseContext extends AbstractComponent implements CourseContextComponent {

    @Inject
    private CourseAccessService _courseAccessService;

    @Instance
    private CourseFacade _self;

    @Inject
    private ComponentService _componentService;

    //for testing
    protected CourseFacade self() { return _self; }

    @Override
    public String getContextType() {
        return "course";
    }

    @Override
    public Long getId() {
        return self().getId();
    }

    @Override
    public String getCourseId() {
        return self().getGroupId();
    }

    @Override
    public Long getBranch() {
        return self().getBranchId();
    }

    @Override
    public String getName() {
        return self().getName();
    }

    @Override
    public String getUrl() {
        return self().getUrl();
    }


    @Override
    public List<String> getRights() {
        CourseComponent course = getCourseComponent();
        Set<Class<? extends Right>> rights =
          asJava(_courseAccessService.getUserRights(course.contextId(), _componentService));

        List<String> rightNames = new ArrayList<>();

        for(Class<? extends Right> right : rights){
            rightNames.add(right.getSimpleName());
        }

        return rightNames;
    }

    protected CourseComponent getCourseComponent() {
        return ComponentSupport.get(getId(), CourseComponent.class);
    }

    @Override
    public ContextProfilesApi getUsers() {
        return asComponent(ContextUsers.class);
    }

    @Override
    public IntegrationRootComponent getIntegrationRoot() {
        return asComponent(IntegrationRootComponent.class);
    }

    @Override
    public Boolean isLmsIntegrated() {
        ApiQuery query = new ApiQuery.Builder()
                .addPropertyMappings(IntegrationComponent.class)
                .addPrefilter(IntegrationComponent.PROPERTY_CONNECTOR + "." + SystemComponent.PROPERTY_IMPLEMENTATION, PredicateOperator.LESS_THAN_OR_EQUALS, LmsSystemComponent.class.getName())
                .setPage(0, 0)
                .build();
        return getIntegrationRoot().getIntegrations(query).getTotalCount() > 0L;
    }

    @Override
    public void delete() {
        _self.delete();
    }
}
