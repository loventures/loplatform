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

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.cpxp.service.exception.AccessForbiddenException;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.service.user.UserConstants;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.component.ComponentComponent;
import loi.cp.component.ComponentRootApi;
import loi.cp.gradebook.csv.CsvWriter;
import scala.compat.java8.OptionConverters;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AccessCodeRoot extends AbstractComponent implements AccessCodeRootComponent {
    private static final Logger logger = Logger.getLogger(AccessCodeRoot.class.getName());

    // -- Access Codes

    @Inject
    private FacadeService _facadeService;

    @Inject
    private QueryService _queryService;

    @Override
    public ApiQueryResults<AccessCodeComponent> getAccessCodes(ApiQuery query) {
        QueryBuilder qb = getParent().queryAccessCodes();
        return ApiQuerySupport.query(qb, query, AccessCodeComponent.class);
    }

    @Override
    public Optional<AccessCodeComponent> getAccessCode(Long id) {
        return getParent().getAccessCode(id);
    }

    @Override
    public void deleteAccessCode(Long id) {
        var code = getAccessCode(id).orElseThrow(() -> new ResourceNotFoundException("Access Code " + id));
        var del = Current.deleteGuid();
        code.delete();
        _queryService.createQuery("UPDATE RedemptionFinder SET del = :del WHERE accessCode.id = :ac")
                .setParameter("del", del)
                .setParameter("ac", id)
                .executeUpdate();
    }

    @Override
    public AccessCodeState validateAccessCode(final AccessCodeRedemption validation) {
        // this should be a validation or a disjunction, then i could just flatmap through
        logger.log(Level.INFO, "Validating access code: " + validation.getAccessCode() + " against schema " + validation.getSchema() + " in context " + ComponentUtils.toJson(validation.getContext()));
        return getParent().findAnyAccessCode(validation.getAccessCode()).map(acc -> {
            if (!schemaMatch(acc, validation.getSchema())) {
                logger.log(Level.INFO, "Schema mismatch for access code " + acc.getId());
                return AccessCodeState.Inapplicable;
            } else if (!contextMatch(acc, validation.getContext())) {
                logger.log(Level.INFO, "Context mismatch for access code " + acc.getId());
                return AccessCodeState.Inapplicable;
            } else {
                AccessCodeState state = acc.validate();
                logger.log(Level.INFO, "Access code validity for access code " + acc.getId() + ": " + state);
                return state;
            }
        }).orElseGet(() -> {
            logger.log(Level.INFO, "No matching access code found");
            return AccessCodeState.Invalid;
        });
    }

    @Override
    public RedemptionSuccess redeemAccessCode(String ac, @Nullable String schema, @Nullable JsonNode context) {
        return redeemAccessCode(new AccessCodeRedemption(ac, schema, context));
    }

    @Override
    public RedemptionSuccess redeemAccessCode(final AccessCodeRedemption redemption) {
        logger.log(Level.INFO, "Redeeming access code: " + redemption.getAccessCode());
        return getParent().findAnyAccessCode(redemption.getAccessCode()).map(acc -> {
            if (!schemaMatch(acc, redemption.getSchema()) || !contextMatch(acc, redemption.getContext())) {
                throw new AccessForbiddenException("incorrect access code schema/context");
            }
            return acc.redeem();
        }).orElseGet(() -> {
            throw new AccessForbiddenException("irredeemable access code");
        });
    }

    private boolean schemaMatch(AccessCodeComponent accessCode, @Nullable String schema) {
        return (schema == null) || schema.equals(ComponentSupport.getSchemaName(accessCode.getBatch()));
    }

    private boolean contextMatch(AccessCodeComponent accessCode, @Nullable JsonNode context) {
        // treat null, "null", "{}" as equivalently absenting context check
        return (context == null) || context.isNull() || context.isEmpty() || accessCode.getBatch().validateContext(context);
    }

    @Override
    public Optional<AccessCodeComponent> getAnyAccessCode(String accessCode) {
        return getParent().findAnyAccessCode(accessCode);
    }

    // -- Batches

    @Override
    public <T extends AccessCodeBatchComponent> T addBatch(T batch) {
        return getParent().addBatch(batch);
    }

    @Override
    public ApiQueryResults<AccessCodeBatchComponent> getBatches(ApiQuery query) {
        QueryBuilder qb = getParent().queryBatches();
        return ApiQuerySupport.query(qb, query, AccessCodeBatchComponent.class);
    }

    @Override
    public Optional<AccessCodeBatchComponent> getBatch(Long id) {
        return getParent().getBatch(id);
    }

    @Override
    public AccessCodeBatchComponent transitionBatch(Long id, Boolean disabled) {
        var batch = getBatch(id).orElseThrow(() -> new ResourceNotFoundException("Batch " + id));
        batch.setDisabled(disabled);
        return batch;
    }

    @Override
    public void deleteBatch(Long id) {
        var batch = getBatch(id).orElseThrow(() -> new ResourceNotFoundException("Batch " + id));
        var del = Current.deleteGuid();
        batch.delete();
        _queryService.createQuery("UPDATE AccessCodeFinder SET del = :del WHERE batch.id = :batch")
                .setParameter("del", del)
                .setParameter("batch", id)
                .executeUpdate();
        // No nice queries in hibernate
        _queryService.createQuery("UPDATE RedemptionFinder SET del = :del WHERE accessCode.id IN (SELECT id FROM AccessCodeFinder WHERE batch.id = :batch)")
                .setParameter("del", del)
                .setParameter("batch", id)
                .executeUpdate();
    }

    // -- Batch Components

    // TODO: Embed ComponentRootComponent directly when I support rights mapping.

    @Override
    public ApiQueryResults<ComponentComponent> getBatchComponents(ApiQuery query) {
        return ComponentSupport.createSingletonComponent(
                        ComponentRootApi.class, AccessCodeBatchComponent.class)
                .getComponents(query);
    }

    @Override
    public Optional<ComponentComponent> getBatchComponent(String identifier) {
        return OptionConverters.toJava(ComponentSupport.createSingletonComponent(
                        ComponentRootApi.class, AccessCodeBatchComponent.class)
                .getComponent(identifier));
    }

    // -- Folder

    public static final String FOLDER_TYPE_ACCESS_CODE = "accessCode";

    public AccessCodeParentFacade getParent() {
        return _facadeService
                .getFacade(Current.getDomain(), AccessCodeRootFacade.class)
                .getFolderByType(FOLDER_TYPE_ACCESS_CODE);
    }

    @Override
    public void exportRedemptions(Long id, WebRequest request, HttpServletResponse response) {
        var batch = getBatch(id).orElseThrow(() -> new ResourceNotFoundException("Batch " + id));

        var qb = _queryService.queryRoot(AccessCodeConstants.ITEM_TYPE_ACCESS_CODE)
                .addCondition(AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH, "eq", batch.getId())
                .setDataProjection(BaseDataProjection.ofDatum(AccessCodeConstants.DATA_TYPE_ACCESS_CODE))
                .setLogQuery(true);

        var rqb = _queryService.queryAllDomains(AccessCodeConstants.ITEM_TYPE_REDEMPTION)
                .setDataProjection(BaseDataProjection.ofDatum(AccessCodeConstants.DATA_TYPE_REDEMPTION_DATE));

        qb.addJoin(new Join.Left("#id", rqb, AccessCodeConstants.DATA_TYPE_REDEMPTION_ACCESS_CODE, DNF.empty()));

        var uqb = _queryService.queryAllDomains(UserConstants.ITEM_TYPE_USER)
                .setDataProjection(new DataProjection[]{
                        BaseDataProjection.ofDatum(UserConstants.DATA_TYPE_GIVEN_NAME),
                        BaseDataProjection.ofDatum(UserConstants.DATA_TYPE_FAMILY_NAME),
                        BaseDataProjection.ofDatum(UserConstants.DATA_TYPE_EMAIL_ADDRESS),
                });

        rqb.addJoin(new Join.Left("#parent", uqb));


        var duration = batch.getDuration();

        List<Object[]> results = qb.getResultList();

        Date now = Current.getTime();
        DateFormat fmt = new SimpleDateFormat("M/d/yyyy h:mm a, z");

        try {
            response.setContentType(MediaType.TEXT_CSV_UTF_8_VALUE);
            String filename = batch.getName() + "_Redemptions_" + new SimpleDateFormat("yyyy-MM-dd").format(now) + ".csv";
            response.addHeader(HttpUtils.HTTP_HEADER_CONTENT_DISPOSITION, HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, filename));
            Writer writer = response.getWriter();
            CsvWriter csv = new CsvWriter(writer);

            csv.write("Access Code");
            csv.write("Name");
            csv.write("Email Address");
            csv.write("Redemption Date");
            csv.write("Expiration Date");
            csv.write(batch.getUseName());

            csv.crlf();

            for (Object[] result : results) {
                var accessCode = (String) result[0];
                var redemptionDate = (Date) result[1];
                var givenName = (String) result[2];
                var familyName = (String) result[3];
                var emailAddress = (String) result[4];

                Date expirationDate = null;
                // Transform duration into an end time
                if (redemptionDate != null && !duration.equalsIgnoreCase(AccessCodeBatchComponent.UNLIMITED)) {
                    long dur = DateUtils.parseDuration(duration);
                    expirationDate = DateUtils.delta(redemptionDate, dur);
                }

                csv.write(accessCode);
                csv.write(givenName == null
                    ? familyName == null ? "" : familyName
                    : familyName == null ? givenName : givenName + " " + familyName);
                csv.write(emailAddress);
                csv.write(redemptionDate == null ? "" : fmt.format(redemptionDate));
                csv.write(expirationDate == null ? "" : fmt.format(expirationDate));
                csv.write(batch.getUse());

                csv.crlf();
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format("Failed to write CSV from the redemption: %s", e.getMessage()), e);
            throw new InvalidRequestException("");
        }
    }
}

