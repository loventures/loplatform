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

import angular from 'angular';
import axios from 'axios';
import { useTranslation } from '../../i18n/translationContext';
import React, { useEffect, useState } from 'react';
import { react2angular } from 'react2angular';
import { Alert } from 'reactstrap';

type PreviewerFC = React.FC<{
  name: string;
  viewUrl?: string;
  getSignedUrl?: string;
}>;

const UnsupportedPreviewer: PreviewerFC = () => {
  const translate = useTranslation();
  return (
    <Alert
      color="dark"
      className="mb-0"
    >
      {translate('ASSIGNMENT_PREVIEW_UNSUPPORTED')}
    </Alert>
  );
};

const OfficePreviewer: PreviewerFC = ({ name, viewUrl, getSignedUrl }) => {
  const translate = useTranslation();
  const [actualViewUrl, setActualViewUrl] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (getSignedUrl) {
      axios
        .get<string>(getSignedUrl)
        .then(({ data: signedUrl }) => {
          const officeUrl =
            'https://view.officeapps.live.com/op/embed.aspx?src=' + encodeURIComponent(signedUrl);
          setActualViewUrl(officeUrl);
        })
        .catch(e => {
          console.warn(e); // happens if no S3 so no direct URL
          setActualViewUrl('');
        });
    } else {
      setActualViewUrl('');
    }
  }, [viewUrl, getSignedUrl]);

  return actualViewUrl == null ? null : !actualViewUrl ? (
    <Alert
      color="dark"
      className="mb-0"
    >
      {translate('ASSIGNMENT_PREVIEW_NOT_YET')}
    </Alert>
  ) : (
    <iframe
      title={name}
      className="preview-document"
      src={actualViewUrl}
      allowFullScreen={true}
    ></iframe>
  );
};

const DocumentPreviewer =
  (mimeType: string): PreviewerFC =>
  ({ viewUrl }) => {
    return (
      <embed
        className="preview-document"
        src={viewUrl}
        type={mimeType}
      />
    );
  };

const ImagePreviewer: PreviewerFC = ({ name, viewUrl }) => {
  return (
    <img
      className="preview-image"
      src={viewUrl}
      alt={name}
    />
  );
};

const AudioPreviewer =
  (mimeType?: string): PreviewerFC =>
  ({ viewUrl }) => {
    return (
      // eslint-disable-next-line jsx-a11y/media-has-caption
      <audio
        controls
        className="w-100"
      >
        <source
          src={viewUrl}
          type={mimeType}
        />
      </audio>
    );
  };

const VideoPreviewer =
  (mimeType?: string): PreviewerFC =>
  ({ viewUrl }) => {
    return (
      // eslint-disable-next-line jsx-a11y/media-has-caption
      <video
        controls
        className="w-100 preview-video"
      >
        <source
          src={viewUrl}
          {...(mimeType ? { type: mimeType } : {})}
        />
      </video>
    );
  };

const previewers: Record<string, PreviewerFC> = {
  docx: OfficePreviewer,
  pptx: OfficePreviewer,
  xlsx: OfficePreviewer,
  pdf: DocumentPreviewer('application/pdf'),
  txt: DocumentPreviewer('text/plain'),
  gif: ImagePreviewer,
  jpg: ImagePreviewer,
  jpeg: ImagePreviewer,
  png: ImagePreviewer,
  webp: ImagePreviewer,
  mp3: AudioPreviewer('audio/mpeg'),
  m4a: AudioPreviewer(), // specifying the mime type makes it unplayable
  ogg: AudioPreviewer('audio/ogg'),
  wav: AudioPreviewer('audio/wav'),
  mp4: VideoPreviewer('video/mp4'),
  m4v: VideoPreviewer(),
  webm: VideoPreviewer('video/webm'),
  /* 
    At the time of writing, Chrome refused to play .mov files with certain encodings when video/quicktime was specified.
    Omitting mimetype forces the browser to download and inspect the file to determine which codec to use. See LOVE-168.
  */
  mov: VideoPreviewer(),
  wmv: VideoPreviewer('video/x-ms-wmv'),
};

const Previewer: PreviewerFC = ({ name, viewUrl, getSignedUrl }) => {
  const fileType = name.replace(/.*\./, '').toLowerCase();
  const Component = previewers[fileType] ?? UnsupportedPreviewer;
  return (
    <Component
      name={name}
      viewUrl={viewUrl}
      getSignedUrl={getSignedUrl}
    />
  );
};

export const ng = angular
  .module('lo.directives.previewer', [])
  .component('previewer', react2angular(Previewer, ['name', 'viewUrl', 'getSignedUrl'], []));

export default Previewer;
