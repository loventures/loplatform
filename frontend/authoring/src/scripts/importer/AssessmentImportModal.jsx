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

import gretchen, { fileApi } from '../grfetchen/';
import { groupBy, isEmpty, mapValues } from 'lodash';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { compose } from 'recompose';

import QUESTION_TYPES from '../asset/constants/questionTypes.constants';
import { useEditedAsset } from '../graphEdit';
import { withTranslations } from '../hoc';
import { closeModal } from '../modals/modalActions';
import reactRouterService from '../router/ReactRouterService';
import AuthoringOpsService from '../services/AuthoringOpsService';
import EdgeEditorService, { filterResponseToNewEdges } from '../services/EdgeEditorService';
import { importExtensions } from './importConstants';
import ImportModal from './ImportModal';
import { createPreview, pollForTaskReport, requestQtiImportPreview } from './importUtils';

const AssessmentImportModal = ({
  dropzoneFile,
  branchId,
  domainId,
  polyglot,
  assessmentNode,
  dispatch,
}) => {
  const validFileTypes = [...importExtensions.qti, ...importExtensions.loqi];
  const [file, setFile] = useState(dropzoneFile);
  const [pristineFile, setPristineFile] = useState(true);
  const assessment = useEditedAsset(assessmentNode);
  const assessmentName = assessment.name;
  const assessmentNodeTitle = assessment.data.title;

  const [{ errors, preview, warnings }, setModalData] = useState({
    errors: [],
    preview: '',
    warnings: [],
  });
  const [loading, setLoading] = useState(false);
  const [qtiImport, setQtiImport] = useState({});
  const [loqiImport, setLoqiImport] = useState({});

  const closeImportModal = () => dispatch(closeModal());

  const finalizeQtiImport = data => {
    gretchen
      .post('/api/v2/authoring/:branchId/import/qti')
      .params({ branchId })
      .data(data)
      .exec()
      .then(({ receipts }) => {
        const importReceipt = receipts[0];
        const importReceiptId = importReceipt.id;
        const taskReport = importReceipt.report;

        pollForTaskReport(importReceiptId, taskReport.id)
          .then(resp => {
            const imported = resp.receipts[0].importedRoots;
            const addEdgeOps = imported.map(node =>
              AuthoringOpsService.addEdgeOp(assessmentName, node.name, 'questions')
            );
            const existingEdges = EdgeEditorService.getEdgesInGroup('questions');
            const ordering = [...existingEdges.map(e => e.name), ...addEdgeOps.map(o => o.name)];
            const setEdgeOrderingOp = AuthoringOpsService.setEdgeOrderOp(
              assessmentName,
              'questions',
              ordering
            );
            // can be replaced with postAddEdge?
            return AuthoringOpsService.postWriteOps([...addEdgeOps, setEdgeOrderingOp])
              .then(EdgeEditorService.sendEdgesToStructurePanel)
              .then(EdgeEditorService.sendEdgesToAssetInEdit())
              .then(filterResponseToNewEdges(assessmentName))
              .then(EdgeEditorService.fetchChildEdgesForStructurePanel)
              .catch(() => EdgeEditorService.handleError())
              .then(closeImportModal);
          })
          .catch(e => {
            console.error(e);
            setModalData({
              errors: [polyglot.t('IMPORT_MODAL.IMPORT_FAILURE')],
              preview: [],
              warnings: [],
            });
            setLoading(false);
          });
      });
  };

  const finalizeImport = () => {
    if (!isEmpty(qtiImport)) {
      setLoading(true);
      const data = {
        receiptId: qtiImport.receiptId,
        description: `importing ${qtiImport.filename}`,
        type: 'plainQuestions',
      };
      finalizeQtiImport(data);
    } else if (!isEmpty(loqiImport)) {
      setLoading(true);
      AuthoringOpsService.postWriteOps(loqiImport)
        .then(EdgeEditorService.sendEdgesToAssetInEdit())
        .then(EdgeEditorService.sendEdgesToStructurePanel)
        .catch(() => EdgeEditorService.handleError())
        .then(closeImportModal);
    }
  };

  const handleQtiPreview = file => {
    setLoqiImport({});
    requestQtiImportPreview(file, branchId, domainId, 'plainQuestions')
      .then(result => {
        const { manifest, taskReport } = result;
        const report = taskReport.report;
        const errors = report.errors.map(error => error.value);
        const warnings = report.warnings.map(w => w.value);
        const preview = !errors.length
          ? createPreview(
              mapValues(
                groupBy(
                  manifest.nodes.filter(node => QUESTION_TYPES.some(q => q === node.typeId)),
                  node => node.typeId
                ),
                nodes => nodes.length
              ),
              polyglot,
              assessmentNodeTitle,
              'IMPORT_MODAL.QUESTION'
            )
          : '';

        setQtiImport({
          receiptId: result.receiptId,
          fileName: file.name,
        });

        setModalData({
          errors,
          preview,
          warnings,
        });
        setLoading(false);
      })
      .catch(() => {
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.IMPORT_FAILURE')],
          preview: [],
          warnings: [],
        });
        setLoading(false);
      });
  };

  const handleLoqiPreview = file => {
    setQtiImport({});

    const tpe = 'loqi'; // file.name.endsWith('.txt') ? 'miqi' : 'loqi';
    fileApi
      .post(`/api/v2/authoring/${branchId}/import/${tpe}/validate?assessmentName=${assessmentName}`)
      .headers({
        'X-CSRF': true,
        'Content-Type': file.type,
      })
      .params({})
      .file(file)
      .exec()
      .then(({ writeOps, errors, warnings }) => {
        if (errors.length) {
          setModalData({
            errors: errors.map(e => e.error),
            preview: '',
            warnings: [],
          });
          setLoading(false);
        } else {
          setLoqiImport(writeOps);
          const preview = createPreview(
            mapValues(
              groupBy(
                writeOps.filter(writeOp => writeOp.op === 'addNode'),
                node => node.typeId
              ),
              nodes => nodes.length
            ),
            polyglot,
            assessmentNodeTitle,
            'IMPORT_MODAL.QUESTION'
          );
          setModalData({
            errors: [],
            preview: preview,
            warnings: warnings.map(e => e.error),
          });
          setLoading(false);
        }
      });
  };

  useEffect(() => {
    if (!file) {
      if (!pristineFile) {
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.NO_FILE_UPLOADED')],
          preview: '',
          warnings: [],
        });
        setLoading(false);
      }
      setPristineFile(false);
    } else {
      setLoading(true);
      const fileExtension = `.${file.name.split('.').pop()}`;

      if (importExtensions.qti.includes(fileExtension)) {
        handleQtiPreview(file);
      } else if (importExtensions.loqi.includes(fileExtension)) {
        handleLoqiPreview(file);
      } else {
        setModalData({
          errors: [
            polyglot.t('IMPORT_MODAL.BAD_FILE_TYPE', {
              fileType: fileExtension,
              validTypes: validFileTypes.join(' or '),
            }),
          ],
          preview: '',
          warnings: [],
        });
      }
      setLoading(false);
    }
  }, [file]);

  return (
    <ImportModal
      validFileTypes={validFileTypes}
      modalTitle="IMPORT_MODAL.ASSESSMENT_IMPORT_TITLE"
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

AssessmentImportModal.propTypes = {
  // from store
  domainId: PropTypes.number.isRequired,
  assessmentNode: PropTypes.string.isRequired,
  branchId: PropTypes.string.isRequired,
  polyglot: PropTypes.object.isRequired,
  dropzoneFile: PropTypes.object,
};

const mapStateToProps = state => {
  const { branchId } = reactRouterService.getCurrentParams(state);

  return {
    domainId: state.configuration.domain.id,
    assessmentNode: state.modal.data?.name ?? state.assetEditor.assetNode.name,
    branchId,
    dropzoneFile: state.modal.data.file,
  };
};

export default compose(withTranslations, connect(mapStateToProps))(AssessmentImportModal);
