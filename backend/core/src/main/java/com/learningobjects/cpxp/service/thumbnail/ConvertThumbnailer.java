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

import org.apache.commons.exec.*;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thumbnailer that shells out to the native convert executable.
 */
public class ConvertThumbnailer extends Thumbnailer {
    private static final Logger logger = Logger.getLogger(ConvertThumbnailer.class.getName());
    private static final long CONVERT_TIMEOUT_MS = 30 * 1000L;

    private static final ProcessDestroyer __processDestroyer = new ShutdownHookProcessDestroyer();

    public void thumbnail() throws Exception {

        Executor exec = new DefaultExecutor();
        exec.setProcessDestroyer(__processDestroyer);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(CONVERT_TIMEOUT_MS);
        exec.setWatchdog(watchdog);
        exec.setWorkingDirectory(_dst.getParentFile());
        CommandLine convert = new CommandLine("convert");
        if (_cropWidth > 0) {
            convert.addArgument("-crop");
            convert.addArgument(_cropWidth + "x" + _cropHeight + "+" + _cropX + "+" + _cropY);
            convert.addArgument("+repage");
        }
        if (_thumbnail) {
            // thumbnail means strip metadata, do a crude rescale down to 5x the target size and
            // then a full quality resize down to the target size.
            convert.addArguments("-thumbnail " + _width + "x" + _height + (_forceSize ? "!" : ""));
        } else {
            convert.addArguments("-resize " + _width + "x" + _height + (_forceSize ? "!" : "")); // -liquid-rescale is great but awful
        }
        convert.addArguments("-quality " + (int) (100 * _quality));
        convert.addArgument("-");
        convert.addArgument(_dst.getPath());

        logger.log(Level.INFO, "Execute, {0}", convert);
        int exitValue;
        InputStream in = _src.openInputStream();
        try {
            exec.setStreamHandler(new PumpStreamHandler(System.out, System.err, in));
            exitValue = exec.execute(convert);
        } finally {
            in.close();
        }

        logger.log(Level.FINE, "Exit value, {0}", exitValue);
        if (exec.isFailure(exitValue)) {
            if (watchdog.killedProcess()) {
                throw new RuntimeException("Convert timed out: " + convert + " -> " + exitValue);
            } else {
                throw new RuntimeException("Convert failed: " + convert + " -> " + exitValue);
            }
        }

    }
}
