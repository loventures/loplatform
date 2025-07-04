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

package com.learningobjects.cpxp.service.upgrade;

import jakarta.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.ColumnDefault;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;

import java.util.Date;

/**
 * A simple entity to represent the current version of the database schema.
 */
@Entity
@Cache(usage = READ_WRITE)
public class SystemInfo {

    private static final long serialVersionUID = 1L;

    /** The id primary key. */
    @Id
    private Long id;

    /**
     * Get the id.
     *
     * @return The id.
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the id.
     *
     * @param id
     *            The id.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /** The version of the database schema. */
    @Column
    private String version;

    /**
     * Get the version.
     *
     * @return The version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version.
     *
     * @param version The version.
     */
    public void setVersion(final String version) {
        this.version = version;
    }


    /** The central host. */
    @Column
    private String centralHost;

    /**
     * Get the central host.
     *
     * @return The central host.
     */
    public String getCentralHost() {
        return centralHost;
    }

    /**
     * Set the central host.
     *
     * @param centralHost The central host.
     */
    public void setCentralHost(final String centralHost) {
        this.centralHost = centralHost;
    }

    /** The last time the central host was set */
    @Column
    public Date centralHostTime;

    public Date getCentralHostTime() {
        return centralHostTime;
    }

    public void setCentralHostTime(final Date centralHostTime) {
        this.centralHostTime = centralHostTime;
    }

    @ColumnDefault("0") // so we don't need to muck about with Integer
    private int cdnVersion = 0;

    public int getCdnVersion() {
        return cdnVersion;
    }

    public void setCdnVersion(final int cdnVersion) {
        this.cdnVersion = cdnVersion;
    }

    @Column
    private String nodeName;

    public String getNodeName() {
       return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    @Column
    private String clusterId;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(final String clusterId) {
        this.clusterId = clusterId;
    }
}
