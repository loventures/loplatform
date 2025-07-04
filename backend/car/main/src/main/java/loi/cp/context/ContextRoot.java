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
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.PathVariable;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.group.GroupFolderFacade;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import loi.cp.domain.DomainComponent;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class ContextRoot extends AbstractComponent implements ContextRootComponent {

    @Inject
    private FacadeService _facadeService;

    @Override
    public Optional<ContextComponent> getContext(Long id) {
        // omgoodness.. because these are decorators pending the big refactor[tm]
        Optional<ContextComponent> context;
        if (Current.getDomain().equals(id)) {
            context = Optional.ofNullable(ComponentSupport.get(id, DomainComponent.class));
        } else {
            context = Optional.ofNullable(ComponentSupport.get(id, CourseContextComponent.class));
        }
        return context;
    }

    public ApiQueryResults<CourseContextComponent> getLibraries(ApiQuery query){
        QueryBuilder librariesQb = _facadeService
          .getFacade("folder-libraries", GroupFolderFacade.class)
          .queryGroups();
        return ApiQuerySupport.query(librariesQb, query, CourseContextComponent.class);
    }

    @Override
    public void deleteContext(@PathVariable("id") Long id) {
        Optional<ContextComponent> maybeContext = getContext(id);
        if (maybeContext.isPresent()) {
            maybeContext.get().delete();
        } else {
            throw new ResourceNotFoundException("No context found for " + id);
        }
    }
}

