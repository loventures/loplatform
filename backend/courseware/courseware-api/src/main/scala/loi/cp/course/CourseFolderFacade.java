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

package loi.cp.course;

import com.learningobjects.cpxp.dto.FacadeComponent;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.folder.FolderConstants;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.group.GroupFolderFacade;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import scaloi.GetOrCreate;

import java.util.List;
import java.util.Optional;

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
public interface CourseFolderFacade extends GroupFolderFacade {

    @FacadeComponent
    public List<CourseComponent> getCourses();
    public CourseComponent getCourse(Long id);
    public Optional<CourseComponent> findCourseByGroupId(
      @FacadeCondition(value = GroupConstants.DATA_TYPE_GROUP_ID, function = Function.LOWER) String groupId
    );
    public Optional<CourseComponent> findCourseByExternalId(
      @FacadeCondition(value = GroupConstants.DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER, function = Function.LOWER)
      String externalId
    );

    public <T extends CourseComponent> T addCourse(Class<T> implClass, CourseComponent.Init init);
    public CourseComponent addCourse(String implementation, CourseComponent.Init init);
    public <T extends CourseComponent> T addCourse(String implementation, T prototype);
    public <T extends CourseComponent> T addCourse(T prototype);
    public GetOrCreate<CourseComponent> getOrCreateCourse(
      @FacadeCondition(value = GroupConstants.DATA_TYPE_GROUP_ID, function = Function.LOWER)
      String groupId,
      String impl,
      CourseComponent.Init init
    );

    public GetOrCreate<CourseComponent> getOrCreateCourseByExternalId(
      @FacadeCondition(value = GroupConstants.DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER, function = Function.LOWER)
      String xid,
      String impl,
      CourseComponent.Init init
    );
    public GetOrCreate<CourseComponent> getOrCreateCourseByOfferingAndExternalId(
            @FacadeCondition(GroupConstants.DATA_TYPE_GROUP_MASTER_COURSE) Long oid,
            @FacadeCondition(value = GroupConstants.DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER, function = Function.LOWER)
                    String xid,
            String impl,
            CourseComponent.Init init
    );
    public GetOrCreate<CourseComponent> getOrCreateCourseBySubquery(
      @FacadeCondition(value = DataTypes.META_DATA_TYPE_ID, comparison = Comparison.in)
      QueryBuilder subQuery,
      String impl,
      CourseComponent.Init init
    );
    public GetOrCreate<CourseComponent> getOrCreateCourseByOfferingAndSubquery(
            @FacadeCondition(GroupConstants.DATA_TYPE_GROUP_MASTER_COURSE) Long oid,
            @FacadeCondition(value = DataTypes.META_DATA_TYPE_ID, comparison = Comparison.in)
                    QueryBuilder subQuery,
            String impl,
            CourseComponent.Init init
    );
    public Optional<CourseComponent> findCourseBySubquery(
      @FacadeCondition(value = DataTypes.META_DATA_TYPE_ID, comparison = Comparison.in)
      QueryBuilder subQuery
    );

    scala.Option<CourseComponent> findCourseByBranchAndAsset(
      @FacadeCondition(GroupConstants.DATA_TYPE_GROUP_BRANCH) Long branch,
      @FacadeCondition(GroupConstants.DATA_TYPE_GROUP_LINKED_ASSET_NAME) String asset
    );
}
