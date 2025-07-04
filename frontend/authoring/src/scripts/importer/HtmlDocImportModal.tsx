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

import React, { ReactElement, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import { reloadAssetEditor } from '../editor/assetEditorActions';
import { useDcmSelector, usePolyglot } from '../hooks';
import useBranchId from '../hooks/useBranchId';
import { closeModal } from '../modals/modalActions';
import { TOAST_TYPES, openToast } from '../toast/actions';
import {
  performHtmlDocImport,
  uploadToCampusPack,
  validateHtmlDocImport,
} from './htmlTransferService';
import { importExtensions } from './importConstants';
import ImportModal from './ImportModal';

const HtmlDocImportModal: React.FC = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const name = useDcmSelector(state => state.modal.data?.name ?? state.assetEditor.assetNode.name);
  const dropzoneFile = useDcmSelector(state => state.modal.data.file) as File | undefined;

  const [file, setFile] = useState(dropzoneFile);
  const [pristine, setPristine] = useState(true);

  const [{ errors, preview, warnings }, setModalData] = useState<{
    errors: string[];
    warnings: string[];
    preview?: ReactElement;
  }>({
    errors: [],
    warnings: [],
  });
  const [loading, setLoading] = useState(false);
  const [upload, setUpload] = useState(undefined);

  const finalizeImport = () => {
    setLoading(true);
    performHtmlDocImport(branchId, name, upload)
      .then(() => {
        dispatch(reloadAssetEditor());
        dispatch(closeModal());
        dispatch(openToast(polyglot.t('IMPORT_HTML_DOC.HTML_CHANGED'), TOAST_TYPES.SUCCESS));
      })
      .catch(() =>
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.UNEXPECTED_ERROR')],
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
          warnings: [],
        });
        setLoading(false);
      }
      return;
    }

    const fileExtension = `.${file.name.split('.').pop()}`;

    if (!importExtensions.html.includes(fileExtension)) {
      setModalData({
        errors: [
          polyglot.t('IMPORT_MODAL.BAD_FILE_TYPE', {
            fileType: fileExtension,
            validTypes: importExtensions.html.join(' or '),
          }),
        ],
        warnings: [],
      });
    }

    setLoading(true);
    setPristine(false);
    uploadToCampusPack(file)
      .then(guid =>
        validateHtmlDocImport(branchId, name, guid).then(
          ({ warnings, errors, added, modified, alignmentsAdded, alignmentsRemoved }) => {
            if (errors) {
              setModalData({
                errors: errors,
                warnings: [],
              });
            } else if (
              !added?.length &&
              !modified?.length &&
              !alignmentsAdded &&
              !alignmentsRemoved
            ) {
              setModalData({
                errors: [polyglot.t('IMPORT_HTML_DOC.NO_CHANGES')],
                warnings: [],
              });
            } else {
              setUpload(guid);
              setModalData({
                errors: [],
                preview: (
                  <ul>
                    {added?.map((name, i) => (
                      <li key={`add-${i}`}>
                        {polyglot.t('IMPORT_HTML_DOC.ADD_ELEMENT', { name })}
                      </li>
                    ))}
                    {modified?.map((name, i) => (
                      <li key={`mod-${i}`}>
                        {polyglot.t('IMPORT_HTML_DOC.MODIFY_ELEMENT', { name })}
                      </li>
                    ))}
                    {!!alignmentsAdded && (
                      <li>
                        {polyglot.t('IMPORT_HTML_DOC.ALIGN', { smart_count: alignmentsAdded })}
                      </li>
                    )}
                    {!!alignmentsRemoved && (
                      <li>
                        {polyglot.t('IMPORT_HTML_DOC.UNALIGN', { smart_count: alignmentsRemoved })}
                      </li>
                    )}
                  </ul>
                ),
                warnings: warnings ?? [],
              });
            }
          }
        )
      )
      .catch(() =>
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.UNEXPECTED_ERROR')],
          warnings: [],
        })
      )
      .finally(() => setLoading(false));
  }, [file]);

  return (
    <ImportModal
      validFileTypes={importExtensions.html}
      modalTitle="IMPORT_HTML_DOC.MODAL_TITLE"
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

export default HtmlDocImportModal;
