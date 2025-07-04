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

import com.learningobjects.cpxp.async.async.AsyncOperationActor;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.exception.HttpMediaTypeNotAcceptableException;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.component.misc.AccessCodeConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import scala.Tuple2;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractAccessCodeBatch extends AbstractComponent implements AccessCodeBatchComponent {
    private static final Logger logger = Logger.getLogger(AbstractAccessCodeBatch.class.getName());
    protected static final int CHUNK_SIZE = 64;

    protected static final int VALIDATE_CHUNK_SIZE = 1024;

    @Instance
    protected AccessCodeBatchFacade _instance;

    @PostCreate
    private void initAbstract(AccessCodeBatchComponent init) {
        _instance.setCreator(Current.getUserDTO());
        _instance.setCreateTime(Current.getTime());
        _instance.setName(init.getName());
        _instance.setDisabled(BooleanUtils.isTrue(init.getDisabled()));
        _instance.setRedemptionLimit(init.getRedemptionLimit());
        if (init.getDuration().equals(UNLIMITED)) {
            _instance.setDuration(UNLIMITED);
        } else {
            _instance.setDuration(DateUtils.parseDelta(init.getDuration()).toString()); // validate duration
        }
    }

    @Override
    public Long getId() {
        return Ids.get(_instance);
    }

    @Override
    public String getName() {
        return _instance.getName();
    }

    @Override
    public Long getCreator() {
        return Ids.get(_instance.getCreator());
    }

    @Override
    public Date getCreateTime() {
        return _instance.getCreateTime();
    }

    @Override
    public Boolean getDisabled() {
        return _instance.getDisabled();
    }

    @Override
    public void setDisabled(Boolean disabled) {
        _instance.setDisabled(disabled);
    }

    @Override
    public Long getRedemptionLimit() {
        return _instance.getRedemptionLimit();
    }

    @Override
    public String getDuration() {
        return _instance.getDuration();
    }

    @Override
    public void delete() {
        _instance.delete();
    }

    @Override
    public Tuple2<Long, Long> countRedemptions() {
        var qb = _instance.getParent()
                .queryAccessCodes()
                .addCondition(AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH, "eq", _instance);
        qb.setDataProjection(new DataProjection[]{
                BaseDataProjection.ofAggregateData(AccessCodeConstants.DATA_TYPE_ACCESS_CODE_REDEMPTION_COUNT, Function.SUM),
                BaseDataProjection.ofAggregateData(DataTypes.META_DATA_TYPE_ID, Function.COUNT),
        });
        var x = (Object[]) qb.getResult();
        var redemptions = x[0] == null ? 0L : (Long) x[0];
        var count = (Long) x[1];
        return Tuple2.apply(redemptions, count);
    }

    @Override
    public void export(WebRequest request, HttpServletResponse response) throws IOException {
        if (request.acceptsMediaType(MediaType.TEXT_CSV_UTF_8)) {
            exportCsv(response);
        } else {
            throw new HttpMediaTypeNotAcceptableException();
        }
    }

    protected void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_CSV_UTF_8_VALUE.toString());
        response.addHeader(HttpUtils.HTTP_HEADER_CONTENT_DISPOSITION, HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, getName() + ".csv"));
        Writer writer = response.getWriter();
        writer.write("Access Code,Use\r\n");
        for (String accessCode : getAllAccessCodes()) {
            writer.write(StringEscapeUtils.escapeCsv(accessCode));
            writer.write(',');
            writer.write(StringEscapeUtils.escapeCsv(getUse()));
            writer.write("\r\n");
        }
        writer.close();
    }

    protected List<String> getAllAccessCodes() {
        QueryBuilder qb = _instance.getParent().queryAccessCodes();
        qb.addCondition(AccessCodeConstants.DATA_TYPE_ACCESS_CODE_BATCH, "eq", _instance);
        List<String> accessCodes = qb.getProjectedResults(AccessCodeConstants.DATA_TYPE_ACCESS_CODE);
        return accessCodes;
    }

    protected void importAccessCodes(List<String> rows) {
        AccessCodeParentFacade parent = _instance.getParent();
        parent.lock(true);

        if (!getAllAccessCodes().isEmpty()) {
            throw new RuntimeException("Already generated");
        }

        for (int i = 0, quantity = rows.size(); i < quantity; i += CHUNK_SIZE) {
            var m = Math.min(CHUNK_SIZE, quantity - i);
            var chunk = rows.subList(i, i + m);
            Set<String> accessCodes = new HashSet<>(m);
            for (var code : chunk) {
                if (!accessCodes.add(code.toLowerCase())) {
                    throw new RuntimeException("Duplicate access codes");
                }
            }
            checkAccessCodes(chunk).ifPresent(ac -> {
                throw new RuntimeException("Existing access code: " + ac);
            });
            for (String code : chunk) {
                AccessCodeComponent.Init init = new AccessCodeComponent.Init(code, this);
                parent.addAccessCode(init);
            }
            EntityContext.flush(true);
            logger.log(Level.INFO, "Progress", quantity);
            AsyncOperationActor.tellProgress(i + m, quantity, "Importing access codes (" + (i + m) + "/" + quantity + ")");
        }
    }

    public CsvValidationResponse validateUpload(UploadInfo uploadInfo) {
        uploadInfo.ref(); // don't let it be autopurged...
        try {
            Set<String> accessCodes = new HashSet<>();
            List<String> data = loadCsv(uploadInfo, false);
            for (int i = 0, quantity = data.size(); i < quantity; i += VALIDATE_CHUNK_SIZE) {
                var m = Math.min(VALIDATE_CHUNK_SIZE, quantity - i);
                var chunk = data.subList(i, i + m);
                for (var code : chunk) {
                    if (!accessCodes.add(code.toLowerCase())) {
                        throw new RuntimeException("Duplicate access code: " + code);
                    }
                }
                checkAccessCodes(chunk).ifPresent(ac -> {
                    throw new RuntimeException("Existing access code: " + ac);
                });
            }
            int rows = data.size();
            return CsvSummary.apply(
                    rows,
                    CollectionConverters.asScala(data.subList(0, Math.min(4, rows))).toList()
            );
        } catch (Exception ex) {
            return CsvError.apply("Invalid upload: " + ex.getMessage());
        }
    }

    /**
     * WARNING: If called within an asynchronous operation this will clear
     * the Hibernate L1 cache.
     */
    protected void generateAccessCodes(Long number, String prefix) {
        AccessCodeParentFacade parent = getParentForGenerate();

        int quantity = number.intValue();
        boolean isAsync = (Current.get(AsyncOperationActor.TASK_ACTOR()) != null);
        // This runs in a bunch of chunks so I can check each chunk
        // and make sure there are no access code collisions. Much
        // more efficient than checking one at a time.
        for (int i = 0; i < quantity; i += CHUNK_SIZE) {
            int m = Math.min(CHUNK_SIZE, quantity - i);
            Set<String> accessCodes = new HashSet<>(m);
            do {
                accessCodes.clear();
                while (accessCodes.size() < m) {
                    accessCodes.add(generateAccessCode(prefix));
                }
            } while (checkAccessCodes(accessCodes).isPresent());
            for (String code : accessCodes) {
                AccessCodeComponent.Init init = new AccessCodeComponent.Init(code, this);
                parent.addAccessCode(init);
            }
            EntityContext.flush(isAsync); // only clear if async
            logger.log(Level.INFO, "Progress", quantity);
            AsyncOperationActor.tellProgress(i + m, quantity, "Generating access codes (" + (i + m) + "/" + quantity + ")");
        }
    }

    protected AccessCodeComponent generateOne(String prefix) {
        AccessCodeParentFacade parent = getParentForGenerate();

        String code;
        do {
            code = generateAccessCode(prefix);
        } while (checkAccessCodes(Collections.singleton(code)).isPresent());

        AccessCodeComponent.Init init = new AccessCodeComponent.Init(code, this);
        return parent.addAccessCode(init);
    }

    private AccessCodeParentFacade getParentForGenerate() {
        AccessCodeParentFacade parent = _instance.getParent();
        parent.lock(true);
        if (!getAllAccessCodes().isEmpty()) {
            throw new RuntimeException("Already generated");
        }
        return parent;
    }

    private static final int CHARS = 16; // 20 ** 16 =~ 2 << 69
    private static final BigInteger MODULUS = BigInteger.valueOf(20).pow(CHARS);

    // Access code looks like RT-BCDF-GHJK-LMNP-QRST.
    // RT- is a standard prefix, the remaining characters are random.
    // Vowels are unused to reduce the chance of objectionable language.
    private String generateAccessCode(String prefix) {
        BigInteger value = new BigInteger(1 + MODULUS.bitLength(), NumberUtils.getSecureRandom());
        String str = NumberUtils.toBase20Encoding(value.mod(MODULUS));
        str = StringUtils.repeat("B", CHARS - str.length()) + str.toUpperCase();
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < CHARS; i += 4) {
            sb.append('-').append(str, i, i + 4);
        }
        return sb.toString();
    }

    protected Optional<String> checkAccessCodes(Iterable<String> accessCodes) {
        // oh my, but support validating. Boy I wish I was a service.
        QueryBuilder qb = _instance == null
                ? ManagedUtils.getService(QueryService.class).queryRoot(AccessCodeConstants.ITEM_TYPE_ACCESS_CODE)
                : _instance.getParent().queryAccessCodes();
        Condition cond = BaseCondition.inIterable(AccessCodeConstants.DATA_TYPE_ACCESS_CODE, accessCodes);
        cond.setFunction(Function.LOWER.name());
        qb.addCondition(cond);
        qb.setLimit(1);
        return qb.getProjectedResults(AccessCodeConstants.DATA_TYPE_ACCESS_CODE).stream().map(o -> (String) o).findFirst();
    }

    protected List<String> loadCsv(final UploadInfo uploadInfo, Boolean skipHeader) {
        if (!StringUtils.endsWithIgnoreCase(uploadInfo.getFileName(), ".csv")) { // should use mime type
            throw new RuntimeException("Not a CSV");
        }
        try {
            List<String> data = new ArrayList<>();
            for (String line : FileUtils.readLines(uploadInfo.getFile(), "UTF-8")) {
                List<String> cols = StringUtils.splitCsv(line, ",");
                if (cols.isEmpty() || StringUtils.isBlank(cols.get(0))) {
                    continue;
                }
                if (Boolean.TRUE.equals(skipHeader)) {
                    skipHeader = false;
                } else {
                    data.add(cols.get(0));
                }
            }
            return data;
        } catch (Exception ex) {
            throw new RuntimeException("Import error", ex);
        }
    }
}
