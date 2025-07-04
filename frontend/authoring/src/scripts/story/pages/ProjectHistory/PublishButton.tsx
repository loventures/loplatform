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

import { isEqual } from 'lodash';
import gretchen from '../../../grfetchen/';
import React, { useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Spinner } from 'reactstrap';

import { trackAuthoringEvent } from '../../../analytics';
import {
  confirmSaveProjectGraphEdits,
  saveProjectGraphEdits,
  useEditedAssetDatum,
} from '../../../graphEdit';
import { useDcmSelector } from '../../../hooks';
import { ProjectOffering } from '../../../revision/revision';
import { openToast, TOAST_TYPES } from '../../../toast/actions';
import { Thunk } from '../../../types/dcmState';
import { editProjectStatusAction } from '../../ActionMenu/actions';
import { useProjectAccess } from '../../hooks';
import { getOfferingConfig, getProjectConfig } from './projectApi';
import { PublishAnalysis } from './PublishAnalysis';
import UpdateVersionModal from './UpdateVersionModal';

const publishProject = (projectId: number) =>
  gretchen.post('/api/v2/authoring/projects/:projectId/publish').params({ projectId }).exec();

const publishUpdate = (projectId: number) =>
  gretchen.post('/api/v2/authoring/projects/:projectId/update').params({ projectId }).exec();

const getPublishAnalysis = (projectId: number): Promise<PublishAnalysis> =>
  gretchen
    .get('/api/v2/authoring/projects/:projectId/publishAnalysis')
    .params({ projectId })
    .exec()
    .then(
      ({ numStaleSections, creates, updates, deletes }) =>
        new PublishAnalysis(numStaleSections, creates, updates, deletes)
    );

export const PublishButton: React.FC<{
  offering: ProjectOffering | undefined;
  commit: number | undefined;
  doReload: () => void;
}> = ({ offering, commit, doReload }) => {
  const dispatch = useDispatch();

  const rights = useDcmSelector(state => state.user.rights);
  const projectAccess = useProjectAccess();
  const project = useDcmSelector(state => state.layout.project);
  const [publishAnalysis, setPublishAnalysis] = useState<PublishAnalysis | null>(null);
  const canPublish =
    rights?.includes('loi.authoring.security.right$PublishOfferingRight') &&
    projectAccess.PublishProject;
  const [publishing, setPublishing] = useState(false);

  const [staleConfig, setStaleConfig] = useState(false);
  useEffect(() => {
    if (offering) {
      Promise.all([getProjectConfig(project.id), getOfferingConfig(offering.id)]).then(
        ([{ overrides: projectCf }, { overrides: offeringCf }]) => {
          setStaleConfig(!isEqual(projectCf, offeringCf));
        }
      );
    }
  }, [project, offering]);

  const projectStatus = useEditedAssetDatum(project.rootNodeName, a => a.projectStatus);

  const needsUpdate = offering?.commitId !== commit || staleConfig;

  const onUpdate = useCallback(
    (updateStatus: boolean) => {
      setPublishing(true);
      trackAuthoringEvent('Narrative Editor - Publish Update');
      const doUpdate: Thunk = dispatch => {
        publishUpdate(project.id)
          .then(() => {
            dispatch(openToast(`Published update.`, TOAST_TYPES.SUCCESS));
            doReload();
          })
          .catch(e => {
            console.warn(e);
            dispatch(openToast('A publishing error occurred.', TOAST_TYPES.DANGER));
          })
          .finally(() => {
            setPublishAnalysis(null);
            setPublishing(false);
          });
      };
      if (updateStatus) {
        dispatch(editProjectStatusAction('Live', null, false));
        dispatch(saveProjectGraphEdits(doUpdate, { unsafe: true }));
      } else {
        dispatch(doUpdate);
      }
    },
    [project, doReload]
  );

  const onPublish = useCallback(
    (updateStatus: boolean) => {
      setPublishing(true);
      trackAuthoringEvent('Narrative Editor - Publish Project');
      const doPublish = dispatch => {
        return publishProject(project.id)
          .then(() => {
            dispatch(openToast(`Published project.`, TOAST_TYPES.SUCCESS));
            doReload();
          })
          .catch(e => {
            console.warn(e);
            dispatch(openToast('A publishing error occurred.', TOAST_TYPES.DANGER));
          })
          .finally(() => {
            setPublishAnalysis(null);
            setPublishing(false);
          });
      };
      if (updateStatus) {
        dispatch(editProjectStatusAction('Live', null, false));
        dispatch(saveProjectGraphEdits(doPublish, { unsafe: true }));
      } else {
        dispatch(doPublish);
      }
    },
    [project, doReload]
  );

  const onClick = useCallback(() => {
    dispatch(
      confirmSaveProjectGraphEdits(() => {
        if (offering == null) {
          setPublishAnalysis(new PublishAnalysis());
        } else {
          setPublishing(true);
          getPublishAnalysis(project.id)
            .then(analysis => {
              setPublishAnalysis(analysis); // opens the modal!
            })
            .catch(e => {
              console.warn(e);
              dispatch(openToast('A publishing error occurred.', TOAST_TYPES.DANGER));
            })
            .finally(() => setPublishing(false));
        }
      })
    );
  }, [project, offering]);

  return canPublish ? (
    <>
      <div className="d-flex justify-content-center">
        <Button
          id="publish-button"
          color="primary"
          className="mt-4"
          onClick={onClick}
          disabled={publishing || !needsUpdate || publishAnalysis != null}
        >
          {offering ? 'Publish Update' : 'Publish Project'}
          {publishing && (
            <Spinner
              size="sm"
              className="ms-2"
            />
          )}
        </Button>
      </div>

      <UpdateVersionModal
        isOpen={publishAnalysis != null}
        mode={offering == null ? 'publish' : 'update'}
        publishing={publishing}
        publishAnalysis={publishAnalysis}
        isLive={projectStatus === 'Live'}
        closeModal={(publish, updateStatus) => {
          if (publish) {
            if (offering == null) {
              onPublish(updateStatus);
            } else {
              onUpdate(updateStatus);
            }
          } else {
            setPublishAnalysis(null);
          }
        }}
      />
    </>
  ) : null;
};
