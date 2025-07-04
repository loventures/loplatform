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

package com.learningobjects.cpxp.service.transcoder;

import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LameSpxToMp3Transcoder  implements AudioTranscoder {
    private static final Logger logger = Logger.getLogger(LameSpxToMp3Transcoder.class.getName());
    private static final long CONVERT_TIMEOUT_MS = 30 * 1000L;

    private static final ProcessDestroyer __processDestroyer = new ShutdownHookProcessDestroyer();

    //command requires OGG123 bins and LAME bin to be installed
    private String buildCommand(String srcFilePath, String destFilePath) {

        StringBuffer buf = new StringBuffer();
        buf.append("ogg123 ");
        buf.append("-d wav ");
        buf.append("-f - ");
        buf.append(srcFilePath);
        buf.append(" | ");
        buf.append("lame - ");
        buf.append(destFilePath);
        return buf.toString();
    }

    public void transcode(InputStream srcStream, String destFilePath) throws AudioTranscoderException {
        File tmpf = null;
        try {
            tmpf = File.createTempFile("media", ".spx");
            tmpf.deleteOnExit();
            FileUtils.copyInputStreamToFile(srcStream, tmpf); // ogg123 cannot process a spx file from stdin
            Executor exec = new DefaultExecutor();
            exec.setProcessDestroyer(__processDestroyer);
            exec.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            ExecuteWatchdog watchdog = new ExecuteWatchdog(CONVERT_TIMEOUT_MS);
            exec.setWatchdog(watchdog);
            CommandLine shell = new CommandLine("sh");
            shell.addArgument("-c");
            String command = buildCommand(tmpf.getPath(), destFilePath);
            shell.addArgument(command, false);
            exec.execute(shell);
        } catch (Exception e) {
            logger.log(Level.WARNING, "There was an error with the transcoding", e);
            throw new AudioTranscoderException ("There was an error with the transcoding",e);
        } finally {
            if (tmpf != null) {
                tmpf.delete();
            }
        }
    }

}
