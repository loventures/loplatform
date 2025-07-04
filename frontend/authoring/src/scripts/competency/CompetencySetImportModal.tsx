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
import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';

import { getAllEditedOutEdges, getEditedAsset, useEditedAsset, useGraphEdits } from '../graphEdit';
import { useModal, usePolyglot } from '../hooks';
import { importExtensions } from '../importer/importConstants';
import ImportModal from '../importer/ImportModal';
import { createPreview } from '../importer/importUtils';
import { errMessage } from '../importer/template/TemplateImportModal';
import { closeModal } from '../modals/modalActions';
import AuthoringOpsService from '../services/AuthoringOpsService';
import EdgeEditorService from '../services/EdgeEditorService';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { WriteOp } from '../types/api';
import { NodeName } from '../types/asset';
import { Polyglot } from '../types/polyglot';
import * as T from '../types/typeIds';
import { generateWriteOps, getDeleteOps } from './competencySetParse';
import { useEditedCompetencySetEdges } from './useFlatCompetencies';

export const competencyTypeIds = [T.Level1Competency, T.Level2Competency, T.Level3Competency];

// This can use the graph edit state because it doesn't do constant small edits
const CompetencySetImportModal: React.FC = () => {
  const [file, setFile] = useState<File | undefined>();
  const [pristine, setPristine] = useState(true);
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const competencySetEdge = useEditedCompetencySetEdges()[0];
  const competencySet = useEditedAsset(competencySetEdge?.targetName);
  const resetCompetencyTree = useModal<{ resetCompetencyTree: () => void }>().data
    .resetCompetencyTree;
  const l1EdgeNames = useMemo(
    () =>
      getAllEditedOutEdges(competencySet?.name, projectGraph, graphEdits)
        .filter(edge => edge.group === 'level1Competencies')
        .map(edge => edge.name),
    [competencySet, projectGraph, graphEdits]
  );

  const competencyTitles = useMemo(() => {
    const titles = new Array<string>();
    const loop = (name: NodeName | undefined): void => {
      for (const { group, targetName } of getAllEditedOutEdges(name, projectGraph, graphEdits)) {
        if (
          group === 'level1Competencies' ||
          group === 'level2Competencies' ||
          group === 'level3Competencies'
        ) {
          titles.push(getEditedAsset(targetName, projectGraph, graphEdits).data.title);
          loop(targetName);
        }
      }
    };
    loop(competencySet?.name);
    return titles;
  }, [competencySet, projectGraph, graphEdits]);

  const [replacePrevious, setReplacePrevious] = useState(false);

  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const [{ errors, preview, warnings }, setModalData] = useState({
    errors: new Array<string>(),
    preview: '',
    warnings: new Array<string>(),
  });
  const [loading, setLoading] = useState(false);
  const [writeOps, setWriteOps] = useState<WriteOp[]>([]);

  const finalizeImport = () => {
    setLoading(true);
    AuthoringOpsService.postWriteOps(writeOps)
      .then(EdgeEditorService.sendEdgesToStructurePanel)
      .then(() => {
        resetCompetencyTree();
        dispatch(closeModal());
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
    generateWriteOps(
      file,
      competencySet,
      replacePrevious ? [] : l1EdgeNames,
      replacePrevious ? [] : competencyTitles,
      projectGraph.rootNodeName
    )
      .then(({ ops, errors, warnings }) => {
        if (errors.length) {
          setModalData({
            errors: errors.map(e => errMessage(e, polyglot)),
            preview: '',
            warnings: [],
          });
        } else {
          const extraDeleteOps = getDeleteOps(
            replacePrevious,
            competencySet,
            projectGraph,
            graphEdits
          );
          setWriteOps(extraDeleteOps.concat(ops));
          const typeIdsToSize = mapValues(
            groupBy(
              ops.filter(fop => fop.op === 'addNode' && fop.typeId !== 'competencySet.1'),
              op => op.typeId
            ),
            nodes => nodes.length
          );
          const preview = createPreview(
            typeIdsToSize,
            polyglot,
            polyglot.t('COMPETENCY_SET_IMPORT.MANIFEST_TO_THE_SET'),
            'COMPETENCY_SET_IMPORT.ITEM'
          );
          const previewDeleted = deletePreview(
            competencyTitles.length,
            polyglot,
            polyglot.t('COMPETENCY_SET_IMPORT.MANIFEST_FROM_THE_SET'),
            'COMPETENCY_SET_IMPORT.ITEM'
          );
          setModalData({
            errors: [],
            preview: replacePrevious && extraDeleteOps.length ? previewDeleted + preview : preview,
            warnings: warnings.map(e => errMessage(e, polyglot)),
          });
        }
      })
      .catch(() =>
        setModalData({
          errors: [polyglot.t('IMPORT_MODAL.UNEXPECTED_ERROR')],
          preview: '',
          warnings: [],
        })
      )
      .finally(() => setLoading(false));
  }, [file, replacePrevious]);

  return (
    <ImportModal
      validFileTypes={importExtensions.csv}
      modalTitle="COMPETENCY_SET_IMPORT.MODAL_TITLE"
      finalizeImport={finalizeImport}
      errors={errors}
      preview={preview}
      warnings={warnings}
      loading={loading}
      file={file}
      setFile={setFile}
      replacePrevious={replacePrevious}
      setReplacePrevious={competencySetEdge ? setReplacePrevious : null}
    />
  );
};

export default CompetencySetImportModal;

/// Private
const deletePreview = (compCount, polyglot: Polyglot, destination: string, itemName: string) => {
  const compMessage = `${polyglot.t(`${itemName}${compCount !== 1 ? '-plural' : ''}`)}`;
  return `${polyglot.t('REMOVE')} ${compCount} ${compMessage} and all alignments ${polyglot.t(
    'IMPORT_MODAL.FROM'
  )} ${destination}. `;
};
