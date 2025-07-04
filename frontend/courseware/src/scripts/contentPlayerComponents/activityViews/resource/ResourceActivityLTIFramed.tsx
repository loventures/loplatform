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

import classnames from 'classnames';
import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle.tsx';
import TooltippedGradeBadge from '../../../components/TooltippedGradeBadge.tsx';
import { useAssetInfo } from '../../../resources/AssetInfo.ts';
import { useCourseSelector } from '../../../loRedux';
import { ActivityProps } from '../ActivityProps.ts';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import ContentBlockInstructions from '../../parts/ContentBlockInstructions';
import { reportProgressActionCreator } from '../../../courseActivityModule/actions/activityActions.ts';
import { selectFullscreenState } from '../../../courseContentModule/selectors/contentLandmarkSelectors.ts';
import { getLTIConfigUrl } from '../../../lti/configUrl';
import { CONTENT_TYPE_LTI } from '../../../utilities/contentTypes.ts';
import { selectCurrentUser } from '../../../utilities/rootSelectors.ts';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';

const onIFrameLoadedEffect = (iframe: HTMLIFrameElement, onLoaded: () => void) => {
  try {
    const doc = iframe.contentWindow?.document;
    if (doc?.readyState === 'complete') {
      onLoaded();
    } else if (doc != null) {
      // not clear this ever happens for an iframe...
      doc.addEventListener('load', onLoaded); // DOMContentLoaded fires when just the DOM are ready
      return () => doc.removeEventListener('load', onLoaded);
    }
  } catch (e) {
    // plausibly security error
    console.log('On loaded error', e);
    onLoaded();
  }
};

const LTIFramedLauncher: React.FC<{ launchPath: string; onLoaded: () => void }> = ({
  launchPath,
  onLoaded,
}) => {
  const frame = useRef<HTMLIFrameElement>(null);
  useEffect(
    () => (frame.current ? onIFrameLoadedEffect(frame.current, onLoaded) : undefined),
    [frame.current, onLoaded]
  );
  return (
    <div className="iframe-container">
      <iframe
        title="LTI Launch"
        className="resource-iframe"
        frameBorder="0"
        allowFullScreen={true}
        src={launchPath}
        ref={frame}
      />
    </div>
  );
};

const ResourceActivityLTIFramed: React.FC<ActivityProps<CONTENT_TYPE_LTI>> = ({
  content,
  printView,
}) => {
  const dispatch = useDispatch();
  const fullscreen = useCourseSelector(selectFullscreenState);
  // onLoaded is called repeatedly so we maintain state to only dispatch progress once.
  const [reported, setReported] = useState<string | undefined>(undefined);
  const recordProgress = useCallback(() => {
    if (reported !== content.id && !printView && !content.hasGradebookEntry) {
      setReported(content.id);
      dispatch(reportProgressActionCreator(content, true));
    }
  }, [dispatch, content.id, reported, setReported, printView]);
  const assetInfo = useAssetInfo(content);
  const user = useCourseSelector(selectCurrentUser);
  return (
    <div className="card lti-activity er-content-wrapper">
      <div className="card-body">
        <div className={classnames('embedded-content', fullscreen && 'full-screen')}>
          <ERContentTitle
            content={content}
            printView={printView}
          />
          <TooltippedGradeBadge />
          {assetInfo.instructions.renderedHtml ? (
            <ContentBlockInstructions
              instructions={assetInfo.instructions}
              className="mb-4"
            />
          ) : null}
          <div className="full-screen-container">
            <LTIFramedLauncher
              launchPath={getLTIConfigUrl(content, user)}
              onLoaded={recordProgress}
            />
          </div>
          <ActivityCompetencies content={content} />
        </div>
      </div>
    </div>
  );
};

export default ResourceActivityLTIFramed;
