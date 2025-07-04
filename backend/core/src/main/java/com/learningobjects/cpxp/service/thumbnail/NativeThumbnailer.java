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

import org.apache.commons.io.FilenameUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A native (in the sense pure-Java) thumbnailer.
 *
 * Note that this provides distinctly low-quality results per the many
 * discussions on the Web. For significant scaling operations we really
 * need to apply a lowpass filter first.
 */
public class NativeThumbnailer extends Thumbnailer {
    private static final Logger logger = Logger.getLogger(NativeThumbnailer.class.getName());

    public void thumbnail() throws Exception {

        logger.log(Level.INFO, "Thumbnailing image, {0}, {1}, {2}, {3}", new Object[]{_src, _dst, _width, _height});
        InputStream in = _src.openInputStream();
        BufferedImage srcImage;
        try {
            srcImage = ImageIO.read(in);
        } finally {
            in.close();
        }

        if (_cropWidth > 0) {
            srcImage = srcImage.getSubimage(_cropX, _cropY, _cropWidth, _cropHeight);
        }

        BufferedImage dstImage = srcImage;
        int srcWidth = srcImage.getWidth(), srcHeight = srcImage.getHeight();
        if (_forceSize ? ((_width != srcWidth) || (_height != srcHeight))
                       : ((_width < srcWidth) || (_height < srcHeight))) {
            int width, height;
            if (_forceSize) {
                width = _width;
                height = _height;
            } else if (_width * srcHeight < _height * srcWidth) { // scale more horizontally
                width = _width;
                height = srcHeight * _width / srcWidth;
            } else {
                width = srcWidth * _height / srcHeight;
                height = _height;
            }

            int type = srcImage.getType();
            if (type == BufferedImage.TYPE_CUSTOM) {
                type = BufferedImage.TYPE_INT_RGB;
            }
            dstImage = new BufferedImage(width, height, type);
            Graphics2D g = dstImage.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                AffineTransform scale = AffineTransform.getScaleInstance((double) width / srcWidth, (double) height / srcHeight);
                g.drawRenderedImage(srcImage, scale);
            } finally {
                g.dispose();
            }
        }

        ImageOutputStream ios = ImageIO.createImageOutputStream(_dst);
        try {
            String extension = FilenameUtils.getExtension(_dst.getName());
            ImageWriter writer = (ImageWriter) ImageIO.getImageWritersByFormatName(extension).next();
            synchronized (writer) {
                try {
                    writer.setOutput(ios);
                    ImageWriteParam iwp = writer.getDefaultWriteParam();
                    if (iwp.canWriteCompressed()) {
                        try {
                            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
                            String[] types = iwp.getCompressionTypes();
                            if ((types != null) && (types.length > 0)) {
                                iwp.setCompressionType(types[0]);
                            }
                            iwp.setCompressionQuality(_quality);
                        } catch (Exception ignored) {
                        }
                    }
                    writer.write(null, new IIOImage(dstImage, null, null), iwp);
                    ios.flush();
                } finally {
                    writer.dispose();
                }
            }
        } finally {
            ios.close();
        }

    }
}
