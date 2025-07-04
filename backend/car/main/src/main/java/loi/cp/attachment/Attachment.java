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

package loi.cp.attachment;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.web.ErrorResponse;
import com.learningobjects.cpxp.component.web.FileResponse;
import com.learningobjects.cpxp.component.web.RedirectResponse;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.*;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.exception.ValidationInfo;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.de.web.MediaType;
import loi.cp.right.RightService;
import loi.cp.user.Profile;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Attachment extends AbstractComponent implements AttachmentComponent, ItemSiteComponent {
    private static final Logger logger = Logger.getLogger(Attachment.class.getName());

    @Inject
    private AttachmentWebService _attachmentWebService;

    @Inject
    private MimeWebService _mimeWebService;

    @Inject
    private RightService _rightService;

    @Instance
    protected AttachmentFacade _self;

    @Override
    public Long getId() {
        return _self.getId();
    }

    @Override
    public String getClientId() {
        return _self.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        if (getClientId() != null) {
            throw new RuntimeException("Cannot set clientId once it has been set.");
        }
        if (StringUtils.isEmpty(clientId)) {
            throw new ValidationException(Collections.singletonList(new ValidationInfo("clientId", null, "Cannot set empty clientId")));
        }
        _self.setClientId(clientId);
    }

    public long getVersion() {
        return NumberUtils.longValue(_self.getGeneration());
    }

    @Override
    public Date getCreateTime() {
        return _self.getCreated();
    }

    @Override
    public Profile getCreator() {
        return ComponentSupport.get(_self.getCreator(), Profile.class);
    }

    @Override
    public String getFileName() {
        return _self.getFileName();
    }

    @Override
    public String getMimeType() {
        return _mimeWebService.getMimeType(getFileName());
    }


    @Override
    public Long getSize() {
        return _self.getSize();
    }

    @Override
    public Long getHeight() {
        return _self.getHeight();
    }

    @Override
    public Long getWidth() {
        return _self.getWidth();
    }

    @Override
    public ResourceDTO toDTO() {
        return ResourceDTO.apply(_self);
    }

    private static final String DOMAIN_PREFIX = "/Domain/";

    private boolean isDomainMedium() {
        return StringUtils.startsWith(_self.getUrl(), DOMAIN_PREFIX);
    }

    @Override
    public WebResponse view(Boolean download, Boolean direct, String size) {
        return viewInternal(download, direct, size, isDomainMedium(), false);
    }

    @Override
    public WebResponse viewInternal(Boolean download, Boolean direct, String size, boolean safe, boolean noDirect) {
        if (StringUtils.isEmpty(_self.getDigest())) {
            return ErrorResponse.notFound(); // still being generated..
        } if (AttachmentConstants.ATTACHMENT_DIGEST_BROKEN.equals(_self.getDigest())) {
            return ErrorResponse.serverError();
        } else if (size != null) {
            return thumbnail(size);
        }
        FileInfo info = noDirect ? _attachmentWebService.getAttachmentBlob(_self.getId()) :  _attachmentWebService.getCachedAttachmentBlob(_self.getId());
        info.setLastModified(getCreateTime());
        final String mimeType = getMimeType();
        final boolean isSafe = (safe || MimeUtils.isSafe(mimeType));
        final String safeMimeType = isSafe ? mimeType : MediaType.TEXT_PLAIN_VALUE;
        info.setContentType(safeMimeType);
        info.setDoCache(false);
        String disposition = (Boolean.TRUE.equals(download) || !isSafe) ? HttpUtils.DISPOSITION_ATTACHMENT
          : HttpUtils.DISPOSITION_INLINE;
        info.setDisposition(HttpUtils.getDisposition(disposition, getFileName()));
        info.setNoRedirect(Boolean.TRUE.equals(direct));
        return FileResponse.apply(info);
    }

    public WebResponse thumbnail(String size) {
        Optional<ThumbnailSize> thumbnailSize = ThumbnailSize.parse(size);
        if (!thumbnailSize.isPresent() || (getWidth() == null)) {
            return ErrorResponse.badRequest(); // not an image or not a valid requested size
        }
        int dim = thumbnailSize.get().getSize();
        AttachmentFacade scaled = _attachmentWebService.getScaledImage(getId(), dim, dim, !Current.isAnonymous()); // anon cannot generate an image
        if ((scaled == null) || StringUtils.isEmpty(scaled.getDigest())) {
            return view(false, false, null); // thumbnail doesn't exist yet so serve full size
        } else {
            return ComponentSupport.get(scaled, AttachmentComponent.class).view(false, false, null);
        }
    }

    @Override
    public void delete() {
        _self.invalidateParent();
        _self.delete();
    }

    @Override
    public void lock(boolean pessimistic) {
        _self.lock(pessimistic);
    }

    @Override
    public Long getParentId() {
        return _self.getParentId();
    }

    @Override
    public Optional<Long> getReference() {
        return _self.getReference();
    }

    private static final Pattern GEOMETRY_RE = Pattern.compile("(\\d+)(x(\\d+))?");
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    /**
     * Legacy URL-based rendering behaviour
     */
    @Nonnull
    @Override
    public WebResponse renderSite(String view) {
        AttachmentAccess access = _self.getAccess().orElse(AttachmentAccess.Admin);
        if ((access == AttachmentAccess.LoggedIn) && Current.isAnonymous() ||
            (access == AttachmentAccess.Admin) && !_rightService.getUserRights().isEmpty()) {
            return ErrorResponse.unauthorized();
        }
        Matcher matcher = GEOMETRY_RE.matcher(StringUtils.defaultString(view));
        boolean doScale = matcher.matches();
        boolean isThumbnail = doScale && StringUtils.isEmpty(matcher.group(2));
        boolean noWait = doScale && !isThumbnail;
        boolean doCache = true;

        Long originalId = _self.getId();
        Long attachmentId = doScale ? _attachmentWebService.getScaledImage(originalId, matcher.group(0)) : originalId;
        AttachmentFacade attachment = _attachmentWebService.getAttachment(attachmentId);
        String digest = (attachment == null) ? null : attachment.getDigest();
        boolean isMissing = com.learningobjects.cpxp.util.StringUtils.isEmpty(digest);
        boolean isBroken = AttachmentConstants.ATTACHMENT_DIGEST_BROKEN.equals(digest);
        // The noWait flag was used when the image is displayed in the rich
        // editor so you don't immediately see the loading image - if the
        // scaled image is not ready, i'll display the original instead.
        // pageEditorInit.js then strips the ,nowait option from the image
        // source when it saves, so that the usual behaviour occurs. In the
        // no wait but no thumbnail scenario, we don't cache the request in
        // the server or browser. No wait was then made the default because
        // it sucks being shown a loading spinner.
        if (doScale && noWait && (isBroken || isMissing)) {
            attachmentId = originalId;
            attachment = _attachmentWebService.getAttachment(attachmentId);
            digest = (attachment == null) ? null : attachment.getDigest();
            if (isMissing) { // If it's broken then do cache it...
                doCache = false;
            }
            isMissing = isBroken = false;
        }
        if (doScale && (attachment != null) && (isMissing || isBroken)) {
            int value = Integer.parseInt(matcher.group(1));
            int min = isThumbnail ? value : Math.min(value, Integer.parseInt(matcher.group(3)));
            int dim = (min < 32) ? 16 : (min < 48) ? 32 : (min < 64) ? 48 : (min < 96) ? 64 : (min < 128) ? 96 : (min < 160) ? 128 : 160;
            String icon = isBroken ? ("broken/" + dim + "x" + dim + ".png")
              : ("wait/" + dim + "x" + dim + ".gif");
            logger.log(Level.FINE, ("Sending " + icon + " in lieu of scaled image") + " {0}", attachment);
            return RedirectResponse.temporary(FormattingUtils.staticUrl("/images/" + icon));
        }
        if (StringUtils.isEmpty(digest)) {
            return ErrorResponse.notFound();
        }
        FileInfo fileInfo = _attachmentWebService.getCachedAttachmentBlob(attachmentId);
        fileInfo.setDoCache(doCache); // TODO: allow caching of domain media
        final var safe = isDomainMedium();
        fileInfo.setEternal(safe); // TODO: Hackus horribilis
        fileInfo.setIsLocalized(false);
        Date date = ObjectUtils.getFirstNonNullIn(attachment.getEdited(), attachment.getCreated());
        fileInfo.setLastModified(ObjectUtils.getFirstNonNullIn(date, Current.getTime()));
        String contentType = _mimeWebService.getMimeType(attachment.getFileName());
        final var isSafe = safe || MimeUtils.isSafe(contentType);
        final var safeContentType = isSafe ? contentType : MediaType.TEXT_PLAIN_VALUE;
        fileInfo.setContentType(StringUtils.defaultString(safeContentType, MimeUtils.MIME_TYPE_APPLICATION_UNKNOWN));
        String disposition = doScale ? null : view; // arse
        if ((disposition == null) && (attachment.getDisposition() != null)) {
            disposition = attachment.getDisposition().name();
        } else if (StringUtils.isEmpty(disposition))  {
            disposition = Disposition.inline.name();
        }
        if (attachment.getFileName() != null) {
            disposition = HttpUtils.getDisposition(disposition, attachment.getFileName());
        }
        fileInfo.setDisposition(disposition);
        fileInfo.setExpires(doCache ? 365 * DAY_MS : 0L); // 6d
        logger.log(Level.FINE, "Sending file" + " {0}", fileInfo);
        return FileResponse.apply(fileInfo);
    }
}
