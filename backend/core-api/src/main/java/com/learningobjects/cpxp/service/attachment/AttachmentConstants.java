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

/**
 * Attachment constants.
 */
public interface AttachmentConstants {
    String ITEM_TYPE_ATTACHMENT = "Attachment";

    String DATA_TYPE_ATTACHMENT_CLIENT_ID = "Attachment.clientId";

    String DATA_TYPE_ATTACHMENT_FILE_NAME = "Attachment.fileName";

    String DATA_TYPE_ATTACHMENT_SIZE = "Attachment.size";

    String DATA_TYPE_ATTACHMENT_WIDTH = "Attachment.width";

    String DATA_TYPE_ATTACHMENT_HEIGHT = "Attachment.height";

    String DATA_TYPE_ATTACHMENT_PROVIDER = "Attachment.provider";

    String DATA_TYPE_ATTACHMENT_DIGEST = "Attachment.digest";

    String DATA_TYPE_ATTACHMENT_GEOMETRY = "Attachment.geometry";

    String DATA_TYPE_ATTACHMENT_DISPOSITION = "Attachment.disposition";

    String DATA_TYPE_ATTACHMENT_THUMBNAIL = "Attachment.thumbnail"; // size+x+y of the thumbnail window

    String DATA_TYPE_ATTACHMENT_ACCESS = "Attachment.access";

    String DATA_TYPE_ATTACHMENT_GENERATION = "Attachment.generation";

    String DATA_TYPE_ATTACHMENT_REFERENCE = "Attachment.reference";

    String ATTACHMENT_DIGEST_BROKEN = "broken";
}
