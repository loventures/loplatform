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

package loi.cp.util.converter;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.component.web.converter.StringConverterComponent;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.controller.upload.Uploads;

@Component
public class UploadConverter extends AbstractComponent implements StringConverterComponent<UploadInfo> {

    @Override
    public scala.Option<UploadInfo> apply(final StringConverter.Raw<UploadInfo> input) {
        // TODO: On POST this should be consumeUpload and no-ref
        var upload = Uploads.retrieveUpload(input.value());
        if (upload != null) upload.ref();
        return scala.Option.apply(upload);
    }
}
