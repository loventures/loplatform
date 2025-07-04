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

import { isEmpty } from 'lodash';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Button, Input, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import { useDispatch } from 'react-redux';

import { trackImportFile } from '../analytics/AnalyticsEvents';
import { Loadable } from '../authoringUi';
import { usePolyglot } from '../hooks';
import { closeModal } from '../modals/modalActions';

const ImportModal = ({
  errors,
  warnings,
  preview,
  loading,
  finalizeImport,
  validFileTypes,
  modalTitle,
  setFile,
  file,
  replacePrevious = null,
  setReplacePrevious = null,
}) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const [warningsExpanded, setWarningsExpanded] = useState(false);

  const disableImportButton = loading || !isEmpty(errors) || !file || isEmpty(preview);

  const close = () => dispatch(closeModal());
  return (
    <Modal
      isOpen={true}
      toggle={close}
      size="lg"
    >
      <ModalHeader>{polyglot.t(modalTitle)}</ModalHeader>
      <ModalBody
        style={{ overflow: 'auto', maxHeight: 'calc(100vh - 12rem)' }}
        className="mb-last-p-0"
      >
        <div className="mb-3">
          <Input
            type="file"
            accept={validFileTypes.join(',')}
            onChange={e => setFile(e.target.files[0])}
            id="customFile"
            title={polyglot.t('TEMPLATE_IMPORT.CHOOSE_FILE')}
          />
        </div>
        {setReplacePrevious !== null ? (
          <div className="ps-1">
            <input
              id="replacePrevious"
              type="checkbox"
              checked={replacePrevious}
              onChange={event => setReplacePrevious(event.target.checked)}
            />
            <label
              className="ms-1"
              htmlFor="replacePrevious"
            >
              Replace existing competencies
            </label>
          </div>
        ) : null}

        <Loadable loading={loading}>
          {() => (
            <>
              {!isEmpty(warnings) && (
                <>
                  <p className="fw-bold">
                    {warnings.length}{' '}
                    {warnings.length > 1
                      ? polyglot.t('IMPORT_MODAL.MODAL_WARNING-plural')
                      : polyglot.t('IMPORT_MODAL.MODAL_WARNING')}
                    <i
                      className="material-icons center"
                      onClick={() => setWarningsExpanded(!warningsExpanded)}
                      style={{ verticalAlign: 'middle', cursor: 'pointer' }}
                    >
                      {warningsExpanded ? 'keyboard_arrow_down' : 'chevron_right'}
                    </i>
                  </p>
                  {warningsExpanded && warnings.map((line, i) => <p key={i}>{line}</p>)}
                </>
              )}
              {isEmpty(errors) ? (
                typeof preview === 'string' ? (
                  <p>{preview}</p>
                ) : (
                  preview
                )
              ) : (
                <>
                  <p className="fw-bold">
                    {errors.length > 1
                      ? polyglot.t('IMPORT_MODAL.MODAL_ERROR-plural')
                      : polyglot.t('IMPORT_MODAL.MODAL_ERROR')}
                  </p>
                  {errors.map((line, i) => (
                    <p
                      className="text-danger"
                      key={i}
                    >
                      {line}
                    </p>
                  ))}
                </>
              )}
            </>
          )}
        </Loadable>
      </ModalBody>

      <ModalFooter>
        <Button
          color="secondary"
          onClick={close}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          color="primary"
          className="ms-2"
          onClick={() => {
            finalizeImport();
            trackImportFile(file.name);
          }}
          disabled={disableImportButton}
        >
          {polyglot.t('IMPORT')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

ImportModal.propTypes = {
  // from parent
  errors: PropTypes.array,
  warnings: PropTypes.array,
  preview: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  loading: PropTypes.bool,
  modalTitle: PropTypes.string.isRequired,
  validFileTypes: PropTypes.array.isRequired,
  finalizeImport: PropTypes.func.isRequired,
  setFile: PropTypes.func.isRequired,
  file: PropTypes.object,
  replacePrevious: PropTypes.bool,
  setReplacePrevious: PropTypes.func,
};

export default ImportModal;
