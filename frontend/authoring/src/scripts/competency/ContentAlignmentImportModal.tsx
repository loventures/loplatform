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

import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';

import { getAllEditedOutEdges, getEditedAsset, useGraphEdits } from '../graphEdit';
import { useModal, usePolyglot } from '../hooks';
import { importExtensions } from '../importer/importConstants';
import ImportModal from '../importer/ImportModal';
import { errMessage } from '../importer/template/TemplateImportModal';
import { closeModal } from '../modals/modalActions';
import AuthoringOpsService from '../services/AuthoringOpsService';
import EdgeEditorService from '../services/EdgeEditorService';
import { isQuestion } from '../story/questionUtil';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { WriteOp } from '../types/api';
import { ElementNodes, generateWriteOps } from './contentAlignmentParse';
import { useFlatCompetencies } from './useFlatCompetencies';

const ContentAlignmentImportModal: React.FC = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const [file, setFile] = useState<File | undefined>();
  const [pristine, setPristine] = useState(true);
  const [{ errors, preview, warnings }, setModalData] = useState({
    errors: new Array<string>(),
    preview: '',
    warnings: new Array<string>(),
  });
  const [loading, setLoading] = useState(false);
  const [writeOps, setWriteOps] = useState<WriteOp[]>([]);
  const resetAlignments = useModal<{ resetAlignments: () => void }>().data.resetAlignments;

  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const { homeNodeName } = projectGraph;
  const competencies = useFlatCompetencies();

  const competenciesByTitle = useMemo(() => {
    const competenciesByTitle: Record<string, string> = {};
    for (const competency of competencies) {
      competenciesByTitle[competency.data.title.trim().toLowerCase()] = competency.name;
    }
    return competenciesByTitle;
  }, [competencies]);

  /** Tree by content title/question number. */
  const contentTree = useMemo(() => {
    const loop = (name: string, elementNodes: ElementNodes): ElementNodes => {
      let question = 0;
      for (const edge of getAllEditedOutEdges(name, projectGraph, graphEdits)) {
        const node = getEditedAsset(edge.targetName, projectGraph, graphEdits);
        if (node && (edge.group === 'elements' || edge.group === 'questions')) {
          const outEdges = getAllEditedOutEdges(node.name, projectGraph, graphEdits);
          const title = isQuestion(node.typeId)
            ? (++question).toString()
            : node.data.title.trim().toLowerCase();
          elementNodes[title] = {
            node,
            teaches: outEdges.filter(edge => edge.group === 'teaches'),
            assesses: outEdges.filter(edge => edge.group === 'assesses'),
            children: loop(node.name, {}),
          };
        }
      }
      return elementNodes;
    };
    return loop(homeNodeName, {});
  }, [homeNodeName, projectGraph, graphEdits]);

  const finalizeImport = () => {
    setLoading(true);
    AuthoringOpsService.postWriteOps(writeOps)
      .then(EdgeEditorService.sendEdgesToStructurePanel)
      .then(() => {
        resetAlignments();
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
    if (!homeNodeName) return;

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

    generateWriteOps(file, contentTree, competenciesByTitle)
      .then(({ ops, errors, warnings, added, removed }) => {
        if (errors.length) {
          setModalData({
            errors: errors.map(e => errMessage(e, polyglot)),
            preview: '',
            warnings: [],
          });
        } else {
          setWriteOps(ops);
          const preview =
            polyglot.t('CONTENT_ALIGNMENT_IMPORT.ADD', { smart_count: added }) +
            (!removed
              ? ''
              : polyglot.t('CONTENT_ALIGNMENT_IMPORT.REMOVE', { smart_count: removed })) +
            ' ' +
            polyglot.t('CONTENT_ALIGNMENT_IMPORT.THE_COURSE');
          setModalData({
            errors: [],
            preview: preview,
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
  }, [homeNodeName, file]);

  return (
    <ImportModal
      validFileTypes={importExtensions.csv}
      modalTitle="CONTENT_ALIGNMENT_IMPORT.MODAL_TITLE"
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

export default ContentAlignmentImportModal;
