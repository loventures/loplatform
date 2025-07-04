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
import { useDispatch, useSelector } from 'react-redux';
import { Button, Input, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../hooks';
import { TOAST_TYPES, openToast } from '../toast/actions';
import { DcmState } from '../types/dcmState';

/*
{
  "title": "eTextbook: Principles of Instrument Management: A Balanced Approach",
  "isbn": "12345",
  "workspaceId": "83c2854b-97eb-4375-84a0-746a5ddd1410",
  "checkpointId": "96f8ba44-91cc-4103-9f4c-abc0661baf54",
  "navigationId": "MYQMW6VGRY42MZASZ113",
  "status": {
    "code": "OK"
  },
  "children": [
    {
      "title": "Cover Page",
      "cgi": "cover-page",
      "depth": 1,
      "nodeType": "nav-point"
    },
    {
      "title": "Part 1. Instrument Management: An Overview",
      "cgi": "QVULK8LB7B55RXVJW754",
      "depth": 1,
      "nodeType": "part",
      "children": [
        {
          "title": "French Horn",
          "cgi": "12345",
          "depth": 2,
          "nodeType": "chapter"
        },
        {
          "title": "Chapter 2. Purchasing Management",
          "cgi": "ZKJBNCXCLRU9ZM8J5335",
          "depth": 2,
          "nodeType": "chapter"
        }
      ]
    }
  ]
}
 */

type EBookNode = {
  title: string;
  cgi?: string;
  depth: number;
  children?: EBookNode[];
};

type EBook = {
  title: string;
  isbn: string;
  children: EBookNode[];
};

const EBookJson = 'eBookJson';

// Were it a thing, there could be a dropdown where you pick the LTI tool
// rather than it being hardcoded to the eReader.
const EBookEmbedModal = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const { callback, cancelback } = useSelector((state: DcmState) => state.modal.data);
  const { modalOpen, toggleModal } = useModal();
  const [initialJson] = useState(() => sessionStorage.getItem(EBookJson) ?? '');
  const [jsonText, setJsonText] = useState(initialJson);
  const [eBook, setEBook] = useState<EBook>();
  const [error, setError] = useState(false);
  const [selected, setSelected] = useState<EBookNode>();
  const updateJsonText = (jsonText: string) => {
    sessionStorage.setItem(EBookJson, jsonText);
    setJsonText(jsonText);
  };
  const cancelModal = () => {
    toggleModal();
    cancelback?.();
  };

  const submit = () => {
    if (eBook == null) {
      try {
        const json = JSON.parse(jsonText) as any;
        if (typeof json.isbn !== 'string' || !Array.isArray(json.children))
          throw Error('Invalid JSON');
        setEBook(json);
      } catch (e) {
        setError(true);
      }
    } else if (selected) {
      const ltiUrl = `lti://eReader?ereader_isbn=${eBook.isbn}&node_id=${selected.cgi}`;
      if (callback) {
        toggleModal();
        callback(selected.title, ltiUrl, true);
      } else {
        /* Should be this but Chrome headless...
        navigator.clipboard.writeText(ltiUrl).then(() => {
          dispatch(openToast(polyglot.t('EBOOK_MODAL_LINK_COPIED'), TOAST_TYPES.SUCCESS))
          toggleModal();
        }, () => {
          setError(true);
        }); */
        try {
          const inputField = document.createElement('input');
          inputField.value = ltiUrl;
          document.body.appendChild(inputField);
          inputField.focus();
          inputField.select();
          const result = document.execCommand('copy');
          inputField.remove();
          if (!result) throw Error('Copy failed');
          dispatch(openToast(polyglot.t('EBOOK_MODAL_LINK_COPIED'), TOAST_TYPES.SUCCESS));
          toggleModal();
        } catch (e) {
          console.log(e);
          setError(true);
        }
      }
    }
  };

  const canSubmit = !error && (eBook == null ? jsonText !== '' : selected != null);

  const renderNodes = (children?: EBookNode[]) =>
    children?.length ? (
      <ol style={{ listStyle: 'none' }}>
        {children.map((node, index) => (
          <li key={index}>
            <label
              style={{
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                display: 'block',
              }}
            >
              <input
                type="radio"
                name="ebook-cgi"
                disabled={!node.cgi}
                className="me-2"
                onChange={() => setSelected(node)}
              />
              {node.title}
            </label>
            {renderNodes(node.children)}
          </li>
        ))}
      </ol>
    ) : null;

  return (
    <Modal
      id="ebook-modal"
      isOpen={modalOpen}
      toggle={cancelModal}
      size="lg"
      className="no-exit-edit"
    >
      <ModalHeader>{polyglot.t('EBOOK_MODAL_TITLE')}</ModalHeader>
      <ModalBody>
        {error ? (
          <p id="ebook-error">
            {polyglot.t(eBook == null ? 'EBOOK_MODAL_ERROR' : 'EBOOK_MODAL_COPY_ERROR')}
          </p>
        ) : eBook == null ? (
          <>
            <p>{polyglot.t('EBOOK_MODAL_JSON_INSTRUCTIONS')}</p>
            <div className="row">
              <div className="col-md-12">
                <Input
                  type="textarea"
                  id="ebook-json-input"
                  className="form-control"
                  autoFocus={initialJson === ''}
                  rows={12}
                  defaultValue={jsonText}
                  onChange={evt => updateJsonText(evt.target.value)}
                  placeholder='{"title":"eTextbook: Principles of Instrument Management: A Balanced...'
                />
              </div>
            </div>
          </>
        ) : (
          <div
            id="ebook-link-checkboxes"
            style={{ overflow: 'auto', height: '20rem' }}
          >
            <p>{polyglot.t('EBOOK_MODAL_LINK_INSTRUCTIONS')}</p>
            {renderNodes(eBook.children)}
          </div>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          id="modal-cancel"
          color="secondary"
          onClick={cancelModal}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          id="modal-submit"
          color="primary"
          onClick={submit}
          disabled={!canSubmit}
          autoFocus={initialJson !== ''}
        >
          {polyglot.t(
            eBook == null ? 'NEXT' : callback ? 'EBOOK_MODAL_INSERT' : 'EBOOK_MODAL_COPY_URL'
          )}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default EBookEmbedModal;
