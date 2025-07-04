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

package com.learningobjects.cpxp.service.replay;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * A table for deduplicating requests to prevent replay attacks.
 */
@Entity
public class Replay {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /** The id primary key. */
    @Id
    @GeneratedValue(generator = "cpxp-sequence")
    private Long id;

    /**
     * Get the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the id.
     *
     * @param id
     *            the id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /** The system id. */
    @Column
    private Long system;

    /**
     * Get the system id.
     *
     * @return the system id
     */
    public Long getSystem() {
        return system;
    }

    /**
     * Set the system id.
     *
     * @param system
     *            the system id
     */
    public void setSystem(final Long system) {
        this.system = system;
    }

    @Column
    private Date time;

    /**
     * The time the token was created.
     */
    public Date getTime() {
        return time;
    }

    public void setTime(final Date time) {
        this.time = time;
    }

    @Column
    private String nonce;

    /**
     * The token nonce.
     */
    public String getNonce() {
        return nonce;
    }

    public void setNonce(final String nonce) {
        this.nonce = nonce;
    }

    @Column
    private String ip;

    /**
     * The IP from which the nonce was received.
     */
    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    @Column
    private Date received;

    /**
     * The time the token was received.
     */
    public Date getReceived() {
        return received;
    }

    public void setReceived(final Date received) {
        this.received = received;
    }
}
