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
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;

import java.util.List;

import static com.learningobjects.de.web.Queryable.Trait.CASE_INSENSITIVE;

@Controller(value = "accessCode", category = Controller.Category.LICENSING)
@Schema("accessCode")
@ItemMapping(value = AccessCodeConstants.ITEM_TYPE_ACCESS_CODE, singleton = true)
public interface AccessCodeComponent extends ComponentInterface, QueryableId {
    public static final String PROPERTY_BATCH_ID = "batch_id";

    @JsonProperty
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE, traits = CASE_INSENSITIVE)
    public String getAccessCode();

    @JsonProperty
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE_REDEMPTION_COUNT)
    public Long getRedemptionCount();

    @JsonProperty(PROPERTY_BATCH_ID)
    @Queryable(dataType = AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH)
    public Long getBatchId();

    @RequestMapping(path = "redemptions", method = Method.GET)
    public List<RedemptionComponent> getRedemptions();

    @RequestMapping(path = "batch", method = Method.GET)
    public AccessCodeBatchComponent getBatch();

    public AccessCodeState validate();

    public RedemptionSuccess redeem();

    void delete();

    public static class Init {
        public final String accessCode;
        public final AccessCodeBatchComponent batch;

        public Init(String accessCode, AccessCodeBatchComponent batch) {
            this.accessCode = accessCode;
            this.batch = batch;
        }
    }
}
