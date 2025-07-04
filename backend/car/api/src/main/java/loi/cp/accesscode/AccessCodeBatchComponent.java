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

package loi.cp.accesscode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;
import com.learningobjects.de.web.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import scala.Tuple2;

import javax.validation.groups.Default;
import java.util.Date;

@Controller(value = "accessCodeBatch", category = Controller.Category.LICENSING)
@Schema("accessCodeBatch")
@ItemMapping(AccessCodeConstants.ITEM_TYPE_ACCESS_CODE_BATCH)
public interface AccessCodeBatchComponent extends ComponentInterface, QueryableId {
    String UNLIMITED = "unlimited";

    @JsonProperty
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH_NAME, traits = Queryable.Trait.CASE_INSENSITIVE)
    String getName();

    @JsonView(Default.class)
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH_CREATOR)
    Long getCreator();

    @JsonView(Default.class)
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH_CREATE_TIME)
    Date getCreateTime();

    @JsonProperty
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH_DISABLED)
    Boolean getDisabled();

    void setDisabled(Boolean disabled);

    @JsonProperty
    Long getRedemptionLimit();

    @JsonProperty
    String getDuration();

    /** Displayed in UI */
    @JsonView(Default.class)
    String getDescription();

    /** Exported in CSV */
    @JsonView(Default.class)
    String getUse();

    @JsonView(Default.class)
    String getUseName();

    @RequestMapping(path = "redemptionCount", method = Method.GET)
    Tuple2<Long, Long> countRedemptions();

    @RequestMapping(path = "export", method = Method.GET)
    @ResponseBody("*/*") // implementation specific mime type
    void export(WebRequest request, HttpServletResponse response) throws Exception;

    boolean validateContext(JsonNode context);

    RedemptionSuccess redeemAccessCode(AccessCodeComponent accessCode, RedemptionComponent redemption);

    void delete();
}
