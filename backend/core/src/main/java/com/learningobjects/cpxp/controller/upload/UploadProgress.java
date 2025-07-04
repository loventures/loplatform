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

package com.learningobjects.cpxp.controller.upload;

import com.learningobjects.cpxp.util.FormattingUtils;
import com.learningobjects.cpxp.util.InternationalizationUtils;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;

import java.io.Serializable;

public class UploadProgress implements Serializable {
    private String _fileName;
    private Long _progress;
    private Long _totalSize;
    private String _state;
    private long _created;
    private long _updated;

    public UploadProgress() {
        _created = System.currentTimeMillis();
    }

    public String getFileName() {
        return _fileName;
    }

    public void setFileName(String fileName) {
        _fileName = fileName;
    }

    public Long getProgress() {
        return _progress;
    }

    public void setProgress(Long progress) {
        _progress = progress;
        _updated = System.currentTimeMillis();
    }

    public Long getTotalSize() {
        return _totalSize;
    }

    public void setTotalSize(Long totalSize) {
        _totalSize = totalSize;
    }

    public String getState() {
        return _state;
    }

    public void setState(String state) {
        _state = state;
    }

    public String getFormattedProgress() {
        if (!StringUtils.isEmpty(_state)) {
            return InternationalizationUtils.formatMessage("upload_UploadProgress_uploadState_" + _state);
        }
        long progress = NumberUtils.longValue(_progress);
        if (progress <= 0L) {
            return "";
        }
        long total = NumberUtils.longValue(_totalSize);
        if (total <= 0L) {
            return FormattingUtils.formatSize(progress);
        } else {
            double percent = 0.99 * progress / total;
            NumberUtils.Unit unit0 = NumberUtils.getDataSizeUnit(progress, 1);
            long scaled0 = (long) NumberUtils.divide(progress, unit0.getValue(), 1);
            NumberUtils.Unit unit1 = NumberUtils.getDataSizeUnit(total, 1);
            // 1KB of 10MB .. 1 of 10MB
            String fmt0 = unit0.equals(unit1)
                ? InternationalizationUtils.format("{0,number}", scaled0)
                : FormattingUtils.formatSize(progress);
            String fmt1 = FormattingUtils.formatSize(total);
            long rate = (long) NumberUtils.divide(1000L * progress, _updated - _created, 0);
            String fmtRate = FormattingUtils.formatRate(rate);
            long eta = (_updated - _created) * (total - progress) / progress;
            String fmtEta = FormattingUtils.formatDuration(eta);
            return InternationalizationUtils.formatMessage("upload_UploadProgress_formatProgress", percent, fmtEta, fmt0, fmt1, fmtRate);
        }
    }

    public void setFormattedProgress(String progress) {
        // for Jsonizer
    }
}
