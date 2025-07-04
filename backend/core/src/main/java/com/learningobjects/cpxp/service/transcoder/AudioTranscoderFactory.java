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

public class AudioTranscoderFactory {

    public static AudioTranscoder getAudioTranscoderForFormats(AudioFormat srcformat, AudioFormat destformat) throws AudioTranscoderException {
        if ((srcformat == AudioFormat.SPX) && (destformat == AudioFormat.MP3)) {
            return new LameSpxToMp3Transcoder();
        } else if ((srcformat == AudioFormat.WAV) && (destformat == AudioFormat.MP3)) {
            return new LameWavToMp3Transcoder();
        } else if ((srcformat == AudioFormat.SPX) && (destformat == AudioFormat.AAC)) {
            return new LameSpxToAacTranscoder();
        } else {
            throw new AudioTranscoderException("No transcoder defined for converting "+srcformat+" to "+destformat);
        }
    }

    public static AudioFormat[] getCovertableFormats(AudioFormat destformat) {
        if (destformat == AudioFormat.MP3) {
            return new AudioFormat[] {AudioFormat.SPX, AudioFormat.WAV};
        } else if (destformat == AudioFormat.AAC) {
            return new AudioFormat[] {AudioFormat.SPX};
        } else {
            return new AudioFormat[] {};
        }
    }
}
