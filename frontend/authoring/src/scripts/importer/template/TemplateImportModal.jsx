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

import { groupBy, mapValues } from 'lodash';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { compose } from 'recompose';

import { useFlatCompetencies } from '../../competency/useFlatCompetencies';
import withTranslations from '../../hoc/withTranslations';
import { closeModal } from '../../modals/modalActions';
import AuthoringOpsService from '../../services/AuthoringOpsService';
import EdgeEditorService from '../../services/EdgeEditorService';
import { useTargets } from '../../structurePanel/hooks';
import { importExtensions } from '../importConstants';
import ImportModal from '../ImportModal';
import { createPreview } from '../importUtils';
import { generateWriteOps } from './templateImportValidation';

const TemplateImportModal = ({ edges, courseNodeName, polyglot, dispatch, dropzoneFile }) => {
  const [file, setFile] = useState(dropzoneFile);
  const [pristine, setPristine] = useState(true);
  const competencies = useFlatCompetencies();
  const categories = useTargets(courseNodeName, 'gradebookCategories', 'gradebookCategory.1');

  const [{ errors, preview, warnings }, setModalData] = useState({
    errors: [],
    preview: '',
    warnings: [],
  });
  const [loading, setLoading] = useState(false);
  const [writeOps, setWriteOps] = useState([]);

  const finalizeImport = () => {
    setLoading(true);
    AuthoringOpsService.postWriteOps(writeOps)
      .then(EdgeEditorService.sendEdgesToStructurePanel)
      .then(EdgeEditorService.sendEdgesToAssetInEdit())
      .then(() => dispatch(closeModal()));
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

    if (!importExtensions.csv.includes(fileExtension)) {
      setModalData({
        errors: [
          polyglot.t('IMPORT_MODAL.BAD_FILE_TYPE', {
            fileType: fileExtension,
            validTypes: importExtensions.csv.join(' or '),
          }),
        ],
        preview: '',
        warnings: [],
      });
      return;
    }

    setLoading(true);
    setPristine(false);
    generateWriteOps(file, edges, courseNodeName, competencies, categories)
      .then(({ ops, errors, warnings }) => {
        if (errors.length) {
          setModalData({
            errors: errors.map(e => errMessage(e, polyglot)),
            preview: '',
            warnings: [],
          });
        } else {
          setWriteOps(ops);
          const typeIdsToSize = mapValues(
            groupBy(
              ops.filter(fop => fop.op === 'addNode'),
              op => op.typeId
            ),
            nodes => nodes.length
          );
          const preview = createPreview(
            typeIdsToSize,
            polyglot,
            polyglot.t('TEMPLATE_IMPORT.MANIFEST_TO_THE_COURSE'),
            'TEMPLATE_IMPORT.ITEM'
          );
          setModalData({
            errors: [],
            preview: preview,
            warnings: warnings.map(w => errMessage(w, polyglot)),
          });
        }
        setLoading(false);
      })
      .catch(e => {
        console.log(e);
        setModalData({
          errors: [polyglot.t('TEMPLATE_IMPORT.UNKNOWN_ERROR')],
          preview: '',
          warnings: [],
        });
      });
  }, [file]);

  return (
    <ImportModal
      validFileTypes={importExtensions.csv}
      modalTitle="TEMPLATE_IMPORT.MODAL_TITLE"
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

TemplateImportModal.propTypes = {
  // from store
  courseNodeName: PropTypes.string.isRequired,
  edges: PropTypes.object.isRequired,
  polyglot: PropTypes.object.isRequired,
  dispatch: PropTypes.func.isRequired,
  dropzoneFile: PropTypes.object,
};

const mapStateToProps = state => {
  return {
    courseNodeName: state.modal.data?.name ?? state.assetEditor.assetNode.name,
    edges: state.projectGraph.edges,
    dropzoneFile: state.modal.data.file,
  };
};

export const errMessage = ({ rowNum, err, warn, ...transVars }, polyglot) => {
  const translatedInterpolationArgs = Object.keys(transVars).reduce(
    (acc, key) => ({
      ...acc,
      [key]: key === 'inputValue' ? transVars[key] : polyglot.t(transVars[key]),
    }),
    {}
  );
  return (
    `${polyglot.t('TEMPLATE_IMPORT.MODAL_LINE')}` +
    ` ${rowNum} ` +
    `- ${polyglot.t(err ?? warn, translatedInterpolationArgs)}`
  );
};

export default compose(withTranslations, connect(mapStateToProps))(TemplateImportModal);
