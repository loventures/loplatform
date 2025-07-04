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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.attachment.ImageFacade;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.component.misc.CourseConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType;
import com.learningobjects.cpxp.service.group.GroupFacade;
import com.learningobjects.cpxp.service.user.UserFacade;

import java.time.Instant;
import java.util.Optional;

@FacadeItem("Group")
public interface CourseFacade extends GroupFacade {
    @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
    public String getIdentifier();

    public void setIdentifier(String identifier);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_ID)
    public String getGroupId();

    public void setGroupId(String groupId);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_TYPE)
    public GroupType getGroupType();

    public void setGroupType(GroupType groupType);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_SUBTENANT)
    public Long getSubtenant();

    public void setSubtenant(Long subtenant);

    @FacadeData(value = DataTypes.DATA_TYPE_LOGO)
    public void setLogo(UploadInfo upload);

    public void setLogo(Id id);

    public ImageFacade getLogo();

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_MASTER_COURSE)
    public GroupFacade getMasterCourse();

    public void setMasterCourse(GroupFacade masterCourse);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_MASTER_COURSE)
    public Long getMasterCourseId();

    public void setMasterCourseId(Long id);

    @FacadeData(DataTypes.DATA_TYPE_CREATOR)
    public UserFacade getCreatedBy();

    public void setCreatedBy(UserFacade user);

    /**
     * Get the amount of time learners will be able to review the content, specified in hours.
     *
     * @return The amount of time allowed for reviewing in hours.
     */
    @FacadeData(CourseConstants.DATA_TYPE_COURSE_SHUTDOWN_DATE)
    public Optional<Instant> getShutdownDate();

    public void setShutdownDate(Optional<Instant> shutdownDate);
}
