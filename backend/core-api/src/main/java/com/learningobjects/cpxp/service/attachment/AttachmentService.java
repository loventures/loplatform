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

package com.learningobjects.cpxp.service.attachment;

import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.BlobInfo;
import com.learningobjects.cpxp.util.MultiKey;

import javax.annotation.Nonnull;
import javax.ejb.Local;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Attachment service.
 */
@Local
public interface AttachmentService {

    public Item getAttachment(Long id);

    public String getActualGeometry(Item image, String geometry);

    public Item findScaledImage(Item image, String geometry);

    public Item createScaledImage(Item image, String geometry);

    public boolean hasData(Item attachment);

    public Item createAttachment(Item parent, Collection<Data> metaData,
            File file);

    public Item createPlaceholder(Item parent);

    public void updateAttachment(Item attachment, Collection<Data> metaData,
            File file);

    public void storeAttachment(Item attachment, File file);

    public void destroyAttachment(Item attachment);

    public Item setImageData(Item item, String type, String fileName,
            Long width, Long height, String disposition, File imageFile);

    public void removeImage(Item item, String type);

    public BlobInfo getAttachmentBlob(Item attachment);
    public BlobInfo getAttachmentBlob(Item attachment, boolean typed);

    public void destroyDomainBinaries(Item domain);

    public void copyDomainBinaries(Item domain, Item src);

    /**
     * Digest an arbitrary file using the algorithm and encoding used for all
     * attachments.
     *
     * @param file
     *            file to digest and encoding
     * @return encoded digest of the file's contents
     * @throws NoSuchAlgorithmException
     *             in case of a mis-configuration and the digest algorithm is
     *             unavailable
     * @throws IOException
     *             if there are any problems reading the file
     */
    public String digestFileForAttachment(File file)
            throws NoSuchAlgorithmException, IOException;

    /**
     * Count the total pages of attachments under the ancestor.
     *
     * @param ancestor
     *            site, user, course or group for which attachments will be
     *            counted
     * @return total number of pages to determine previous and next links
     */
    public int countAttachmentPages(Item ancestor);

    /**
     * Count the total attachments under the ancestor.
     *
     *
     * @param ancestor
     *            site, user, course or group for which attachments will be
     *            counted
     * @return total number of attachments
     */
    public int countAttachments(Item ancestor);

    /**
     * Get a single page worth of attachments under the provided ancestor.
     *
     * @param ancestor
     *            site, user, course or group for which attachments will be
     *            fetched
     * @param index
     *            one based page index
     * @return one page or less
     */
    public List<Item> getAttachmentPage(Item ancestor, int index);

    /**
     * Fetch a map of attachments to the sites in which they are found, for a
     * particular page.
     *
     * @param ancestor
     *            site, user, course or group for which attachments will be
     *            fetched
     * @param index
     *            one based page index
     * @return the requested mapping
     */
    public Map<Long, Item> getAttachmentSitesPage(Item ancestor, int index);

    /**
     * Fetch a map of reference counts for the files to which Attachment
     * entities refer, for a particular page. Used to provider more helpful,
     * context sensitive feedback when deleting a references other than the last
     * remaining reference.
     *
     * @param ancestor
     *            site, user, course or group for which attachments will be
     *            fetched
     * @param index
     *            one based page index
     * @return the requested mapping
     */
    public Map<MultiKey, Integer> getAttachmentReferencesPage(Item ancestor,
            int index);

    /**
     * Run a write/read test against all providers.
     */
    public void testProviders();

    /**
     * Create a temporary Blob, to one day be deleted once its refcount hits zero,
     * returns its name.
     */
    public String createTemporaryBlob(Item domain, String prefix,
            String suffix, File content);

    /**
     * Get a temporary Blob.
     */
    public BlobInfo getTemporaryBlob(String name);


    @Nonnull
    AttachmentProvider getDefaultProvider();

    @Nonnull
    AttachmentProvider getProvider(@Nonnull String name);

    Collection<AttachmentProvider> getAttachmentProviders();
}
