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

package com.learningobjects.cpxp.service.status;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.attachment.AttachmentService;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.util.NumberUtils;
import com.typesafe.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.RandomUtils;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StatusWebServiceBean extends BasicServiceBean implements StatusWebService {
    private static final Logger logger = Logger.getLogger(StatusWebServiceBean.class.getName());

    @Inject
    private Config config;

    /** The attachment service. */
    @Inject
    private AttachmentService _attachmentService;

    public void testCacheStorage() {
        String fileCacheDir = config.getString("com.learningobjects.cpxp.cache.file.basedir");
        testFile(new File(fileCacheDir));

    }

    public void testAttachmentStorage() {

        _attachmentService.testProviders();

    }

    public void testDatabase() {

        QueryBuilder qb = querySystem(DomainConstants.ITEM_TYPE_DOMAIN);
        Long value = qb.getAggregateResult(Function.COUNT);
        logger.log(Level.FINE, "Domain count, {0}", value);
        if (NumberUtils.longValue(value) == 0L) {
            throw new RuntimeException("Domain count error: " + value);
        }

    }

    private void testFile(File dir) {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String fileName = hostName + "-" + RandomUtils.nextInt(0, 65536) + ".tst";
            File file = new File(dir, fileName);
            FileUtils.writeStringToFile(file, fileName, CharEncoding.UTF_8);
            String result = FileUtils.readFileToString(file, CharEncoding.UTF_8);
            FileUtils.forceDelete(file);
            if (!result.equals(fileName)) {
                throw new RuntimeException("File write/read error: " + fileName + " / " + result);
            }
        } catch (IOException ex) {
            throw new RuntimeException("File error", ex);
        }
    }

}
