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

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LameWavToMp3Transcoder  implements AudioTranscoder {
    private static final Logger logger = Logger.getLogger(LameWavToMp3Transcoder.class.getName());
    private static final long CONVERT_TIMEOUT_MS = 30 * 1000L;

    private static final ProcessDestroyer __processDestroyer = new ShutdownHookProcessDestroyer();

    //command requires OGG123 bins and LAME bin to be installed
    private String buildCommand(String destFilePath) {

        StringBuffer buf = new StringBuffer();
        buf.append("lame - ");
        buf.append(destFilePath);
        return buf.toString();
    }

    public void transcode(InputStream srcStream, String destFilePath) throws AudioTranscoderException {
        try {
            Executor exec = new DefaultExecutor();
            exec.setProcessDestroyer(__processDestroyer);
            exec.setStreamHandler(new PumpStreamHandler(System.out, System.err,
                    srcStream));
            ExecuteWatchdog watchdog = new ExecuteWatchdog(CONVERT_TIMEOUT_MS);
            exec.setWatchdog(watchdog);
            CommandLine shell = new CommandLine("sh");
            shell.addArgument("-c");
            shell.addArgument(buildCommand(destFilePath), false);
            exec.execute(shell);
        } catch (ExecuteException e) {
            logger.log(Level.WARNING, "audio cannot be transcoded due to system run time error", e);
            throw new AudioTranscoderException ("audio cannot be transcoded due to system run time error", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "audio cannot be transcoded due to IO error", e);
            throw new AudioTranscoderException ("audio cannot be transcoded due to IO error", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "There was an error with the transcoding", e);
            throw new AudioTranscoderException ("There was an error with the transcoding",e);
        }
    }

}
