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

package com.learningobjects.cpxp.service.overlord;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.FileUtils;
import com.typesafe.config.Config;
import org.apache.commons.io.FilenameUtils;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Centralized log extraction.
 */
@Stateless()
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@SuppressWarnings("unused") // component
public class ExternalOverlordServiceBean extends BasicServiceBean
  implements ExternalOverlordService {
    @Inject
    private OverlordWebService _overlordWebService;

    @Inject
    private AttachmentService _attachmentService;

    @Inject
    private Config config;

    @Override
    public String getNodeName() {
        return BaseServiceMeta.getServiceMeta().getNode();
    }

    @Override
    public String getLogFile(Date time) throws Exception {
        final Config deTomcatConfig = config.getConfig("de.tomcat");
        final File logsDir = new File(deTomcatConfig.getString("logDirectory"));
        final String logger = deTomcatConfig.getString("logger");
        final String suffix = getSuffix(logger);
        // detomcat.20180119.json.gz or detomcat.20180120.json
        final Pattern rotated = Pattern.compile("detomcat\\.[0-9]*\\" + suffix + "(?:\\.gz)?");

        List<String> logNames = Arrays.asList(logsDir.list(
          (File dir, String name) -> rotated.matcher(name).matches()
        ));
        Collections.sort(logNames); // crude but works...

        // Search for the oldest logfile that closed after the target date.. if the log file
        // has rotated to S3 then this will be an incorrect logfile.
        File logFile = null;
        for (String name : logNames) {
            File file = new File(logsDir, name);
            Date when = new Date(file.lastModified()); // the date in the filename isn't terribly useful
            if (when.after(time)) {
                logFile = file;
                break;
            }
        }

        if (logFile == null) {
            logFile = new File(logsDir, "detomcat" + suffix);
        }

        Item item = _itemService.get(_overlordWebService.findOverlordDomainId());

        String tempFile;
        if (!logFile.getName().endsWith(".gz")) {
            try (UploadInfo tmp = UploadInfo.tempFile("detomcat" + suffix + ".gz")) {
                try (OutputStream out = new GZIPOutputStream(FileUtils.openOutputStream(tmp.getFile()))) {
                    FileUtils.copyFile(logFile, out);
                }
                tempFile = _attachmentService.createTemporaryBlob(item, getPrefix(logFile), suffix + ".gz", tmp.getFile());
            }
        } else {
            tempFile = _attachmentService.createTemporaryBlob(item, getPrefix(logFile), suffix + ".gz", logFile);
        }

        return tempFile;
    }

    private static String getPrefix(File logFile) {
        return "logs/" + BaseServiceMeta.getServiceMeta().getNode() + "/" + FilenameUtils.getBaseName(logFile.getName()) + "_";
    }

    // distasteful matching obscured away here
    private static String getSuffix(String logger) {
        if ("old".equals(logger)) return ".log";
        if ("jsonsingle".equals(logger)) return ".json";
        return "." + logger;
    }
}
