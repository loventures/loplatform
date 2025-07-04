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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.Mode;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.web.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.component.ComponentComponent;
import loi.cp.web.challenge.ChallengeGuard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
@Controller(value = "accessCodes", root = true, category = Controller.Category.LICENSING)
@RequestMapping(path = "accessCodes")
@Secured(AccessCodeAdminRight.class)
public interface AccessCodeRootComponent extends ApiRootComponent, ComponentDecorator {

    @RequestMapping(method = Method.GET)
    @Secured(overrides = true, value = AccessCodeSupportRight.class)
    ApiQueryResults<AccessCodeComponent> getAccessCodes(@MaxLimit(256) ApiQuery query);

    @RequestMapping(path = "{id}", method = Method.GET)
    @Secured(overrides = true, value = AccessCodeSupportRight.class)
    Optional<AccessCodeComponent> getAccessCode(@PathVariable("id") Long id);

    @RequestMapping(path = "{id}", method = Method.DELETE)
    void deleteAccessCode(@PathVariable("id") Long id);

    @RequestMapping(path = "validate", method = Method.POST, mode = Mode.READ_ONLY)
    @Secured(overrides = true, guard = ChallengeGuard.class, allowAnonymous = true)
    AccessCodeState validateAccessCode(@RequestBody AccessCodeRedemption validation);

    @RequestMapping(path = "redeem/{accessCode}", method = Method.POST)
    @Secured(overrides = true)
    @Deprecated
        // use the alternative without path info
    RedemptionSuccess redeemAccessCode(@PathVariable("accessCode") String accessCode,
                                       @Nullable @QueryParam(required = false) String schema,
                                       @Nullable @RequestBody JsonNode context);

    @RequestMapping(path = "redeem", method = Method.POST)
    @Secured(overrides = true)
    RedemptionSuccess redeemAccessCode(@RequestBody AccessCodeRedemption redemption);

    Optional<AccessCodeComponent> getAnyAccessCode(String accessCode);

    /* batches */

    @RequestMapping(path = "batches", method = Method.POST)
    public <T extends AccessCodeBatchComponent> T addBatch(@RequestBody T batch);

    @RequestMapping(path = "batches", method = Method.GET)
    public ApiQueryResults<AccessCodeBatchComponent> getBatches(@MaxLimit(100) ApiQuery query);

    @RequestMapping(path = "batches/{id}", method = Method.GET)
    public Optional<AccessCodeBatchComponent> getBatch(@PathVariable("id") Long id);

    @RequestMapping(path = "batches/{id}/disabled", method = Method.PUT)
    public AccessCodeBatchComponent transitionBatch(@PathVariable("id") Long id, @RequestBody Boolean disabled);

    @RequestMapping(path = "batches/{id}", method = Method.DELETE)
    void deleteBatch(@PathVariable("id") Long id);

    @ResponseBody("text/csv")
    @RequestMapping(path = "batches/{id}/redemptionReport", method = Method.GET)
    void exportRedemptions(@PathVariable("id") Long id, WebRequest request, HttpServletResponse response);

    /* batch components */

    @RequestMapping(path = "batchComponents", method = Method.GET)
    public ApiQueryResults<ComponentComponent> getBatchComponents(ApiQuery query);

    @RequestMapping(path = "batchComponents/{identifier}", method = Method.GET)
    public Optional<ComponentComponent> getBatchComponent(@PathVariable("identifier") String identifier);

    class AccessCodeRedemption {
        private final String accessCode;
        private final String schema;
        private final JsonNode context;

        @JsonCreator
        public AccessCodeRedemption(@JsonProperty(value = "accessCode", required = true) final String accessCode,
                                    @JsonProperty("schema") final String schema,
                                    @JsonProperty("context") final JsonNode context) {
            this.accessCode = accessCode;
            this.schema = schema;
            this.context = context;
        }

        @JsonProperty
        @Nonnull
        public String getAccessCode() {
            return accessCode;
        }

        @JsonProperty
        @Nullable
        public String getSchema() {
            return schema;
        }

        @JsonProperty
        @Nullable
        public JsonNode getContext() {
            return context;
        }
    }
}
