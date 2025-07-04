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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.copy.DefaultItemCopier;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.operation.OperationService;
import com.learningobjects.cpxp.service.thumbnail.ThumbnailService;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.task.Priority;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An implementation of the attachment web service
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AttachmentWebServiceBean extends BasicServiceBean implements AttachmentWebService {

    private static final Logger logger = Logger.getLogger(AttachmentWebServiceBean.class.getName());

    /** The attachment service. */
    @Inject
    private AttachmentService _attachmentService;

    /** The data service. */
    @Inject
    private DataService _dataService;

    /** The facade service. */
    @Inject
    private FacadeService _facadeService;

    /** The name service. */
    @Inject
    private NameService _nameService;

    /** The operation service. */
    @Inject
    private OperationService _operationService;

    // Map from attachment ID to Object, which we lock during zip access.
    // Want a ConcurrentMap which auto-populates with Object, since all we
    // really want are named locks.  Guava's LoadingCache would appear to be
    // right for the job.  Configured with soft values, so lock objects will
    // vanish if the zip hasn't been looked at recently.
    private static final LoadingCache<String, Object> ZIP_LOCKS = CacheBuilder.
            newBuilder().softValues().build(new CacheLoader<>() {
        @Override
        public Object load(String idOrBlobName) {
            return new Object();
        }
    });

    @Override
    public AttachmentFacade getAttachment(Long id) {

        AttachmentFacade facade = _facadeService.getFacade(id,
                AttachmentFacade.class);

        return facade;
    }

    @Override
    public AttachmentFacade getAttachment(Item item) {

        AttachmentFacade facade = _facadeService.getFacade(item,
                AttachmentFacade.class);

        return facade;
    }

    @Override
    public AttachmentFacade getRawAttachment(Long id) {

        AttachmentFacade facade = _facadeService.getFacade(id,
                AttachmentFacade.class);

        return facade;
    }

    public AttachmentFacade getScaledImage(Long id, int maxWidth, int maxHeight, boolean generate) {
        AttachmentFacade raw = getRawAttachment(id);
        if ((raw.getWidth() == null) || (maxWidth <= 0) || (maxHeight <= 0)) {
            return null;
        }
        Dimension scaled = getScaledDimension(new Dimension(raw.getWidth().intValue(), raw.getHeight().intValue()), new Dimension(maxWidth, maxHeight));
        return getRawAttachment(getScaledImage(id, scaled.width + "x" + scaled.height, generate));
    }

    static Dimension getScaledDimension(Dimension size, Dimension max) {
        boolean scaleX = max.width * size.height < max.height * size.width;
        int numerator = scaleX ? max.width : max.height;
        int denominator = scaleX ? size.width : size.height;
        return (numerator >= denominator) ? size : new Dimension(size.width * numerator / denominator, size.height * numerator / denominator);
    }

    // Returns:
    // a possibly uncreated facade if one has been created or scheduled
    @Override
    public Long getScaledImage(Long id, String geometry) {

        Long imageId = getScaledImage(id, geometry, true);

        return imageId;
    }

    private Long getScaledImage(Long id, String geometry, boolean generate) {

        Item image = _itemService.get(id);
        String geom = _attachmentService.getActualGeometry(image, geometry);
        if (geom != null) {
            Item scaled = _attachmentService.findScaledImage(image, geom);
            if (scaled != null) {
                image = scaled;
            } else if (true) { // TODO: if ACL permitted to do this
                // TODO: should it be create attachment on the parent, and
                // should it be non-anonymous???

                // TODO: When not called from setThumbnailGeometry the
                // whole lot should be done in a new tx? I do it this
                // way because I want the setThumbnailGeometry() method
                // to, at its completion, have already created the
                // stub scaled image items
                image = _attachmentService.createScaledImage(image, geom);
                if (generate) {
                    final Long imageId = flush(image);
                    _operationService.defer(new VoidOperation() {
                            @Override
                            public void execute() {
                                Operations.transact(new ScaleImageOperation(imageId));
                            }
                        }, Priority.High, "ThumbnailImage(" + imageId + ")", 5000L); // HACK: Wait 5s for scaling...
                }
            }
        }

        return flush(image);
    }

    private static class ScaleImageOperation extends VoidOperation {
        @Inject
        private ItemService _itemService;

        @Inject
        private ThumbnailService _thumbnailService;

        private Long _id;

        public ScaleImageOperation(Long id) {
            _id = id;
        }

        public void execute() {
            // I defer this rather than requesting it
            // because I need it to be done on the same host
            // so that a) the request processor doesn't run
            // before the cache invalidate has broadcast,
            // thus seeing stale data and b) we don't have
            // multiple appservers trying to update this
            // thing concurrently.
            Item image = _itemService.get(_id);
            if (image != null) {
                _thumbnailService.generateScaledImage(image);
            }
        }
    }

    @Override
    public void setThumbnailGeometry(Long id, String thumbnail) {
        AttachmentFacade facade = _facadeService.getFacade(id, AttachmentFacade.class);
        String old = facade.getThumbnail();

        if ((thumbnail == null) || !thumbnail.equals(old)) {
            facade.setThumbnail(thumbnail);
            facade.setGeneration(1L + NumberUtils.longValue(facade.getGeneration()));
            for (AttachmentFacade child : facade.getAttachments()) {
                child.delete();
            }
            // crap.. TODO: fix me, push this out to the controllers??
            final List<Long> ids = new ArrayList<Long>();
            for (String geometry : ThumbnailService.PROFILE_GEOMETRIES) {
                Long scaled = getScaledImage(id, geometry, false);
                if (!id.equals(scaled)) {
                    ids.add(scaled);
                }
            }
            _operationService.defer(new VoidOperation() {
                    @Override
                    public void execute() {
                        for (Long imageId : ids) {
                            Operations.transact(new ScaleImageOperation(imageId));
                        }
                    }
                }, Priority.High, "ThumbnailProfile(" + id + ")", 5000L); // HACK: Wait 5s for scaling...
        }

    }

    @Override
    public AttachmentFacade addAttachment(Long parentId, File file) {
        Item parent = _itemService.get(parentId);
        AttachmentFacade attachment = _facadeService.addFacade(parent, AttachmentFacade.class);
        attachment.setCreator(Current.getUser());
        attachment.setCreated(Current.getTime());
        if (file != null) {
            updateAttachment(attachment.getId(), file);
        }
        return attachment;
    }

    @Override
    public Long createAttachment(Long parentId, UploadInfo upload) {
        return createAttachment(parentId, upload, null);
    }

    @Override
    public Long createPublicAttachment(Long parentId, UploadInfo upload) {
        return createAttachment(parentId, upload, AttachmentAccess.Anonymous);
    }

    private Long createAttachment(Long parentId, UploadInfo upload, AttachmentAccess access) {
        AttachmentFacade attachment = addAttachment(parentId, upload.getFile());
        attachment.setFileName(upload.getFileName());
        attachment.setWidth(upload.getWidth());
        attachment.setHeight(upload.getHeight());
        attachment.setDisposition(Disposition.inline);
        attachment.setAccess(access);
        if (_nameService.getPath(parentId) != null) {
            attachment.bindUrl(upload.getFileName());
        }
        return attachment.getId();
    }

    @Override
    public void updateAttachment(Long id, File file) {
        Item item = _itemService.get(id);
        _attachmentService.updateAttachment(item, new ArrayList<>(), file);

    }

    @Override
    public void destroyAttachment(Long id) {

        Item attachment = _itemService.get(id);
        _attachmentService.destroyAttachment(attachment);

    }

    @Override
    public AttachmentFacade createPlaceholder(Id parentId) {

        Item parent = _itemService.get(parentId.getId());
        Item attachment = _attachmentService.createPlaceholder(parent);
        AttachmentFacade facade = _facadeService.getFacade(attachment, AttachmentFacade.class);

        return facade;
    }

    @Override
    public AttachmentFacade copyAttachment(final Long attachmentId, final Long parentId) {

        final Item attachment = _itemService.get(attachmentId);
        final Item destination = _itemService.get(parentId);
        final Item newAttachment = itemCopier(attachment).copyInto(destination);

        final AttachmentFacade newFacade =
          _facadeService.getFacade(newAttachment, AttachmentFacade.class);

        final String path = _nameService.getPath(parentId);
        if (path != null) {
            String pattern =
              getFilenameBindingPattern(path, newFacade.getFileName(), "Attachment");
            _nameService.setBindingPattern(attachmentId, pattern);
        }

        return newFacade;
    }

    @Override
    public AttachmentFacade copyAttachmentUnsafely(Long attachmentId, Long parentId) {

        final Item source = _itemService.get(attachmentId);
        final Item destination = _itemService.get(parentId);

        final Item newAttachment = new DefaultItemCopier(source, ServiceContext.getContext(), false, true).copyInto(destination);
        return _facadeService.getFacade(newAttachment, AttachmentFacade.class);
    }

    @Override
    public BlobInfo getAttachmentBlob(Long id) {

        Item attachment = _itemService.get(id);
        BlobInfo blobInfo = _attachmentService.getAttachmentBlob(attachment);

        return blobInfo;
    }

    @Override
    public FileInfo getCachedAttachmentBlob(Long id) {
        FileCache fileCache = FileCache.getInstance();
        AttachmentFacade facade = getRawAttachment(id);
        if (facade == null) {
            return null;
        }
        if (facade.getSize() > fileCache.getCacheAttachmentSize()) {
            return getAttachmentBlob(id);
        }
        String digest = facade.getDigest();
        String entryPath = "attach/" + digest.substring(0, 1) + "/" + digest.substring(1, 2) + "/" + digest.substring(2);
        FileHandle handle = fileCache.getFile(entryPath, Optional.of(digest), Optional.empty());
        try {
            boolean stale = false; // TODO: if sizes differ?
            if (stale || !handle.exists()) {
                handle.recreate(); // May change the handle file
                try {
                    FileInfo blob = getAttachmentBlob(id);
                    InputStream blobIn = blob.openInputStream();
                    try {
                        com.learningobjects.cpxp.util.FileUtils.copyInputStreamToFile(blobIn, handle.getFile());
                    } finally {
                        blobIn.close();
                    }
                    handle.created();
                } catch (Exception ex) {
                    handle.failed();
                    throw new RuntimeException("File cache error", ex);
                }
            }
            handle.ref();
            FileInfo fileInfo = new LocalFileInfo(handle.getFile(), handle::deref);
            fileInfo.setCachePath(entryPath);
            return fileInfo;
        } finally {
            handle.deref();
        }
    }

    @Override
    public void removeImage(Long id, String type) {

        Item item = _itemService.get(id);
        _attachmentService.removeImage(item,type);

    }

    @Override
    public List<AttachmentFacade> getAttachments(Item item) {
        List<AttachmentFacade> attachments = new ArrayList<>();
        for (Item attachment : findByParentAndType(item,
               AttachmentConstants.ITEM_TYPE_ATTACHMENT)) {
            attachments.add(getRawAttachment(attachment.getId()));
        }
        return attachments;
    }

    private File getZipAttachmentDirectory(final Long id) throws IOException {
        return getZipDirectory(id, Objects::toString, this::getAttachmentBlob);
    }

    private File getZipBlobDirectory(final BlobInfo blobInfo) throws IOException {
        return getZipDirectory(blobInfo, BlobInfo::getBlobName, Function.identity());
    }

    private <T> File getZipDirectory(final T id, Function<T, String> getCacheKey, Function<T, BlobInfo> getBlobInfo) throws IOException {
        Object lock;

        final String cacheKey = getCacheKey.apply(id);

        try {
            lock = ZIP_LOCKS.get(cacheKey);
        } catch (ExecutionException e) {
            throw new IOException("Exception thrown during ZIP_LOCKS cache load - should be impossible!", e);
        }
        synchronized(lock)  {
            File storageRoot = FileCache.getInstance().getCacheDir();
            final File storageDirectory = new File(storageRoot, cacheKey.replace('/', '_') + "_files");
            if (! storageDirectory.exists()) {
                BlobInfo attachment = getBlobInfo.apply(id);
                if (null == attachment) {
                    logger.log(Level.FINE, "Returning null because we can't find file for attachment "+id);
                    return (File)null;
                }
                try {
                    if (! storageDirectory.mkdirs() || ! storageDirectory.exists()) {
                        throw new IOException("Could not get unpacking directory for attachment "+id+" in "+storageDirectory.getCanonicalPath());
                    }
                    try (InputStream in = attachment.openInputStream()) {
                        List<String> toc = new ArrayList<String>();
                        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in), StandardCharsets.ISO_8859_1);
                        ZipEntry entry;
                        while ((entry = zipIn.getNextEntry()) != null) {
                            String name = entry.getName();
                            //instead of creating a file with the md5 hash of the full path
                            //TODO: how best to encode the path of the file, as to not choke on bad chars?
                            File resultFile = new File(storageDirectory, name);
                            if (name.endsWith("/")) {
                                resultFile.mkdirs();
                            } else {
                                if (!resultFile.getParentFile().exists()) {
                                    resultFile.getParentFile().mkdirs();
                                }
                                try (OutputStream out = FileUtils.openOutputStream(resultFile)) {
                                    IOUtils.copy(zipIn, out);
                                    toc.add(name);
                                }
                            }
                        }
                    }
                } catch (RuntimeException | IOException ex) {
                    FileUtils.deleteDirectory(storageDirectory);
                    throw ex;
                }
            }

            return storageDirectory;
        } // end of synchronized block
    }

    @Override
    public File getZipAttachmentFile(Long attachmentId, String pathInZip) throws IOException {
        File storageDirectory = getZipAttachmentDirectory(attachmentId);
        if (!storageDirectory.exists()) {
            throw new IOException("Missing attachment directory: " + attachmentId);
        }

        return new File(storageDirectory, pathInZip);
    }

    @Override
    public File getZipBlobFile(final BlobInfo blobInfo, final String pathInZip) throws IOException {
        File storageDirectory = getZipBlobDirectory(blobInfo);
        if (!storageDirectory.exists()) {
            throw new IOException("Missing attachment directory: " + blobInfo.getBlobName());
        }
        return new File(storageDirectory, pathInZip);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, File> getZipAttachmentFiles(Long attachmentId) throws IOException {
        File storageDirectory = getZipAttachmentDirectory(attachmentId);
        return getFilenamesRecursively(getZipAttachmentDirectory(attachmentId))
          .stream()
          .collect(Collectors.toMap(name -> name, name -> new File(storageDirectory, name)));
    }

    /**
     *
     * @param dir the directory to get the fine names from
     * @return a list of all file names in the directory passed & all of it's subdirectories. The values will be relative to the directory passed.
     */
    private List<String> getFilenamesRecursively(final File dir){
        Iterable<File> files = IteratorUtils.toList(
          FileUtils.iterateFilesAndDirs(dir, TrueFileFilter.TRUE, TrueFileFilter.TRUE)).stream().filter(file -> !file.isDirectory()).collect(Collectors.toList());
        return IteratorUtils.toList(files.iterator()).stream().map(file -> {
            //return the name of the file relative to the dir passed to us
            return file == null ? null : dir.toPath().relativize(file.toPath()).toString();
        }).collect(Collectors.toList());
    }

    @Override
    public ImageFacade copyImage(Long parentId, String dataType, ImageFacade image) {
        // This is just the worst...
        AttachmentFacade attachment = addAttachment(parentId, null);

        // copy the underlying attachment metadata
        AttachmentFacade source = image.asFacade(AttachmentFacade.class);
        attachment.setFileName(source.getFileName());
        attachment.setWidth(source.getWidth());
        attachment.setHeight(source.getHeight());
        attachment.setDisposition(source.getDisposition());
        attachment.setSize(source.getSize());
        attachment.setProvider(source.getProvider());
        attachment.setDigest(source.getDigest());
        attachment.bindUrl(attachment.getFileName());

        if (dataType != null) {
            _dataService.setItem(_itemService.get(parentId), dataType, _itemService.get(attachment.getId()));
        }
        return attachment;
    }
}
