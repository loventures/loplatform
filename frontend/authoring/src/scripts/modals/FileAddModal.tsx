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

import * as React from 'react';
import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Input, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../hooks';
import AuthoringApiService from '../services/AuthoringApiService';
import AuthoringOpsService from '../services/AuthoringOpsService';
import { DcmState } from '../types/dcmState';
import { FullEdge } from '../types/edge';

type fileOrImage = 'file.1' | 'image.1';

interface FileAddModalData {
  typeId: fileOrImage;
  sourceName: string;
  existingEdges: string[];
  callback: (edge: FullEdge) => void;
  failureCallback: () => void;
}

interface PartialFileAsset {
  typeId: fileOrImage;
  data: {
    title: string;
    caption?: string;
    altText?: string;
  };
}

const FileAddModal = () => {
  const { modalOpen, toggleModal } = useModal();
  const polyglot = usePolyglot();
  const modalData: FileAddModalData = useSelector((state: DcmState) => state.modal.data);
  const { typeId, sourceName, existingEdges, callback, failureCallback } = modalData;

  const [file, setFile] = useState(null);
  const [fileForm, setFileForm] = useState({
    title: '',
    caption: '',
    altText: '',
  });

  const setFileTitle = file => {
    setFile(file);
    if (!fileForm.title) {
      setFileForm({
        ...fileForm,
        title: file.name,
      });
    }
  };

  const close = () => {
    toggleModal();
    failureCallback?.();
  };

  const submit = () => {
    const asset: PartialFileAsset = {
      typeId,
      data: {
        title: fileForm.title,
      },
    };

    if (typeId === 'image.1') {
      asset.data.caption = fileForm.caption;
      asset.data.altText = fileForm.altText;
    }

    AuthoringApiService.uploadToBlobstore(asset, file)
      .then(blobData => {
        const newAssetData = { ...asset, data: { ...asset.data, source: blobData } };
        const addNode = AuthoringOpsService.addNodeOp(newAssetData);
        console.log(newAssetData, addNode.data);
        const addEdge = AuthoringOpsService.addEdgeOp(sourceName, addNode.name, 'resources');
        const setEdgeOrder = AuthoringOpsService.setEdgeOrderOp(sourceName, 'resources', [
          ...existingEdges,
          addEdge.name,
        ]);
        return AuthoringOpsService.postWriteOps([addNode, addEdge, setEdgeOrder]).then(
          ({ edges, newEdges }) => {
            callback(edges[newEdges[addEdge.name]]);
            toggleModal();
          }
        );
      })
      .catch(e => {
        console.error(e);
        failureCallback?.();
      });
  };

  return (
    <Modal isOpen={modalOpen}>
      <ModalHeader tag="h3">
        {polyglot.t('FILE_ADD.HEADER_TITLE', {
          fileType: polyglot.t(typeId === 'image.1' ? 'IMAGE' : 'FILE'),
        })}
      </ModalHeader>
      <ModalBody>
        <div className="control-group">
          <label className="control-label">{polyglot.t('TITLE')}</label>
          <div className="controls">
            <input
              type="text"
              className="form-control"
              placeholder={polyglot.t('TITLE')}
              onChange={evt =>
                setFileForm({
                  ...fileForm,
                  title: evt.target.value,
                })
              }
            />
          </div>
          <label className="control-label my-2">{polyglot.t('FILE')}</label>
          <div className="controls">
            <Input
              type="file"
              accept={typeId === 'image.1' ? '.png, .jpg, .gif, .jpeg' : ''}
              onChange={evt => setFileTitle(evt.target.files[0])}
              id="customFile"
              title={polyglot.t('FILE_ADD.CHOOSE_FILE')}
            />
          </div>
          {typeId === 'image.1' && (
            <React.Fragment>
              <label className="control-label my-2">{polyglot.t('CAPTION')}</label>
              <div className="controls">
                <input
                  type="text"
                  className="form-control"
                  placeholder={polyglot.t('CAPTION')}
                  onChange={evt =>
                    setFileForm({
                      ...fileForm,
                      caption: evt.target.value,
                    })
                  }
                />
              </div>
              <label className="control-label my-2">{polyglot.t('ALT_TEXT')}</label>
              <div className="controls">
                <input
                  type="text"
                  className="form-control"
                  placeholder={polyglot.t('ALT_TEXT')}
                  onChange={evt =>
                    setFileForm({
                      ...fileForm,
                      altText: evt.target.value,
                    })
                  }
                />
              </div>
            </React.Fragment>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <button
          className="btn btn-secondary"
          onClick={close}
        >
          {polyglot.t('CLOSE')}
        </button>
        <button
          className="btn btn-primary"
          onClick={submit}
          disabled={!file || !fileForm.title}
        >
          {polyglot.t('SUBMIT')}
        </button>
      </ModalFooter>
    </Modal>
  );
};

export default FileAddModal;
