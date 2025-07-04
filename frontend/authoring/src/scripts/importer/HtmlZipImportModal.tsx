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

import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import { useDcmSelector, usePolyglot } from '../hooks';
import useBranchId from '../hooks/useBranchId';
import { closeModal } from '../modals/modalActions';
import { TOAST_TYPES, openToast } from '../toast/actions';
import {
  performHtmlZipImport,
  uploadToCampusPack,
  validateHtmlZipImport,
} from './htmlTransferService';
import { importExtensions } from './importConstants';
import ImportModal from './ImportModal';

const HtmlZipImportModal: React.FC = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const name = useDcmSelector(state => state.modal.data?.name ?? state.assetEditor.assetNode.name);
  const dropzoneFile = useDcmSelector(state => state.modal.data.file) as File | undefined;

  const [file, setFile] = useState(dropzoneFile);
  const [pristine, setPristine] = useState(true);

  const [{ errors, preview, warnings }, setModalData] = useState({
    errors: [],
    preview: '',
    warnings: [],
  });
  const [loading, setLoading] = useState(false);
  const [upload, setUpload] = useState(undefined);

  const finalizeImport = () => {
    setLoading(true);
    performHtmlZipImport(branchId, name, upload)
      .then(count => {
        dispatch(closeModal());
        dispatch(
          openToast(
            polyglot.t('IMPORT_HTML_ZIP.HTML_CHANGED', { smart_count: count }),
            TOAST_TYPES.SUCCESS
          )
        );
      })
      .catch(() =>
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.UNEXPECTED_ERROR')],
          preview: '',
          warnings: [],
        })
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!file) {
      if (!pristine) {
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.NO_FILE_UPLOADED')],
          preview: '',
          warnings: [],
        });
        setLoading(false);
      }
      return;
    }

    const fileExtension = `.${file.name.split('.').pop()}`;

    if (!importExtensions.zip.includes(fileExtension)) {
      setModalData({
        errors: [
          polyglot.t('IMPORT_MODAL.BAD_FILE_TYPE', {
            fileType: fileExtension,
            validTypes: importExtensions.zip.join(' or '),
          }),
        ],
        preview: '',
        warnings: [],
      });
    }

    setLoading(true);
    setPristine(false);
    uploadToCampusPack(file)
      .then(guid =>
        validateHtmlZipImport(branchId, name, guid).then(results => {
          setUpload(guid);
          if (!results.length) {
            setModalData({
              errors: [polyglot.t('IMPORT_HTML_ZIP.NO_HTML_CHANGES')],
              preview: '',
              warnings: [],
            });
          } else {
            setModalData({
              errors: [],
              preview: polyglot.t('IMPORT_HTML_ZIP.HTML_WILL_CHANGE', {
                changes: results.join(', '),
              }),
              warnings: [],
            });
          }
        })
      )
      .catch(() =>
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.UNEXPECTED_ERROR')],
          preview: '',
          warnings: [],
        })
      )
      .finally(() => setLoading(false));
  }, [file]);

  return (
    <ImportModal
      validFileTypes={importExtensions.zip}
      modalTitle="IMPORT_HTML_ZIP.MODAL_TITLE"
      finalizeImport={finalizeImport}
      errors={errors}
      preview={preview}
      warnings={warnings}
      loading={loading}
      file={file}
      setFile={setFile}
    />
  );
};

export default HtmlZipImportModal;
