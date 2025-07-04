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

package loi.authoring.exchange.imprt.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.postgresql.JsonNodeUserType;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Type;
import org.hibernate.dialect.type.PostgreSQLJsonPGObjectJsonbType;

import java.util.Date;

import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;

@Entity
@Table(name = "assetimportreceipt", indexes = {
  @Index(name = "assetimportreceipt_root_idx", columnList = "root_id")
})
@Cache(usage = READ_WRITE)
public class ImportReceiptEntity {

    private Long id;
    private JsonNode data;
    private JsonNode importedRoots;
    private JsonNode report;
    private Long attachmentId;
    private String downloadFilename;
    private String status;
    private Date createTime;
    private Date startTime;
    private Date endTime;
    private Long createdBy;
    private Long root;
    private JsonNode source;
    private JsonNode unconvertedSource;

    // Hibernate apparently needs it
    // http://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#entity
    @SuppressWarnings("unused")
    ImportReceiptEntity() {
    }

    public ImportReceiptEntity(final Long id, final JsonNode data, final JsonNode importedRoots,
                               final JsonNode report, final Long attachmentId, final String downloadFilename, final String status,
                               final Date createTime, final Date startTime, final Date endTime,
                               final Long createdBy, final Long root, final JsonNode source, final JsonNode unconvertedSource) {
        this.id = id;
        this.data = data;
        this.importedRoots = importedRoots;
        this.report = report;
        this.attachmentId = attachmentId;
        this.downloadFilename = downloadFilename;
        this.status = status;
        this.createTime = createTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdBy = createdBy;
        this.root = root;
        this.source = source;
        this.unconvertedSource = unconvertedSource;
    }

    @Id
    @GeneratedValue(generator = "cpxp-sequence")
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Column(nullable = false, columnDefinition = "JSONB")
    @Type(JsonNodeUserType.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    public JsonNode getData() {
        return data;
    }

    public void setData(final JsonNode data) {
        this.data = data;
    }

    @Column(columnDefinition = "JSONB")
    @Type(JsonNodeUserType.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    public JsonNode getImportedRoots() {
        return importedRoots;
    }

    public void setImportedRoots(final JsonNode importedRoots) {
        this.importedRoots = importedRoots;
    }

    @Column(columnDefinition = "JSONB")
    @Type(JsonNodeUserType.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    public JsonNode getReport() {
        return report;
    }

    public void setReport(final JsonNode report) {
        this.report = report;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(final Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getDownloadFilename() {
        return downloadFilename;
    }

    public void setDownloadFilename(final String downloadFilename) {
        this.downloadFilename = downloadFilename;
    }

    @Column(nullable = false)
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(final Date endTime) {
        this.endTime = endTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    @Column(name = "root_id", nullable = false)
    public Long getRoot() {
        return root;
    }

    public void setRoot(final Long root) {
        this.root = root;
    }

    @Column(columnDefinition = "JSONB")
    @Type(JsonNodeUserType.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    public JsonNode getSource() {
        return source;
    }

    public void setSource(final JsonNode source) {
        this.source = source;
    }

    @Column(columnDefinition = "JSONB")
    @Type(JsonNodeUserType.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    public JsonNode getUnconvertedSource() {
        return unconvertedSource;
    }

    public void setUnconvertedSource(final JsonNode unconvertedSource) {
        this.unconvertedSource = unconvertedSource;
    }


}
