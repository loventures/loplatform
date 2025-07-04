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

import { sortBy } from 'lodash';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import CreatableSelect from 'react-select/creatable';
import { Button, Col, Input, Label, Row, Spinner } from 'reactstrap';

import { UPDATE_BRANCH } from '../../../dcmStoreConstants';
import { useDcmSelector } from '../../../hooks';
import { putImportFileInBlobstore } from '../../../importer/importUtils';
import { openToast, TOAST_TYPES } from '../../../toast/actions';
import { User } from '../../../types/user';
import { stockClassnames } from '../../AlignmentEditor/Aligner';
import { reloadProjects } from '../../dataActions';
import { ProjectResponse } from '../../NarrativeMultiverse';
import { useIsStoryEditMode } from '../../storyHooks';
import {
  copyProject,
  CourseConfig,
  CoursePreferences,
  createProject,
  getProject,
  getProjectConfig,
  importProject,
  loadProjectProps,
  PutProject,
  putProject,
  putProjectConfig,
  putProjectContributors,
} from '../ProjectHistory/projectApi';
import { ProjectConfigurationForm } from './ProjectConfigurationForm';
import { ProjectContributors } from './ProjectContributors';

export const ProjectSettingsForm: React.FC<{
  create?: boolean;
  copy?: ProjectResponse;
  dirty: boolean;
  setDirty: (dirty: boolean) => void;
  doCancel?: () => void;
}> = ({ create, copy, dirty, setDirty, doCancel }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const editMode = useIsStoryEditMode() || create;
  const [saving, setSaving] = useState(false);
  const [dirty0, setDirty0] = useState(false);
  const [dirty1, setDirty1] = useState(false);
  const [dirty2, setDirty2] = useState(false);
  const { projectStatuses, projectStatusMapping } = useDcmSelector(s => s.configuration);

  useEffect(() => {
    setDirty(dirty0 || dirty1 || dirty2);
  }, [dirty0, dirty1, dirty2]);

  const [productTypeOptions, setProductTypeOptions] = useState(
    new Array<{ label: string; value: string }>()
  );
  const [categoryOptions, setCategoryOptions] = useState(
    new Array<{ label: string; value: string }>()
  );
  const [subcategoryOptions, setSubcategoryOptions] = useState(
    new Array<{ label: string; value: string }>()
  );

  const [configuration, setConfiguration] = useState<CourseConfig>({
    effective: {},
    overrides: {},
    defaults: {},
  });

  const [users, setUsers] = useState<Record<number, User>>({});
  const [contributors, setContributorsInternal] = useState<Record<number, string | null>>({});

  const setOverrides = (f: (c: CoursePreferences) => CoursePreferences): void => {
    setConfiguration(c => ({ ...c, overrides: f(c.overrides) }));
    setDirty1(true);
  };

  const setContributors = (c: Record<number, string>): void => {
    setContributorsInternal(c);
    setDirty2(true);
  };
  const badOwners = dirty2 && Object.values(contributors).filter(s => s === 'Owner').length !== 1;

  useEffect(() => {
    // ensure fresh
    if (!create) {
      getProject(project.id).then(({ projects: [{ project }], users }) => {
        dispatch({ type: UPDATE_BRANCH, layout: { project } });
        setUsers(users);
      });
      getProjectConfig(project.id).then(c => {
        setConfiguration(c);
      });
    }
    loadProjectProps('productType').then(options =>
      setProductTypeOptions(
        sortBy(
          options.map(value => ({ label: value, value })),
          'label'
        )
      )
    );
    loadProjectProps('category').then(options =>
      setCategoryOptions(
        sortBy(
          options.map(value => ({ label: value, value })),
          'label'
        )
      )
    );
    loadProjectProps('subCategory').then(options =>
      setSubcategoryOptions(
        sortBy(
          options.map(value => ({ label: value, value })),
          'label'
        )
      )
    );
  }, [create]);

  const project = useDcmSelector(s => s.layout.project);
  const domainId = useDcmSelector(s => s.configuration.domain.id);
  const [projectName, setProjectName] = useState(project.name ?? '');
  const [code, setCode] = useState(project.code ?? '');
  const [productType, setProductType] = useState(project.productType);
  const [launchDate, setLaunchDate] = useState(project.launchDate);
  const [s3, setS3] = useState(project.s3 ?? '');
  const [category, setCategory] = useState(project.category);
  const [subCategory, setSubCategory] = useState(project.subCategory);
  const [failed, setFailed] = useState(false);
  const [file, setFile] = useState<File>();
  const canImport = useDcmSelector(
    s => !s.layout.platform.isProduction || s.user.profile?.user_type === 'Overlord'
  );

  useEffect(() => {
    const from = copy?.project ?? project;
    setProjectName(copy ? `Copy of ${from.name}` : from.name);
    setCode(from.code ?? '');
    setProductType(from.productType);
    setLaunchDate(from.launchDate);
    setS3(from.s3 ?? '');
    setCategory(from.category);
    setSubCategory(from.subCategory);
    setContributorsInternal({
      [from.ownedBy]: 'Owner',
      ...from.contributedBy,
    });
  }, [project, copy]);

  const doSave = () => {
    setSaving(true);

    const putDto: PutProject = {
      projectName: projectName.trim(),
      code: code?.trim(),
      productType,
      category,
      subCategory,
      launchDate,
      s3,
      revision: project.revision, // unused
      liveVersion: project.liveVersion, // now project status
    };
    const status = 'Dev';
    const projectStatus = projectStatuses[status]; // mapped name
    const createDto = {
      ...putDto,
      liveVersion: projectStatus,
      projectStatus: projectStatus ? status : undefined,
      courseStatus: projectStatus ? projectStatusMapping[status] : undefined,
    };

    if (copy) {
      // Copy also supports specifying a targetDomain in which case it does a deep copy
      // and so returns a task report that you poll for until it is done. No one does this.
      copyProject({
        ...createDto,
        branchId: copy.branchId,
      })
        .then(res => {
          dispatch(openToast('Project copied.', TOAST_TYPES.SUCCESS));
          dispatch(reloadProjects());
          history.push(`/branch/${res.branchId}/story/${res.homeNodeName}`);
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('Create failed.', TOAST_TYPES.DANGER));
          setSaving(false);
        });
    } else if (file) {
      putImportFileInBlobstore(file, file.type, domainId)
        .then(source => importProject({ ...createDto, source }))
        .then(({ receipts }) => {
          if (receipts[0].status !== 'SUCCESS') throw 'Import status was not success.';
          const branchId = receipts[0].data.targetBranchId;
          dispatch(openToast('Project imported.', TOAST_TYPES.SUCCESS));
          dispatch(reloadProjects());
          history.push(`/branch/${branchId}/launch/home`);
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('Import failed.', TOAST_TYPES.DANGER));
          setSaving(false);
          setFailed(true);
        });
    } else if (create) {
      createProject(createDto)
        .then(({ projects: [branch] }) => {
          dispatch(openToast('Project created.', TOAST_TYPES.SUCCESS));
          dispatch(reloadProjects());
          history.push(`/branch/${branch.branchId}/story/${branch.project.homeNodeName}`);
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('Create failed.', TOAST_TYPES.DANGER));
          setSaving(false);
        });
    } else {
      const contrib = {
        owner: +Object.entries(contributors).find(([, role]) => role === 'Owner')[0],
        contributors: Object.fromEntries(
          Object.entries(contributors).filter(([, role]) => role !== 'Owner')
        ),
      };
      Promise.all([
        dirty0 ? putProject(project.id, putDto).then(() => setDirty0(false)) : void 0,
        dirty1
          ? putProjectConfig(project.id, configuration.overrides).then(() => setDirty1(false))
          : void 0,
        dirty2 ? putProjectContributors(project.id, contrib).then(() => setDirty2(false)) : void 0,
      ])
        .then(
          () =>
            getProject(project.id)
              .then(({ projects: [{ project }] }) =>
                dispatch({ type: UPDATE_BRANCH, layout: { project } })
              )
              .catch(() => history.push('/')) // 404: no longer have access
        )
        .then(() => {
          dispatch(openToast('Settings saved.', TOAST_TYPES.SUCCESS));
          dispatch(reloadProjects());
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('Save failed.', TOAST_TYPES.DANGER));
        })
        .finally(() => {
          setSaving(false);
        });
    }
  };

  const doReset = () => {
    getProject(project.id).then(({ projects: [{ project }], users }) => {
      dispatch({ type: UPDATE_BRANCH, layout: { project } });
      setUsers(users);
      setDirty0(false);
      setDirty2(false);
    });
    getProjectConfig(project.id).then(c => {
      setConfiguration(c);
      setDirty1(false);
    });
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (create && projectName.trim() && e.key === 'Enter') doSave();
  };

  return (
    <>
      <div className={create ? 'modal-body' : 'm-5'}>
        <div className="d-flex flex-column gap-3">
          <Row>
            <Col
              sm={10}
              className="pe-2"
            >
              <Label
                for="project-name-input"
                className="small gray-700"
              >
                Project Name
              </Label>
              <Input
                id="project-name-input"
                value={projectName}
                onChange={e => {
                  setProjectName(e.target.value);
                  setDirty0(true);
                }}
                type="text"
                disabled={!editMode}
                autoFocus={create}
                onKeyDown={onKeyDown}
                maxLength={255}
                autoComplete="off"
              />
            </Col>
            <Col
              sm={2}
              className="ps-2"
            >
              <Label
                for="course-code-input"
                className="small gray-700 text-nowrap"
              >
                Course Code
              </Label>
              <Input
                id="course-code-input"
                value={code}
                onChange={e => {
                  setCode(e.target.value);
                  setDirty0(true);
                }}
                type="text"
                disabled={!editMode}
                onKeyDown={onKeyDown}
                maxLength={255}
              />
            </Col>
          </Row>
          <Row>
            <Col
              sm={6}
              className="pe-2"
            >
              <Label
                for="product-type-input"
                className="small gray-700"
              >
                Product Type
              </Label>
              <CreatableSelect
                id="product-type-input"
                className="Select secretly"
                classNames={stockClassnames}
                options={productTypeOptions}
                value={productTypeOptions.find(p => p.value === productType)}
                onChange={e => {
                  setProductType(e?.value);
                  setDirty0(true);
                }}
                isDisabled={!editMode}
                isClearable={true}
                tabSelectsValue={false}
              />
            </Col>
            <Col
              sm={3}
              className="px-2"
            >
              <Label
                for="date-input"
                className="small gray-700"
              >
                Launch Date
              </Label>
              <Input
                id="date-input"
                value={!launchDate ? '' : launchDate}
                onChange={e => {
                  setLaunchDate(e.target.value || undefined);
                  setDirty0(true);
                }}
                type="date"
                disabled={!editMode}
              />
            </Col>
            <Col
              sm={3}
              className="ps-2"
            >
              <Label
                for="s3-input"
                className="small gray-700"
              >
                S3 Folder
              </Label>
              <Input
                id="s3-input"
                value={s3}
                onChange={e => {
                  setS3(e.target.value);
                  setDirty0(true);
                }}
                type="text"
                disabled={!editMode}
                maxLength={255}
              />
            </Col>
          </Row>
          <Row>
            <Col
              sm={6}
              className="pe-2"
            >
              <Label
                for="category-input"
                className="small gray-700"
              >
                Category
              </Label>
              <CreatableSelect
                id="category-input"
                className="Select secretly"
                classNames={stockClassnames}
                options={categoryOptions}
                value={categoryOptions.find(p => p.value === category)}
                onChange={e => {
                  setCategory(e?.value);
                  setDirty0(true);
                }}
                isDisabled={!editMode}
                isClearable={true}
                tabSelectsValue={false}
              />
            </Col>
            <Col
              sm={6}
              className="ps-2"
            >
              <Label
                for="subcategory-input"
                className="small gray-700"
              >
                Subcategory
              </Label>
              <CreatableSelect
                id="subcategory-input"
                className="Select secretly"
                classNames={stockClassnames}
                options={subcategoryOptions}
                value={subcategoryOptions.find(p => p.value === subCategory)}
                onChange={e => {
                  setSubCategory(e?.value);
                  setDirty0(true);
                }}
                isDisabled={!editMode}
                isClearable={true}
                tabSelectsValue={false}
              />
            </Col>
          </Row>
          {create && !copy && canImport && (
            <div>
              <Label
                for="import-project"
                className="small gray-700"
              >
                Exchange File
              </Label>
              <Input
                id="import-project"
                type="file"
                onChange={e => {
                  setFile(e.target.files.item(0));
                  setFailed(false);
                }}
                accept=".zip"
                invalid={failed}
              />
            </div>
          )}
          {!create && (
            <ProjectContributors
              editMode={editMode}
              users={users}
              contributors={contributors}
              setContributors={setContributors}
            />
          )}
          {!create && (
            <ProjectConfigurationForm
              editMode={editMode}
              config={configuration}
              setOverrides={setOverrides}
            />
          )}
        </div>
      </div>
      {create ? (
        <div className="modal-footer">
          <Button
            id="project-settings-cancel-btn"
            color="primary"
            outline
            onClick={doCancel}
          >
            Cancel
          </Button>
          <Button
            id="project-settings-create-btn"
            color="primary"
            disabled={saving || !projectName.trim()}
            onClick={() => doSave()}
          >
            Create Project
            {saving && (
              <Spinner
                className="ms-2"
                size="sm"
              />
            )}
          </Button>
        </div>
      ) : editMode ? (
        <div className="d-flex justify-content-center mt-5 mb-4 gap-3">
          <Button
            id="project-settings-reset-btn"
            color="primary"
            outline
            disabled={!dirty || saving}
            onClick={() => doReset()}
          >
            Reset
          </Button>
          <Button
            id="project-settings-save-btn"
            color="primary"
            disabled={!dirty || saving || !projectName.trim() || badOwners}
            className={saving ? 'saving' : undefined}
            onClick={() => doSave()}
          >
            Save Project Settings
          </Button>
        </div>
      ) : null}
    </>
  );
};
