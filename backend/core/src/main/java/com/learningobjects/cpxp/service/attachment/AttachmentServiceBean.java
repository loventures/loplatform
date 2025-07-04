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

import com.google.common.io.ByteSource;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.aws.AwsService;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataUtil;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.service.overlord.OverlordWebService;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Projection;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.tx.TransactionCompletion;
import com.typesafe.config.Config;
import jakarta.persistence.Query;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.FilePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;

/**
 * Attachment service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AttachmentServiceBean extends BasicServiceBean implements AttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentServiceBean.class.getName());

    private static final String COUNT_ATTACHMENT_PAGES = "SELECT count(a) FROM AttachmentFinder a "
            + "JOIN a.owner AS i "
            + "WHERE i.path LIKE CONCAT(:ancestor_path, '%') "
            + "AND a.geometry IS NULL AND a.del IS NULL";

    private static final String SELECT_ATTACHMENT_PAGE = "SELECT i "
            + "FROM AttachmentFinder a " + "JOIN a.owner AS i "
            + "WHERE i.path LIKE CONCAT(:ancestor_path, '%') "
            + "AND a.geometry IS NULL AND a.del IS NULL ORDER BY a.size DESC, a.fileName";

    private static final String SELECT_REF_COUNTS_PAGE = "SELECT count(*) as ref_count, a.filename, a.digest "
            + "FROM Item i "
            + "JOIN AttachmentFinder a ON a.path LIKE i.path || '%' "
            + "WHERE i.type_name IN ('Wiki', 'Blog', 'Podcast') "
            + "AND i.path LIKE :ancestor_path || '%' "
            + "AND i.root_id = :root_id "
            + "AND a.root_id = :root_id "
            + "AND a.geometry IS NULL AND a.del IS NULL "
            + "GROUP BY a.filename, a.digest";

    private static final String SELECT_ATTACHMENT_SITES_PAGE = "SELECT i.id, a.owner_id "
            + "FROM Item i "
            + "JOIN AttachmentFinder a ON a.path LIKE i.path || '%' "
            + "WHERE i.type_name IN ('Wiki', 'Blog', 'Podcast') "
            + "AND i.path LIKE :ancestor_path || '%' "
            + "AND i.root_id = :root_id "
            + "AND a.root_id = :root_id "
            + "AND a.geometry IS NULL AND a.del IS NULL "
            + "ORDER BY a.size DESC, a.filename";

    private static final String SELECT_DOMAIN_ATTACHMENT_COORDINATES =
            "SELECT DISTINCT provider, digest FROM AttachmentFinder WHERE " +
            "root_id = :root_id AND del IS NULL";

    /** The data service. */
    @Inject
    private DataService _dataService;

    /** The facade service. */
    @Inject
    private FacadeService _facadeService;

    /** The MIME web service. */
    @Inject
    private MimeWebService _mimeWebService;

    /** The name service. */
    @Inject
    private NameService _nameService;

    /** The overlord web service. */
    @Inject
    private OverlordWebService _overlordWebService;

    /** The query service. */
    @Inject
    private QueryService _queryService;

    @Inject
    private Config config;

    private Map<String,AttachmentProvider> _providers = new HashMap<>();
    private String _defaultProviderName       = null;

    private static final String FAILOVER_SUFFIX = ".failover";

    // Configuration of attachment providers takes place in
    // tomcat/conf/context.xml.  Each provider is configured under
    // AttachmentService/providers/[provider-name]/, with two mandatory
    // sub-fields being type and containerName.  So:
    //
    //   AttachmentService/providers/localFS/type=filesystem
    //   AttachmentService/providers/localFS/jclouds.credential=bogus
    //   AttachmentService/providers/localFS/jclouds.filesystem.basedir=/var/lib/cpfusion
    //   AttachmentService/providers/localFS/containerName=data
    //
    // configures a local filesystem provider called localFS, which will store
    // its contents under /var/lib/cpfusion/data (jclouds.credential is
    // unused here, but its absence crashes jclouds).  Similarly,
    //
    //   AttachmentService/providers/chS3/type=aws-s3
    //   AttachmentService/providers/chS3/jclouds.identity=<Access Key Id>
    //   AttachmentService/providers/chS3/jclouds.credential=<Secret Access Key>
    //   AttachmentService/providers/chS3/containerName=<bucket>
    //
    // configures an Amazon S3 provider called chS3 using the provided identity
    // (="Access Key Id") and credential (="Secret Access Key"), and storing
    // content in the "<bucket>" bucket.
    //
    // Several providers may be configured, and the "provider" field in
    // attachmentfinder in the database will identify which provider to obtain
    // a particular attachment from.  Two providers are special, and configured
    // in context.xml:
    //
    //   AttachmentService/defaultProvider=chS3
    //
    // ... defaultProvider is where all new attachments will be stored (except
    // in a domain copy --- see below).
    //
    // Copy domain will copy attachments *within their original domain*, so
    // that database "provider" fields remain accurate: a case could be made
    // for copying them into defaultProvider, but that would necessitate
    // setting all provider fields to defaultProvider, so I haven't implemented
    // it.
    //
    // Delete domain will delete in all providers.

    @PostConstruct
    @SuppressWarnings("unused")
    private void init() {
        Config attachmentConfig = config.getConfig("com.learningobjects.cpxp.attachment");
        _defaultProviderName = attachmentConfig.getString("defaultProvider");
        List<? extends Config> providers = attachmentConfig.getConfigList("providers");
        providers.forEach(this::fromConfig);
    }

    private void fromConfig(Config config) {
        Properties props = toProperties(config.getConfig("properties"));

        // if this is a failover it should report to the system under the non-failover name so attachments are correctly identified
        final String providerName = config.getString("name");
        final String failoverFor = configOpt(config, "failoverFor");
        final String logicalName, registeredName;
        if (StringUtils.isEmpty(failoverFor)) {
            logicalName = registeredName = providerName;
        } else {
            logicalName = failoverFor;
            registeredName = failoverFor + FAILOVER_SUFFIX;
        }

        BaseAttachmentProvider provider = new BaseAttachmentProvider(
          logicalName,
          config.getString("type"),
          props,
          config.getString("container"),
          configOpt(config, "region"),
          configOpt(config, "pathPrefix")
        );
        provider.initializeContainer();
        _providers.put(registeredName, provider);
    }

    private static String configOpt(Config config, String key) {
        return config.hasPath(key) ? config.getString(key) : "";
    }

    private static Properties toProperties(Config config) {
        Properties properties = new Properties();
        config.entrySet().forEach(e -> properties.setProperty(e.getKey(), config.getString(e.getKey())));
        return properties;
    }

    @Override
    public Item getAttachment(Long id) {

        Item attachment = _itemService.get(id);
        assertItemType(attachment,
               AttachmentConstants.ITEM_TYPE_ATTACHMENT);

        return attachment;
    }

    @Override
    public Item createAttachment(Item parent, Collection<Data> metaData,
            File file) {

        Item attachment = _itemService.create(parent,
                AttachmentConstants.ITEM_TYPE_ATTACHMENT);
        _dataService.setData(attachment, metaData);
        AttachmentFacade facade = _facadeService.getFacade(attachment, AttachmentFacade.class);
        facade.setCreator(Current.getUser());
        facade.setCreated(Current.getTime());
        facade.setEditor(Current.getUser());
        facade.setEdited(Current.getTime());
        if (file != null) {
            writeAttachment(file, attachment);
        }

        return attachment;
    }

    @Override
    public Item createPlaceholder(Item parent) {

        List<Data> metadata = new ArrayList<Data>();
        // CJCH TODO: Ideally want a PRNG with >>160 bits of entropy here;
        // using a secure PRNG is probably overkill though.
        byte[] bogusData = new byte[20];
        Random r = NumberUtils.getSecureRandom();
        r.nextBytes(bogusData);
        String digest = new String(Hex.encodeHex(bogusData));
        metadata.add(DataUtil.getInstance(
                AttachmentConstants.DATA_TYPE_ATTACHMENT_DIGEST, digest, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
        metadata.add(DataUtil.getInstance(
                AttachmentConstants.DATA_TYPE_ATTACHMENT_PROVIDER,
                _defaultProviderName, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
        Item attachment = createAttachment(parent, metadata, null);

        // String rv = getAttachmentBlobName(_defaultProvider, attachment, digest);

        return attachment;
    }

    @Override

    public void updateAttachment(Item attachment, Collection<Data> metaData,
            File file) {

        assertItemType(attachment,
                AttachmentConstants.ITEM_TYPE_ATTACHMENT);
        _dataService.setData(attachment, metaData);
        AttachmentFacade facade = _facadeService.getFacade(attachment, AttachmentFacade.class);
        facade.setEditor(Current.getUser());
        facade.setEdited(Current.getTime());
        facade.setGeneration(1L + NumberUtils.longValue(facade.getGeneration()));
        for (Item scaled : findByParentAndType(attachment,
          AttachmentConstants.ITEM_TYPE_ATTACHMENT)) {
            _itemService.delete(scaled);
        }
        if (file != null) {
            // TODO: scheduleBinaryRemovalTest(attachment);
            writeAttachment(file, attachment);
        }

    }

    // this is a hack for the dump service so it can avoid the document
    // service updating authorship...
    @Override
    public void storeAttachment(Item attachment, File file) {

        assertItemType(attachment,
               AttachmentConstants.ITEM_TYPE_ATTACHMENT);
        // TODO: scheduleBinaryRemovalTest(attachment);
        writeAttachment(file, attachment);

    }

    @Override
    public void destroyAttachment(Item attachment) {

        assertItemType(attachment,
               AttachmentConstants.ITEM_TYPE_ATTACHMENT);
        // TODO: scheduleBinaryRemovalTest(attachment);
        _itemService.delete(attachment);

    }

    @Override
    public BlobInfo getAttachmentBlob(Item attachment) {
        return getAttachmentBlob(attachment, true);
    }

    @Override
    public BlobInfo getAttachmentBlob(Item attachment, boolean typed) {

        BlobInfo blobInfo = null;
        String digest = DataTransfer.getStringData(attachment,
                AttachmentConstants.DATA_TYPE_ATTACHMENT_DIGEST);
        if (!StringUtils.isEmpty(digest) &&
                !AttachmentConstants.ATTACHMENT_DIGEST_BROKEN.equals(digest)) {
            String providerName = DataTransfer.getStringData(attachment,
                    AttachmentConstants.DATA_TYPE_ATTACHMENT_PROVIDER);
            AttachmentProvider p = getProvider(providerName);
            String blobName = getAttachmentBlobName(p, attachment, digest);
            String name = DataTransfer.getStringData(attachment, AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME);
            name = StringUtils.defaultString(name, digest);
            Long size = DataTransfer.getNumberData(attachment, AttachmentConstants.DATA_TYPE_ATTACHMENT_SIZE);
            blobInfo = new BlobInfo(null, p, name, blobName, size);
            String contentType = typed ? _mimeWebService.getMimeType(name) : // ack
                    null;
            blobInfo.setContentType(StringUtils.defaultString(contentType, // ack
                    MimeUtils.MIME_TYPE_APPLICATION_UNKNOWN));
        }

        if(blobInfo == null && config
          .getString("com.learningobjects.cpxp.network.cluster.type")
          .equals("Local")){
            logger.warn("Attachment was not found in default provider.  Attempting to find in other providers");
            for(AttachmentProvider p: _providers.values()) {
                String blobName = getAttachmentBlobName(p, attachment, digest);
                String name = DataTransfer.getStringData(attachment, AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME);
                name = StringUtils.defaultString(name, digest);
                Long size = DataTransfer.getNumberData(attachment, AttachmentConstants.DATA_TYPE_ATTACHMENT_SIZE);
                blobInfo = new BlobInfo(null, p, name, blobName, size);
                String contentType = typed ? _mimeWebService.getMimeType(name) : // ack
                  null;
                blobInfo.setContentType(StringUtils.defaultString(contentType, // ack
                  MimeUtils.MIME_TYPE_APPLICATION_UNKNOWN));
            }
        }


        return blobInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String digestFileForAttachment(File file)
            throws NoSuchAlgorithmException, IOException {
        VitalStatistics stats = digestStreamForAttachment(
                FileUtils.openInputStream(file));
        return stats.digest;
    }

    static private class VitalStatistics {
        public long size;
        public String digest;
    }

    public VitalStatistics digestStreamForAttachment(InputStream in)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        long size = 0;
        try {
            DigestInputStream digestIn = new DigestInputStream(in, md5);
            byte[] tmp = new byte[16384];
            int bytes;
            while ((bytes = digestIn.read(tmp)) > 0) {
                size += bytes;
            }
        } finally {
            in.close();
        }
        VitalStatistics rv = new VitalStatistics();
        rv.digest = new String(Hex.encodeHex(md5.digest()));
        rv.size = size;
        return rv;
    }

    private void writeAttachment(File file, Item attachment) {
        writeAttachment(new FilePayload(file), attachment);
    }

    private void writeAttachment(final Payload payload, final Item attachment) {
        try {
            final AttachmentProvider p = getDefaultProvider();
            final BlobStore bs = p.blobStore();

            final VitalStatistics stats = digestStreamForAttachment(
                    payload.openStream()); // chaos! if the payload is non repeatable then it has now been consumed...
            final String blobName = getAttachmentBlobName(p, attachment, stats.digest);

            // If the attachment is already in the database then check the blob store before uploading
            // it a second time. Not only does this save some S3 cost, re-uploading files seems
            // to cause them to sometimes be momentarily unavailable.
            if (attachmentExists(p, stats, attachment) && blobExists(p, stats, blobName)) {
                logger.info("Attachment already exists in blob store {}, {}", stats.size, stats.digest);
            } else {
                BlobStoreUtils.attemptBlobStoreOperation(() -> {
                    logger.info("Writing attachment {}, {}", stats.size, stats.digest);
                    long then = System.currentTimeMillis();
                    Blob blob = bs.blobBuilder(blobName).payload(payload).build();
                    bs.putBlob(p.container(), blob, multipart(stats.size > 1024));
                    long delta = System.currentTimeMillis() - then;
                    if (delta > 0) {
                        long rate = (long) NumberUtils.divide(1000L * stats.size, delta, 0);
                        NumberUtils.Unit unit = NumberUtils.getDataSizeUnit(rate, 1);
                        long scaled = (long) NumberUtils.divide(rate, unit.getValue(), 1);
                        String unitStr = (unit == NumberUtils.Unit.bytes) ? "B" : unit.toString();
                        logger.info("Attachment " + stats.digest + " written in " + delta + "ms, " + stats.size + " bytes, " + scaled + " " + unitStr + "s-1");
                    } else {
                        logger.info("Attachment " + stats.digest + " written in 0ms, " + stats.size + " bytes.");
                    }
                    return blob;
                }, S3Meta.apply(
                  payload.isRepeatable(),
                  scala.Option.apply(stats.size),
                  false
                ), S3Statistics.apply(p.name()));
            }
            AttachmentFacade facade = _facadeService.getFacade(attachment, AttachmentFacade.class);
            facade.setSize(stats.size);
            facade.setDigest(stats.digest);
            facade.setProvider(p.name());
        } catch (Exception ex) {
            logger.warn("Error preparing attachment", ex);
            throw new RuntimeException("Error preparing attachment",
              ex);
        }
    }

    /**
     * Check whether an attachment exists in the database, deleted or not,
     * that matches this provider and digest.
     */
    private boolean attachmentExists(AttachmentProvider p, VitalStatistics stats, Item att) {
        /* This service can and does create cross-domain attachments, particularly during domain bootstrap.
         * Therefore, check for the duplicate attachment in the attachment item's domain, not ours. */
        List<Long> existingAttachment = _queryService.queryRoot(att.getRoot().getId(), AttachmentConstants.ITEM_TYPE_ATTACHMENT)
          .setIncludeDeleted(true)
          .addCondition(AttachmentConstants.DATA_TYPE_ATTACHMENT_PROVIDER, Comparison.eq, p.name())
          .addCondition(AttachmentConstants.DATA_TYPE_ATTACHMENT_DIGEST, Comparison.eq, stats.digest)
          .setLimit(1)
          .setProjection(Projection.ID)
          .getResultList();
        return !existingAttachment.isEmpty();
    }

    /**
     * Check whether a blob exists in the blob store. This will raise an exception
     * if the blob exists but does not match expectations.
     */
    private boolean blobExists(final AttachmentProvider p, final VitalStatistics stats, final String blobName) throws IOException{
        if (!p.blobExists(blobName)) return false;
        Blob blob = p.getBlob(blobName);
        try (Payload payload = blob.getPayload()) {
            Long existingLength = payload.getContentMetadata().getContentLength();
            if (stats.size != existingLength) {
                throw new IllegalStateException("Existing attachment has invalid length: " + existingLength
                  + " vs " + stats.size + " for digest " + stats.digest + " at path " + blobName
                  + " in provider " + p.name() + " and container " + p.container());
            }
        }
        return true;
    }

    @Override
    public Item setImageData(Item item, String type, String fileName,
            Long width, Long height, String disposition, File imageFile) {

        Item image = DataTransfer.getItemData(item, type);
        try {
            List<Data> datas = new ArrayList<Data>();
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME,
                                    fileName, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas.add(DataUtil.getInstance(AttachmentConstants.DATA_TYPE_ATTACHMENT_WIDTH, width, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas.add(DataUtil.getInstance(AttachmentConstants.DATA_TYPE_ATTACHMENT_HEIGHT, height, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_DISPOSITION,
                                    StringUtils.defaultIfEmpty(disposition,
                                            "inline"), BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
                    .add(DataUtil
                            .getInstance(
                                    AttachmentConstants.DATA_TYPE_ATTACHMENT_THUMBNAIL,
                                    null, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
            datas
              .add(DataUtil
                .getInstance(
                  AttachmentConstants.DATA_TYPE_ATTACHMENT_ACCESS,
                  AttachmentAccess.LoggedIn.name(), BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));

            if (image == null) {
                image = createAttachment(item, datas, imageFile);
            } else {
                updateAttachment(image, datas, imageFile);
            }
            String path = _nameService.getPath(item);
            if (path != null) {
                String pattern = getFilenameBindingPattern(path, fileName,
                        "unknown");
                _nameService.setBindingPattern(image, pattern);
            }
            _dataService.setItem(item, type, image);
        } catch (Exception ex) {
            throw new RuntimeException("Error setting " + item
              + " image", ex);
        }

        return image;
    }

    @Override
    public void removeImage(Item item, String type) {

        Item image = DataTransfer.getItemData(item, type);
        if (image != null) {
            _itemService.delete(image);
            _dataService.setItem(item, type, null);
        }

    }

    @Override
    @Nonnull
    public AttachmentProvider getDefaultProvider() {
        return getProvider(_defaultProviderName);
    }

    @Override
    @Nonnull
    public AttachmentProvider getProvider(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null attachment provider");
        }
        final AwsService aws = ComponentSupport.lookupService(AwsService.class);
        if ((aws != null) && aws.s3Failover()) {
            final String failoverName = name + FAILOVER_SUFFIX;
            if (_providers.containsKey(failoverName)) {
                name = failoverName;
            }
        }
        AttachmentProvider provider = _providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown attachment provider: " + name);
        }
        return provider;
    }

    @Override
    public Collection<AttachmentProvider> getAttachmentProviders() {
        return _providers.values();
    }

    @Override
    public void destroyDomainBinaries(final Item domain) {

        // this will push the delete until the TX commits, skipping it if there is an error
        EntityContext.onCompletion(new TransactionCompletion() {
            final Long domainId = domain.getId();

            @Override
            public void onCommit() {

                logger.warn("Wiping domain binaries, {}", domainId);

                for (AttachmentProvider p : getAttachmentProviders()) {
                    BlobStore bs = p.blobStore();
                    String container = p.container();
                    String providerName = p.name();
                    String domainPath = getDomainName(p, domain);

                    // CJCH: urgh, turns out that for S3, directoryExists and
                    // deleteDirectory *do not work* under JClouds; some
                    // report that S3 stores folders as "<name>_$folder$", but
                    // no joy with that either (commented-out attempt below).
                    // As a result, I delete each blob individually.
                    // Brilliant.
                    ListContainerOptions opts = new ListContainerOptions().
                            inDirectory(domainPath).recursive();
                    long n = 0;
                    for (StorageMetadata md : bs.list(container, opts)) {
                        String name = md.getName();
                        try {
                            bs.removeBlob(container, name);
                            ++n;
                        } catch (Exception ex) {
                            throw new RuntimeException(
                              "Error removing blob " + container + ":" +
                                name + " from provider " + providerName,
                              ex);
                        }
                    }
                    logger.info("Removed " + n + " blobs from " +
                                                    providerName + ":" + container + ":" + domainPath);

//                    domainPath += "_$folder$";
//                    if (bs.directoryExists(container, domainPath)) {
//                        try {
//                            log(Level.INFO, "Deleting " + providerName + ":" +
//                                    container + ":" + domainPath);
//                            bs.deleteDirectory(container, domainPath);
//                        } catch (Exception ex) {
//                            throw logThrowing(new RuntimeException(
//                                    "Error wiping domain files for domain " +
//                                    domainId + ": " + providerName + ":" +
//                                    container + ":" + domainPath, ex));
//                        }
//                    }
                }
            }
        });

    }

    private Map<String, List<String>> getDomainAttachmentCoordinates(
            long domainId) {
        Query query = getEntityManager().createNativeQuery(
                SELECT_DOMAIN_ATTACHMENT_COORDINATES);
        query.setParameter("root_id", domainId);
        List<Object[]> results = query.getResultList();
        Map<String, List<String>> rv = new HashMap<String, List<String>>();
        for (Object[] result : results) {
            String provider = (String) result[0];
            String digest = (String) result[1];
            List<String> digests = rv.get(provider);
            if (digests == null) {
                digests = new ArrayList<String>();
                rv.put(provider, digests);
            }
            digests.add(digest);
        }
        return rv;
    }

    @Override
    public void copyDomainBinaries(Item domain, Item src) {
        logger.warn("Copying domain binaries, {}, {}", domain.getId(), src.getId());

        // CJCH TODO: Might be sensible to copy all providers to
        // _defaultProvider, except that'd involve patching all Attachment
        // entries' provider fields in the DB...
        Long srcId = src.getId();
        long dstId = domain.getId();
        Map<String, List<String>> coords = getDomainAttachmentCoordinates(srcId);
        for(Map.Entry<String,List<String>> e : coords.entrySet()) {
            String pName = e.getKey();
            List<String> digests = e.getValue();
            AttachmentProvider p = getProvider(pName);
            BlobStore bs = p.blobStore();
            String container = p.container();
            for (String digest : digests) {
                if (StringUtils.isEmpty(digest) || AttachmentConstants.ATTACHMENT_DIGEST_BROKEN.equals(digest)) {
                    continue;
                }
                String srcName = getDigestBlobName(p, srcId, digest);
                String dstName = getDigestBlobName(p, dstId, digest);
                Blob srcBlob =  BlobStoreUtils.attemptBlobStoreOperation(() -> bs.getBlob(container, srcName), S3Meta.repeatableEmpty(), S3Statistics.apply(p.name()));
                try (Payload srcPayload = srcBlob.getPayload()) {
                    final long length = srcPayload.getContentMetadata().getContentLength();
                    Blob dstBlob = bs.blobBuilder(dstName).payload(srcPayload).build();
                    BlobStoreUtils.attemptBlobStoreOperation(() -> bs.putBlob(container, dstBlob, multipart()), S3Meta.apply(
                      srcPayload.isRepeatable(),
                      scala.Option.apply(length),
                      false
                    ), S3Statistics.apply(p.name()));
//                System.err.println("Copied " + p.getName() + "/" + digest +
//                        " from " + srcId + " to " + dstId);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }

    // geometry is the requested geometry; I compute the actual geometry or null
    @Override
    public String getActualGeometry(Item image, String geometry) {

        String geom = null; // The actual geometry to build, or null
        Long w = DataTransfer.getNumberData(image,
               AttachmentConstants.DATA_TYPE_ATTACHMENT_WIDTH);
        Long h = DataTransfer.getNumberData(image,
               AttachmentConstants.DATA_TYPE_ATTACHMENT_HEIGHT);
        if ((geometry != null) && (w != null)) {
            long imgWidth = NumberUtils.longValue(w);
            long imgHeight = NumberUtils.longValue(h);
            if (geometry.indexOf("x") < 0) {
                long value = Long.parseLong(geometry);
                String thumbnail = DataTransfer
                        .getStringData(
                                image,
                               AttachmentConstants.DATA_TYPE_ATTACHMENT_THUMBNAIL);
                if (thumbnail == null) {
                    long min = Math.min(imgWidth, imgHeight);
                    long dim = Math.max(Math.min(min, value), min * 7 / 8);
                    long x = (imgWidth - dim) >> 1, y = (min - dim) >> 1;
                    thumbnail = dim + "+" + x + "+" + y;
                }
                // I will build a thumbnail iff the image has thumbnail
                // geometry and it's non-square or not exactly NxN
                // or the thumbnail geometry is not the entire image.
                if ((thumbnail != null)
                        && ((imgWidth != imgHeight) || (imgWidth != value) || !thumbnail
                                .equals(imgWidth + "+0+0"))) {
                    geom = thumbnail + "@" + value + "x" + value;
                }
            } else {
                long width = Long.parseLong(StringUtils.substringBefore(
                        geometry, "x"));
                long height = Long.parseLong(StringUtils.substringAfter(
                        geometry, "x"));
                if ((width < imgWidth) || (height < imgHeight)) {
                    if (width * imgHeight <= height * imgWidth) { // width is
                        // scaled more
                        height = Math.max(1, (imgHeight * width + imgWidth / 2)
                                / imgWidth);
                    } else {
                        width = Math.max(1, (imgWidth * height + imgHeight / 2)
                                / imgHeight);
                    }
                    // TODO: should i look for a near match? should i only do
                    // this if the scaling is somewhat substantial (50%)?
                    geom = width + "x" + height;
                }
            }
        }

        return geom;
    }

    @Override
    public Item findScaledImage(Item image, String geometry) {

        Item scaled = com.google.common.collect.Iterables.getFirst(findByParentAndTypeAndStringData(image,
                AttachmentConstants.ITEM_TYPE_ATTACHMENT,
                AttachmentConstants.DATA_TYPE_ATTACHMENT_GEOMETRY,
                geometry), null);

        return scaled;
    }

    @Override
    public Item createScaledImage(Item image, String geometry) {

        List<Data> metaData = new ArrayList<Data>();
        metaData.add(DataUtil.getInstance(
                AttachmentConstants.DATA_TYPE_ATTACHMENT_GEOMETRY,
                geometry, BaseOntology.getOntology(), ServiceContext.getContext().getItemService()));
        Item scaled = createAttachment(image, metaData, null);
        return scaled;
    }

    @Override
    public boolean hasData(Item attachment) {

        boolean has = (attachment != null)
                && (DataTransfer
                        .getStringData(
                                attachment,
                               AttachmentConstants.DATA_TYPE_ATTACHMENT_DIGEST) != null);

        return has;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countAttachmentPages(Item ancestor) {
        return (int) Math.ceil(countAttachments(ancestor) / 10.0D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countAttachments(Item ancestor) {
        Query query = getEntityManager().createQuery(COUNT_ATTACHMENT_PAGES);
        query.setParameter("ancestor_path", ancestor.getPath());
        Long result = (Long) query.getSingleResult();

        return (null == result) ? 0 : result.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Item> getAttachmentPage(Item ancestor, int index) {
        Query query = getEntityManager().createQuery(SELECT_ATTACHMENT_PAGE);
        query.setParameter("ancestor_path", ancestor.getPath());
        query.setFirstResult((index - 1) * 10);
        query.setMaxResults(10);
        return query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<Long, Item> getAttachmentSitesPage(Item ancestor, int index) {
        assert index > 0 : "Index should be greater than zero.";
        Query query = getEntityManager().createNativeQuery(
                SELECT_ATTACHMENT_SITES_PAGE);
        query.setParameter("ancestor_path", ancestor.getPath());
        query.setParameter("root_id", ancestor.getRoot());
        query.setFirstResult((index - 1) * 10);
        query.setMaxResults(10);
        List<Object[]> results = query.getResultList();
        Map<Long, Item> sites = new HashMap<Long, Item>();
        for (Object[] result : results) {
            Number siteId = (Number) result[0];
            Item site = _itemService.get(siteId.longValue());
            Number attachmentId = (Number) result[1];
            sites.put(attachmentId.longValue(), site);
        }
        return sites;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<MultiKey, Integer> getAttachmentReferencesPage(Item ancestor,
            int index) {
        assert index > 0 : "Index should be greater than zero.";
        Query query = getEntityManager().createNativeQuery(
                SELECT_REF_COUNTS_PAGE);
        query.setParameter("ancestor_path", ancestor.getPath());
        query.setParameter("root_id", ancestor.getRoot());
        query.setFirstResult((index - 1) * 10);
        query.setMaxResults(10);
        List<Object[]> results = query.getResultList();
        Map<MultiKey, Integer> referenceCounts = new HashMap<MultiKey, Integer>();
        for (Object[] result : results) {
            BigInteger referenceCount = (BigInteger) result[0];
            String filename = (String) result[1];
            String digest = (String) result[2];
            referenceCounts.put(new MultiKey(filename, digest), referenceCount.intValue());
        }
        return referenceCounts;
    }


    private String getAttachmentBlobName(AttachmentProvider p, Item attachment,
            String digest) {
        return getDigestBlobName(p, attachment.getRoot().getId(), digest);
    }

    private String getDigestBlobName(AttachmentProvider p, Long domainId,
            String digest) {
        return getBlobName(p, domainId, digest.substring(0, 1) + "/" +
                digest.substring(1, 2) + "/" + digest.substring(2, 3) + "/" +
                digest.substring(3));
    }

    private String getDomainName(AttachmentProvider p, Item domain) {
        return getBlobName(p, domain.getId(), null);
    }

    private String getBlobName(AttachmentProvider p, Long domainId,
            String name) {
        return p.getNameFor(domainId.toString(), name);
    }

    @Override
    public void testProviders() {
        final Long overlordDomainId = _overlordWebService.findOverlordDomainId();
        for (AttachmentProvider p : getAttachmentProviders()) {
            final BlobStore bs = p.blobStore();
            String testName = "status/";
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                testName += localHost.getHostName();
            } catch (UnknownHostException ex) {
                testName += "unknownHost";
            }
            testName += "-" + RandomUtils.nextInt(0, 65536) + ".tst";
            final String blobName = getBlobName(p, overlordDomainId, testName);
            BlobBuilder testBlobBuilder = bs.blobBuilder(blobName);
            final String testPayloadIn = testName + ": " + RandomUtils.nextInt(0, 65536);
            final ByteSource byteSource = ByteSource.wrap(testPayloadIn.getBytes());
            testBlobBuilder = testBlobBuilder.payload(byteSource);
            final Blob testBlob = testBlobBuilder.build();
            final String container = p.container();
            try {
                BlobStoreUtils.attemptBlobStoreOperation(() -> bs.putBlob(container, testBlob, multipart()), S3Meta.repeatableBounded(byteSource.size()), S3Statistics.apply(p.name()));
                final Blob gotBlob = BlobStoreUtils.attemptBlobStoreOperation(() -> bs.getBlob(container, blobName), S3Meta.repeatableEmpty(), S3Statistics.apply(p.name()));
                if (gotBlob == null) {
                    throw new RuntimeException(
                      "Attachment test failed for provider " + p.name() +
                        ": container " + container + ", blob " + testName +
                        " (returned null blob)");
                }
                final String testPayloadOut;
                try (InputStream testPayloadReader = gotBlob.getPayload().getInput()) {
                    testPayloadOut = IOUtils.toString(testPayloadReader, StandardCharsets.UTF_8);
                }
                if (!testPayloadIn.equals(testPayloadOut)) {
                    throw new RuntimeException(
                      "Attachment test failed for provider " + p.name() +
                        ": container " + container + ", blob " + testName +
                        " (read \"" + testPayloadOut + "\")");

                }
            } catch (IOException ex) {
                throw new RuntimeException(
                  "Attachment test failed for provider " + p.name() +
                    ": container " + container + ", blob " +
                    testName + " (got Exception: " +
                    ex.getMessage() + ")", ex);
            } finally {
                bs.removeBlob(container, testName);
            }
        }
    }

    @Override
    public String createTemporaryBlob(Item domain, String prefix,
            String suffix, File content) {
        final AttachmentProvider p = getProvider(_defaultProviderName);
        final BlobStore bs = p.blobStore();
        final String tempName = getBlobName(p, domain.getId(), "tmp/" +
            prefix + GuidUtil.guid() + suffix);
        BlobBuilder tempBlobBuilder = bs.blobBuilder(tempName);
        tempBlobBuilder = tempBlobBuilder.payload(content);
        Blob tempBlob = tempBlobBuilder.build();
        final String container = p.container();
        BlobStoreUtils.attemptBlobStoreOperation(() -> bs.putBlob(container, tempBlob, multipart()), S3Meta.repeatableBounded(content.length()), S3Statistics.apply(p.name()));
        // JClouds doesn't populate tempBob with any useful
        // information *such as its download URL*.  So get it from scratch, and
        // everything appears to be ok sigh.
        return tempName; // Always on the default provider
    }

    public BlobInfo getTemporaryBlob(String tempName) {
        final AttachmentProvider p = getProvider(_defaultProviderName);
        final BlobStore bs = p.blobStore();
        final String container = p.container();
        Blob tempBlob = BlobStoreUtils.attemptBlobStoreOperation(() -> bs.getBlob(container, tempName), S3Meta.repeatableEmpty(), S3Statistics.apply(p.name()));
        return new BlobInfo(tempBlob, p, tempName, tempName, -1L,
                new Runnable() {
            @Override
            public void run() {
                // Sure it'd be nice to remove the blob after download, but
                // we have no idea when that's gonna happen, so a periodic
                // cleaning script'll be needed.  Perhaps we could use URL
                // validity to clean up after 60 seconds?  CJCH TODO
                //bs.removeBlob(container, tempName);
            }
        });
    }

}
