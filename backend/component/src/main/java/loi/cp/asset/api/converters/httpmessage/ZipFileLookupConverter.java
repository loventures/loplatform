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

package loi.cp.asset.api.converters.httpmessage;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.converter.ConvertOptions;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverter;
import com.learningobjects.cpxp.component.web.exception.HttpMessageNotReadableException;
import com.learningobjects.cpxp.util.TempFileMap;
import com.learningobjects.cpxp.util.lookup.FileLookup;
import com.learningobjects.cpxp.util.lookup.FileLookups;
import com.learningobjects.de.web.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import static com.learningobjects.de.web.MediaType.isCompatibleWithPredicate;

/**
 * Reads a zip payload into a {@link FileLookup}. TODO support multipart/zip
 */
@Component
public class ZipFileLookupConverter extends AbstractComponent
        implements HttpMessageConverter<FileLookup> {

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.APPLICATION_ZIP);
    }

    @Override
    public boolean canRead(final Type type, final MediaType mediaType) {

        final boolean canReadMediaType = getSupportedMediaTypes().stream().anyMatch(isCompatibleWithPredicate(mediaType));
        final boolean canWriteType = type instanceof Class &&
                FileLookup.class.isAssignableFrom((Class<?>) type);

        return canReadMediaType && canWriteType;
    }

    @Override
    public boolean canWrite(final Object value, final MediaType mediaType) {
        return false;
    }

    @Override
    public FileLookup read(final RequestBody requestBody, final WebRequest request, final Type target) {

        final TempFileMap fileMap = new TempFileMap("zip-hmc", ".tmp");
        try {
            fileMap.importZip(request.getRawRequest().getInputStream());
        } catch (final IOException e) {
            throw new HttpMessageNotReadableException(e);
        }

        return FileLookups.lookup(fileMap);
    }

    @Override
    public void write(final FileLookup source, final ConvertOptions options,
                      HttpServletRequest request, final HttpServletResponse response) {
        throw new UnsupportedOperationException();
    }
}
