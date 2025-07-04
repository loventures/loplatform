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

package com.learningobjects.cpxp.service.data;

import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.item.Item;
import org.hibernate.annotations.Cache;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * A datum. This consists of a type, and either a number, string
 * (short, indexed) or text (long, unindexed) value.
 */
@Entity
@Cache(usage = READ_WRITE)
public class Data {

    /** The id primary key. */
    @Id
    @GeneratedValue(generator = "cpxp-sequence")
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
     * @param id The id.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /** The data type. */
    @Column(name = "TYPE_NAME", nullable = false)
    private String type;

    /**
     * Get the data type.
     *
     * @return The data type.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the data type.
     *
     * @param type The data type.
     */
    public void setType(final String type) {
        this.type = type;
    }

    /** The number value. */
    @Column
    private Long num;

    /**
     * Get the number value.
     *
     * @return The number value.
     */
    public Long getNumber() {
        return num;
    }

    /**
     * Set the number value.
     *
     * @param number The number value.
     */
    public void setNumber(final Long number) {
        this.num = number;
    }

    /** The string value. */
    @Column
    private String string;

    /**
     * Get the string value.
     *
     * @return The string value.
     */
    public String getString() {
        return string;
    }

    /**
     * Set the string value
     *
     * @param string
     */
    public void setString(final String string) {
        this.string = string;
    }

    /**
     * Set the string value and check against max length for the type
     * in Ontology.
     *
     * @param string The string value.
     * @param ontology
     */
    public void setString(final String string, final Ontology ontology) {
        if (type != null) {
            int length = ontology.getDataType(type).length();
            if ((string != null) && (string.length() > ((length < 0) ? 255 : length))) {
                throw new IllegalArgumentException("String too large for type `" + type + "': " + string);
            }
        }
        this.string = string;
    }

    /** The text value. */
    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY) // TODO: is ignored. make it happen.
    private String text;

    /**
     * Get the text value.
     *
     * @return The text value.
     */
    public String getText() {
        return text;
    }

    /**
     * Set the text value.
     *
     * @param text The text value.
     */
    public void setText(final String text) {
        this.text = text;
    }

    /** The item value. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Item item;

    /**
     * Get the item value.
     *
     * @return The item value.
     */
    public Item getItem() {
        return item;
    }

    /**
     * Set the item value.
     *
     * @param item The item value.
     */
    public void setItem(final Item item) {
        if ((item != null) && (root != null) && !root.equals(item.getRoot())) {
            throw new IllegalArgumentException("Cross-domain data: " + item.getId());
        }
        this.item = item;
    }

    /** The root for the owner item. */
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Item root;

    /**
     * Get the root of the owner.
     *
     * @return The root of the owner.
     */
    public Item getRoot() {
        return root;
    }

    /**
     * Set the root of the owner.
     *
     * @param root The root of the owner.
     */
    public void setRoot(final Item root) {
        this.root = root;
    }

    /** The owner item. */
    @ManyToOne
    @JoinColumn(nullable = false)
    private Item owner;

    /**
     * Get the owner.
     *
     * @return The owner.
     */
    public Item getOwner() {
        return owner;
    }

    /**
     * Set the owner.
     *
     * @param owner The owner.
     */
    public void setOwner(final Item owner) {
        this.owner = owner;
    }

    /**
     * Get the number value as a boolean.
     *
     * @return The boolean value.
     */
    public Boolean getBoolean() {
        return DataSupport.toBoolean(getNumber());
    }

    /**
     * Set the number value from a boolean. Truth is represented as one,
     * falsehood as zero.
     *
     * @param bool The boolean value.
     */
    public void setBoolean(final Boolean bool) {
        setNumber(DataSupport.toNumber(bool));
    }

    /**
     * Get the number value as a date.
     *
     * @return The date value.
     */
    public Date getTime() {
        return DataSupport.toTime(getNumber());
    }

    /**
     * Set the number value from a date. The date is represend as its
     * number of milliseconds since the epoch.
     *
     * @param date The date value.
     */
    public void setTime(final Date date) {
        setNumber(DataSupport.toNumber(date));
    }

    @Transient
    transient Object _json; // this is just when data is transiently encapsulating json; intentionally package private.

    /**
     * Get the value by examining the data format.
     *
     * @return the value
     * @param ontology
     */
    public Object getValue(final Ontology ontology) {
        switch (ontology.getDataFormat(type)) {
            case string:
            case path:
            case tsvector:
                return getString();
            case text:
                return getText();
            case number:
                return getNumber();
            case DOUBLE:
                String s = getString();
                return (s == null) ? null : Double.valueOf(s);
            case time:
                return getTime();
            case bool:
                return getBoolean();
            case item: {
                Item item = getItem();
                return ((item == null) || (item.getDeleted() != null)) ? null : item;
            }
            case json:
                return _json;
            case uuid:
                return UUID.fromString(getString());
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public String toString() {
        // crude value string
        String value = (string != null) ? ("/" + string)
            : (num != null) ? ("/" + num)
            : (item != null) ? ("/" + item)
            : "";
        return "Data[" + id + "/" + type + value + "]";
    }

}
