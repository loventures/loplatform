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

import classNames from 'classnames';
import React, { useEffect, useState } from 'react';
import { IoTrashOutline } from 'react-icons/io5';
import { MdOutlineOpenInNew } from 'react-icons/md';
import { useDispatch } from 'react-redux';
import { Button, Card, CardBody, Input, FormFeedback, Spinner } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../graphEdit';
import { useBranchId, useDcmSelector } from '../../hooks';
import AuthoringApiService from '../../services/AuthoringApiService';
import { generateScormMetadata } from '../../services/scormUtilities';
import { NarrativeEditor } from '../story';
import { useIsEditable, useRevisionCommit } from '../storyHooks';

export const ScormEditor: NarrativeEditor<'scorm.1'> = ({ asset, readOnly }) => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const commitId = useRevisionCommit();

  const editMode = useIsEditable(asset.name) && !readOnly;
  const [uploading, setUploading] = useState(false);
  const [invalid, setInvalid] = useState(false);
  const scorm = asset.data;

  const launchUrl = commitId
    ? `/api/v2/authoring/${commitId}/scorm.1/${asset.name}/serve/${scorm.resourcePath}`
    : `/api/v2/authoring/branches/${branchId}/scorm.1/${asset.name}/serve/${scorm.resourcePath}`;

  const downloadUrl = commitId
    ? `/api/v2/authoring/${branchId}/commits/${commitId}/nodes/${asset.name}/serve`
    : `/api/v2/authoring/${branchId}/nodes/${asset.name}/serve`;

  const dimensions = scorm.contentWidth
    ? {
        height: '100%',
        aspectRatio: `${scorm.contentWidth} / ${scorm.contentHeight}`,
      }
    : { height: `${scorm.contentHeight}px` };

  const onRemove = () => {
    dispatch(beginProjectGraphEdit('Remove SCORM package'));
    dispatch(
      editProjectGraphNodeData(asset.name, {
        source: null,
        zipPaths: [],
        scormTitle: null,
        resourcePath: '',
        allRefs: {},
        passingScore: null,
        objectiveIds: [],
        sharedDataIds: [],
      })
    );
    dispatch(autoSaveProjectGraphEdits());
  };

  const onUpload = (file: File) => {
    if (!file) return;
    setInvalid(false);
    setUploading(true);
    AuthoringApiService.uploadToBlobstore(asset, file)
      .then(blobData => {
        return generateScormMetadata(file).then(meta => {
          const { paths, title, resourcePath, allRefs, passingScore, objectiveIds, sharedDataIds } =
            meta as any;
          dispatch(beginProjectGraphEdit('Upload SCORM package'));
          dispatch(
            editProjectGraphNodeData(asset.name, {
              source: blobData,
              zipPaths: paths,
              scormTitle: title,
              resourcePath,
              allRefs,
              passingScore,
              objectiveIds,
              sharedDataIds,
            })
          );
          dispatch(autoSaveProjectGraphEdits(() => setUploading(false)));
        });
      })
      .catch(e => {
        console.log(e);
        setInvalid(true);
        setUploading(false);
      });
  };

  const launchInNewWindow = () => {
    const height = scorm.contentHeight || 600;
    const width = scorm.contentWidth || 960;
    const left = (window.screenLeft || window.screenX) + (window.outerWidth - width) / 2;
    const top = (window.screenTop || window.screenY) + (window.outerHeight - height) / 2;
    const features = `popup,height=${height},width=${width},left=${left},top=${top}`;
    window.open(location.origin + launchUrl, `scorm:${asset.name}`, features);
  };

  const user = useDcmSelector(s => s.user);

  // multiple scorms in inline view no bueno
  useEffect(() => {
    const userId = user.profile.emailAddress || user.profile.id.toString();
    const userName = user.profile.fullName;
    const credit = scorm.isForCredit ? 'credit' : 'no-credit';
    const data = {
      'cmi.core.student_id': userId,
      'cmi.learner_id': userId,
      'cmi.core.student_name': userName,
      'cmi.learner_name': userName,
      'cmi._version': '1.0',
      'cmi.launch_data': '',
      'cmi.core.lesson_mode': 'normal', // "browse"???
      'cmi.mode': 'normal',
      'cmi.core.credit': credit,
      'cmi.credit': credit,
      'cmi.completion_threshold': '',
      'cmi.max_time_allowed': '',
      'cmi.time_limit_action': '',
      'cmi.total_time': '',
      'cmi.core.entry': 'ab-initio',
      'cmi.entry': 'ab-initio',
    };
    scorm.objectiveIds?.forEach((oid, i) => {
      data[`cmi.objectives.${i}.id`] = oid;
    });

    // 100% SCORM compliant
    const API = {
      LMSInitialize: function (str: string) {
        void str;
        return 'true';
      },
      LMSFinish: function (str: string) {
        void str;
        return 'true';
      },
      LMSGetValue: function (key: string) {
        return data[key] ?? '';
      },
      LMSSetValue: function (key: string, value: any) {
        data[key] = value;
      },
      LMSCommit: function (str: string) {
        void str;
        return 'true';
      },
      LMSGetLastError: function () {
        return 0;
      },
      LMSGetErrorString: function (errorCode: any) {
        return errorCode ? 'No error string available.' : '';
      },
      LMSGetDiagnostic: function (errorCode: any) {
        return errorCode ? 'No diagnostic information available.' : '';
      },
    };

    const API_1484_11 = {
      Initialize: API.LMSInitialize,
      Terminate: API.LMSFinish,
      GetValue: API.LMSGetValue,
      SetValue: API.LMSSetValue,
      Commit: API.LMSCommit,
      GetLastError: API.LMSGetLastError,
      GetErrorString: API.LMSGetErrorString,
      GetDiagnostic: API.LMSGetDiagnostic,
    };
    (window as any).API = API;
    (window as any).API_1484_11 = API_1484_11;

    return () => {
      delete (window as any).API;
      delete (window as any).API_1484_11;
    };
  }, [scorm]);

  return (
    <>
      <div className="my-4 mx-2">
        {!editMode ? null : scorm.source ? (
          <div className="d-flex align-items-center justify-content-center">
            {uploading ? (
              <span className="input-padding px-0">{scorm.source.filename}</span>
            ) : (
              <a
                className="input-padding px-0"
                href={downloadUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                {scorm.source.filename}
              </a>
            )}
            <Button
              outline
              color="danger"
              className="border-0 d-flex p-2 ms-3"
              title="Remove SCORM Package"
              onClick={() => onRemove()}
            >
              <IoTrashOutline />
            </Button>
          </div>
        ) : (
          <>
            <Input
              id={`scorm-file-${asset.name}`}
              type="file"
              title="Upload SCORM package"
              className="narrative flex-grow-1 scorm-upload"
              disabled={uploading || !editMode}
              onChange={e => onUpload(e.target.files[0])}
              accept="application/zip"
              invalid={invalid}
            />
            {invalid && (
              <FormFeedback className="d-block">
                This SCORM package could not be processed.
              </FormFeedback>
            )}
          </>
        )}
        {scorm.launchNewWindow ? (
          <div className="mt-2">
            <div>
              <a
                href={launchUrl}
                className={classNames(
                  'btn btn-primary btn-block d-flex align-items-center justify-content-center',
                  (uploading || !scorm.source) && 'disabled'
                )}
                onClick={e => {
                  e.preventDefault();
                  if (uploading || !scorm.source) return;
                  launchInNewWindow();
                }}
              >
                {`Launch ${scorm.scormTitle ?? 'SCORM Activity'}`}
                {uploading ? (
                  <Spinner
                    size="sm"
                    className="ms-2"
                  />
                ) : (
                  <MdOutlineOpenInNew className="ms-2" />
                )}
              </a>
            </div>
          </div>
        ) : uploading || !scorm.source ? (
          <Card
            className="mt-2"
            style={dimensions}
          >
            <CardBody className="d-flex align-items-center justify-content-center text-muted">
              {uploading ? 'Uploading...' : 'No SCORM package configured.'}
            </CardBody>
          </Card>
        ) : (
          <>
            <iframe
              className="mt-2 w-100 mx-0"
              style={dimensions}
              src={launchUrl}
            ></iframe>
            <div
              className="mt-1 text-center"
              style={{ fontSize: '.875rem', color: '#916f08' }}
            >
              SCORM support in the authoring environment is limited, use student preview for a full
              implementation.
            </div>
          </>
        )}
      </div>
    </>
  );
};
