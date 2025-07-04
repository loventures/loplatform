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

package com.learningobjects.cpxp.service.thumbnail;

import com.learningobjects.cpxp.util.FileInfo;

import java.io.File;

/**
 * Describes a class that can generate image thumbnails.
 */
public abstract class Thumbnailer  {
    protected FileInfo _src;

    protected File _dst;

    protected int _cropX, _cropY, _cropWidth, _cropHeight;

    protected int _width, _height;

    protected boolean _forceSize;

    protected boolean _thumbnail;

    protected float _quality;

    public void setSource(FileInfo src) {
        _src = src;
    }

    public void setDestination(File dst) {
        _dst = dst;
    }

    public void setWindow(int x, int y, int width, int height) {
        _cropX = x;
        _cropY = y;
        _cropWidth = width;
        _cropHeight = height;
    }

    public void setDimensions(int width, int height, boolean force) {
        _width = width;
        _height = height;
        _forceSize = force;
    }

    public void setQuality(float quality) {
        _quality = quality;
    }

    public void setThumbnail(boolean thumbnail) {
        _thumbnail = thumbnail;
    }

    public abstract void thumbnail() throws Exception;
}
