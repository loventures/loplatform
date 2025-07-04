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

package loi.cp.context.accesscode;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.group.GroupFacade;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.de.group.GroupComponent;
import loi.cp.accesscode.AbstractAccessCodeBatch;
import loi.cp.accesscode.AccessCodeComponent;
import loi.cp.accesscode.RedemptionComponent;
import loi.cp.accesscode.RedemptionSuccess;
import loi.cp.role.RoleComponent;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Component(
  alias = "loi.cp.course.accesscode.EnrollAccessCodeBatch")
public class EnrollAccessCodeBatch extends AbstractAccessCodeBatch implements EnrollAccessCodeBatchComponent {

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    @Inject
    private FacadeService _facadeService;

    @PostCreate
    private void initEnroll(EnrollAccessCodeBatchComponent init) {
        _instance.setItem(init.getRole());
        _instance.setAttribute("courseId", init.getCourseId());
    }

    @Override
    public Long getRole() {
        return _instance.getItem();
    }

    @Override
    public Long getCourseId() {
        return _instance.getAttribute("courseId", Long.class);
    }

    @Override
    public String getDescription() {
        return
          "Course: " + getCourseName() +
          ". Role: " + getRoleId() +
          ". Duration: " + getDuration() +
          ". Quantity: " + _instance.getQuantity() +
          ". Redemption Limit: " + getRedemptionLimit() + ".";
    }

    @Override
    public String getUse() {
        return  getCourseName();
    }

    @Override
    public String getUseName() {
        return "Course";
    }
    private String getCourseName() {
        var course = _facadeService.getFacade(getCourseId(), GroupFacade.class);
        return course.getName() + " (" + course.getGroupId() + ")";
    }

    private String getRoleId() {
        return _facadeService.getFacade(getRole(), RoleFacade.class).getRoleId();
    }

    @Override
    public void generateBatch(String prefix, Long quantity) {
        _instance.setQuantity(quantity);
        generateAccessCodes(quantity, prefix);
    }

    public AccessCodeComponent generateAccessCode(String prefix) {
        _instance.setQuantity(1L);
        return generateOne(prefix);
    }

    @Override
    public void importBatch(Boolean skipHeader, UploadInfo uploadInfo) {
        List<String> rows = loadCsv(uploadInfo, Boolean.TRUE.equals(skipHeader));
        _instance.addImport(uploadInfo);
        super.importAccessCodes(rows);
    }

    @Override
    public boolean validateContext(JsonNode context) { // expect { courseId: 1234 }
        return getCourseId().equals(context.get("courseId").longValue());
    }

    @Override
    public RedemptionSuccess redeemAccessCode(AccessCodeComponent accessCode, RedemptionComponent redemption) {
        //we need to use the approximate server time,
        //   b/c that's what enrollment queries use when filtering active enrollments
        Date startTime = DateUtils.getApproximateTime(redemption.getDate());
        Date endTime = null;
        String durationStr = getDuration();
        if (!UNLIMITED.equals(durationStr)) {
            long duration = DateUtils.parseDuration(durationStr).longValue();
            endTime = DateUtils.delta(redemption.getDate(), duration);
        }

        _enrollmentWebService.setEnrollment(
          getCourseId(), getRole(), redemption.getUser().getId(), "AccessCode", startTime, endTime);

        return new EnrollmentRedemptionSuccess(accessCode, getCourseId(), getRole());
    }

    /**
     * An initializer class for creating access enrollment access code
     * batchs from Java code.
     */
    public static class Init extends EnrollAccessCodeBatch {
        private final String name;
        private final Boolean disabled;
        private final String duration;
        private final Long redemptionLimit;
        private final Long role;
        private final Long course;

        public Init(final String name, final Boolean disabled, final String duration, final Long redemptionLimit, final Long course, final Long role) {
            this.name = name;
            this.disabled = disabled;
            this.duration = duration;
            this.redemptionLimit = redemptionLimit;
            this.course = course;
            this.role = role;
        }

        public Init(final GroupComponent course, final RoleComponent role) {
            this("Course Access", false, UNLIMITED, -1L, course.getId(), role.getId());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Boolean getDisabled() {
            return disabled;
        }

        @Override
        public String getDuration() {
            return duration;
        }

        @Override
        public Long getRedemptionLimit() {
            return redemptionLimit;
        }

        @Override
        public Long getRole() {
            return role;
        }

        @Override
        public Long getCourseId() {
            return course;
        }
    }
}
