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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.script.ComponentArchiveFacade;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ComponentArchiveSource implements ComponentSource {
    private final AttachmentWebService _attachmentWebService;
    private final FacadeService _facadeService;

    private final ComponentCollection _collection;
    private final Long _id;
    private final String _identifier, _prefix, _strip, _version;

    public ComponentArchiveSource(ComponentCollection collection, ComponentArchiveFacade scriptArchive) {
        _attachmentWebService = ServiceContext.getContext().getService(AttachmentWebService.class);
        _facadeService = ServiceContext.getContext().getService(FacadeService.class);

        _collection = collection;
        _id = scriptArchive.getId();
        _identifier = scriptArchive.getIdentifier();
        _version = scriptArchive.getVersion();
        _prefix = scriptArchive.getPrefix();
        _strip = scriptArchive.getStrip();
    }

    @Override
    public ComponentCollection getCollection() {
        return _collection;
    }

    @Override
    public String getIdentifier() {
        return _identifier;
    }

    @Override
    public String getVersion() {
        return _version;
    }

    @Override
    public long getLastModified() {
        return NumberUtils.longValue(_facadeService.getFacade(_id, ComponentArchiveFacade.class).getGeneration());
    }

    private Long getAttachmentId() {
        return _facadeService.getFacade(_id, ComponentArchiveFacade.class).getArchiveFile();
    }

    @Override
    public Optional<Path> getResource(String name) throws IOException {
        String path = _prefix + StringUtils.removeStart(name, _strip);
        return Optional.of(_attachmentWebService.getZipAttachmentFile(getAttachmentId(), path).toPath());
    }

    @Override
    public Map<String, Path> getResources() throws IOException {
        Map<String, Path> resources = new HashMap<>();
        _attachmentWebService.getZipAttachmentFiles(getAttachmentId()).forEach((name, file) -> {
            resources.put(_strip + StringUtils.removeStart(name, _prefix), file.toPath());
        });
        return resources;
    }

    @Override
    public long getLastModified(String name) {
        return getLastModified();
    }

    @Override
    public String toString() {
        return "VirtualComponentArchive[" + _id + "/" + _identifier + "]";
    }
}
