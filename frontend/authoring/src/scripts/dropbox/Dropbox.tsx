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

import { flatMap } from 'lodash';
import React, { useEffect, useMemo, useState } from 'react';
import Dropzone from 'react-dropzone';
import { IoFolderOpenOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { useSet } from 'react-use';
import {
  Alert,
  Button,
  Form,
  FormFeedback,
  FormGroup,
  Input,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Progress,
  Table,
} from 'reactstrap';
import { useDebounce } from 'use-debounce';

import { Loadable } from '../authoringUi';
import { formatMDYHM } from '../dateUtil';
import { useDcmSelector, usePolyglot } from '../hooks';
import { uploadToCampusPack } from '../importer/htmlTransferService';
import { ApiQueryResults } from '../srs/apiQuery';
import { openToast } from '../toast/actions';
import { Polyglot } from '../types/polyglot';
import { setDropboxState } from './dropboxActions';
import {
  DropboxFileDto,
  DropboxParams,
  archiveDropboxFile,
  createDropboxFolder,
  dropboxFileUrl,
  dropboxFilesUrl,
  loadDropboxFiles,
  moveDropboxFile,
  renameDropboxFile,
  uploadDropboxFile,
} from './dropboxApi';
import { DropboxDirectory } from './dropboxReducer';

const formatSize = (s: number, polyglot: Polyglot) =>
  s < 1024
    ? polyglot.t('SIZE.B', { size: s })
    : s < 1024 * 1024
      ? polyglot.t('SIZE.KB', { size: Math.round(s / 1024) })
      : s < 1024 * 1024 * 1024
        ? polyglot.t('SIZE.MB', { size: Math.round(s / 1024 / 1024) })
        : polyglot.t('SIZE.GB', { size: Math.round(s / 1024 / 1024 / 1024) });

const suffix = (filename: string | undefined) => {
  const match = filename?.match(/\.[a-zA-Z0-9]+$/);
  return match ? match[0] : '';
};

const toFqdn = (path: DropboxFileDto[]): string =>
  '/' + path.map(dir => dir.fileName.toLowerCase()).join('/');

const DropboxRenameModal: React.FC<{
  file: DropboxFileDto;
  renameFile: (id: number, name: string) => void;
  closeModal: () => void;
}> = ({ file, renameFile, closeModal }) => {
  const polyglot = usePolyglot();
  const [fileName, setFileName] = useState(file.fileName);
  const invalidRename = !file.directory && suffix(fileName) !== suffix(file.fileName);

  return (
    <Modal
      isOpen
      toggle={closeModal}
    >
      <Form
        className="d-flex flex-column"
        style={{ minHeight: 0 }}
        onSubmit={e => {
          e.preventDefault();
          const trimmed = fileName.trim();
          if (!trimmed) return;
          if (trimmed.toLowerCase() !== file.fileName.toLowerCase()) renameFile(file.id, trimmed);
          closeModal();
        }}
      >
        <ModalHeader toggle={closeModal}>{polyglot.t('DROPBOX_RENAME_FILE')}</ModalHeader>
        <ModalBody>
          <FormGroup>
            <Label for="file-name">{polyglot.t('DROPBOX_FILE_NAME')}</Label>
            <Input
              type="text"
              id="file-name"
              value={fileName}
              onChange={e => setFileName(e.target.value)}
              invalid={invalidRename}
            />
            <FormFeedback>
              {polyglot.t('DROPBOX_INVALID_RENAME', {
                suffix: suffix(file.fileName),
              })}
            </FormFeedback>
          </FormGroup>
        </ModalBody>
        <ModalFooter>
          <Button
            outline
            onClick={closeModal}
          >
            {polyglot.t('CLOSE')}
          </Button>
          <Button
            color="primary"
            type="submit"
            disabled={!fileName.trim() || invalidRename}
          >
            {polyglot.t('RENAME')}
          </Button>
        </ModalFooter>
      </Form>
    </Modal>
  );
};

const DropboxFolderModal: React.FC<{
  createFolder: (name: string) => void;
  closeModal: () => void;
}> = ({ closeModal, createFolder }) => {
  const polyglot = usePolyglot();
  const [fileName, setFileName] = useState('');

  return (
    <Modal
      isOpen
      toggle={closeModal}
    >
      <Form
        className="d-flex flex-column"
        style={{ minHeight: 0 }}
        onSubmit={e => {
          e.preventDefault();
          if (!fileName.trim()) return;
          createFolder(fileName.trim());
          closeModal();
        }}
      >
        <ModalHeader toggle={closeModal}>{polyglot.t('DROPBOX_CREATE_FOLDER')}</ModalHeader>
        <ModalBody>
          <FormGroup>
            <Label for="folder-name">{polyglot.t('DROPBOX_FOLDER_NAME')}</Label>
            <Input
              type="text"
              id="folder-name"
              value={fileName}
              onChange={e => setFileName(e.target.value)}
            />
          </FormGroup>
        </ModalBody>
        <ModalFooter>
          <Button
            outline
            onClick={closeModal}
          >
            {polyglot.t('CLOSE')}
          </Button>
          <Button
            color="primary"
            type="submit"
            disabled={!fileName.trim()}
          >
            {polyglot.t('CREATE')}
          </Button>
        </ModalFooter>
      </Form>
    </Modal>
  );
};

type Dir = DropboxFileDto & { depth: number; disabled: boolean; children: Dir[] };

const DropboxMoveModal: React.FC<{
  assetName?: string;
  files: DropboxFileDto[];
  moveFiles: (files: DropboxFileDto[], folder: number | null) => void;
  closeModal: () => void;
}> = ({ assetName, files, moveFiles, closeModal }) => {
  const polyglot = usePolyglot();
  const {
    project: { id: projectId },
    branchId,
  } = useDcmSelector(state => state.layout);

  const [selection, setSelection] = useState<null | number | undefined>(undefined);
  const [dirs, setDirs] = useState(new Array<Dir>());

  useEffect(() => {
    loadDropboxFiles(projectId, branchId, {
      asset: assetName,
      archived: false,
      directory: true,
    }).then(({ objects }) => {
      const mkDir = (dto: DropboxFileDto, depth: number): Dir => ({
        ...dto,
        depth,
        disabled: files.some(f => f.id === dto.id),
        children: objects.filter(o => o.folder === dto.id).map(o => mkDir(o, 1 + depth)),
      });
      const tree = objects.filter(o => o.folder == null).map(o => mkDir(o, 1));
      const flattenDir = (dir: Dir): Dir[] =>
        dir.disabled ? [dir] : [dir, ...flatMap(dir.children, dir => flattenDir(dir))];
      setDirs(flatMap(tree, flattenDir));
    });
  }, []);

  return (
    <Modal
      isOpen
      toggle={closeModal}
    >
      <ModalHeader toggle={closeModal}>{polyglot.t('DROPBOX_CHOOSE_DIR')}</ModalHeader>
      <ModalBody className="ps-5 pb-0">
        <FormGroup check>
          <Label check>
            <Input
              name="target"
              type="radio"
              checked={selection === null}
              onChange={() => setSelection(null)}
            />
            <IoFolderOpenOutline className="text-primary me-1" />
            {polyglot.t('DROPBOX_LINK_TITLE')}
          </Label>
        </FormGroup>
        {dirs.map(dir => (
          <FormGroup
            key={dir.id}
            style={{ paddingLeft: `${dir.depth * 1.4}rem` }}
            check
          >
            <Label check>
              <Input
                name="target"
                type="radio"
                disabled={dir.disabled}
                checked={dir.id === selection}
                onChange={() => setSelection(dir.id)}
              />
              <IoFolderOpenOutline className="text-primary me-1" />
              {dir.fileName}
            </Label>
          </FormGroup>
        ))}
      </ModalBody>
      <ModalFooter>
        <Button
          outline
          onClick={closeModal}
        >
          {polyglot.t('CLOSE')}
        </Button>
        <Button
          color="primary"
          onClick={() => {
            if (selection !== undefined && files.some(f => f.folder !== selection))
              moveFiles(files, selection);
            closeModal();
          }}
          disabled={selection === undefined}
        >
          {polyglot.t('MOVE')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

const DropboxArchiveModal: React.FC<{
  files: DropboxFileDto[];
  archiveFiles: (files: DropboxFileDto[], boolean) => void;
  closeModal: () => void;
}> = ({ files, archiveFiles, closeModal }) => {
  const polyglot = usePolyglot();

  return (
    <Modal
      isOpen
      toggle={closeModal}
    >
      <ModalHeader toggle={closeModal}>{polyglot.t('DROPBOX_CONFIRM_ARCHIVE')}</ModalHeader>
      <ModalBody>
        {polyglot.t('DROPBOX_CONFIRM_ARCHIVE_BODY', { smart_count: files.length })}
      </ModalBody>
      <ModalFooter>
        <Button
          outline
          onClick={closeModal}
        >
          {polyglot.t('CLOSE')}
        </Button>
        <Button
          color="primary"
          onClick={() => {
            archiveFiles(files, true);
            closeModal();
          }}
        >
          {polyglot.t('ARCHIVE')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

type DropboxTableProps = ApiQueryResults<DropboxFileDto> & {
  folder: number | null;
  directories?: Record<number, DropboxDirectory>;
  assetName?: string;
  doRefresh: () => void;
  setOffset: (offset: number) => void;
  onUpload: () => void;
  createFolder: (directory: string) => void;
  renameFile: (id: number, fileName: string) => void;
  moveFiles: (files: DropboxFileDto[], folder: number | null) => void;
  archiveFiles: (files: DropboxFileDto[], archived: boolean) => void;
  userCanEdit: boolean;
};

const DropboxTable: React.FC<DropboxTableProps> = ({
  objects,
  count,
  offset,
  limit,
  filterCount,
  totalCount,
  folder,
  directories,
  assetName,
  doRefresh,
  setOffset,
  onUpload,
  createFolder,
  renameFile,
  moveFiles,
  archiveFiles,
  userCanEdit,
}) => {
  const polyglot = usePolyglot();
  const {
    project: { id: projectId },
    branchId,
  } = useDcmSelector(state => state.layout);

  const [rotate, setRotate] = useState(0);

  const [modalOpen, setModalOpen] = useState<'folder' | 'rename' | 'move' | 'archive' | undefined>(
    undefined
  );
  const closeModal = () => setModalOpen(undefined);

  const { archived } = useDcmSelector(state => state.dropbox);

  const [selected, { toggle: toggleSelected, reset: resetSelected }] = useSet(new Set<number>());
  useEffect(resetSelected, [objects]);
  const selectedFiles = objects.filter(o => selected.has(o.id));
  const firstSelection = selectedFiles[0];

  const getPath = (dir: number | null) => {
    const path = directories?.[dir]?.path;
    if (!path || !folder) return path ?? [];
    const idx = path.findIndex(dir => dir.id === folder);
    return path.slice(1 + idx);
  };

  return (
    <div onClick={e => e.stopPropagation()}>
      <div
        className="d-flex flex-row align-items-center py-2 px-3 position-sticky bg-light border-bottom"
        style={{ top: '3rem' }}
      >
        {archived ? (
          <>
            <Button
              size="sm"
              color="success"
              outline
              className="material-icons md-18 p-1 minibutton"
              title={polyglot.t('DROPBOX_UNARCHIVE')}
              onClick={() => archiveFiles(selectedFiles, false)}
              disabled={!userCanEdit || !selected.size}
            >
              <span className="material-icons md-18">restore</span>
            </Button>
          </>
        ) : (
          <>
            <Button
              size="sm"
              color="success"
              className="py-1"
              onClick={onUpload}
              disabled={archived || !userCanEdit}
            >
              {polyglot.t('DROPBOX_UPLOAD')}
            </Button>
            <Button
              size="sm"
              color="primary"
              outline
              className="material-icons md-18 p-1 ms-2 mini-button"
              title={polyglot.t('DROPBOX_CREATE_FOLDER')}
              onClick={() => setModalOpen('folder')}
              disabled={archived || !userCanEdit}
            >
              <span className="material-icons md-18">create_new_folder</span>
            </Button>
            <Button
              size="sm"
              color="primary"
              outline
              className="material-icons md-18 p-1 ms-2 mini-button"
              title={polyglot.t('DROPBOX_RENAME_FILE')}
              onClick={() => setModalOpen('rename')}
              disabled={archived || !userCanEdit || selected.size !== 1}
            >
              <span className="material-icons md-18">edit</span>
            </Button>
            <Button
              size="sm"
              color="primary"
              outline
              className="material-icons md-18 p-1 ms-2 mini-button"
              title={polyglot.t('DROPBOX_MOVE')}
              onClick={() => setModalOpen('move')}
              disabled={!userCanEdit || !selected.size}
            >
              <span className="material-icons md-18">drive_file_move</span>
            </Button>
            <Button
              size="sm"
              color="danger"
              outline
              className="material-icons md-18 p-1 ms-2 mini-button"
              title={polyglot.t('DROPBOX_ARCHIVE')}
              onClick={() => setModalOpen('archive')}
              disabled={!userCanEdit || !selected.size}
            >
              <span className="material-icons md-18">delete</span>
            </Button>
            <Button
              size="sm"
              color="primary"
              outline
              className="material-icons md-18 p-1 ms-2 mini-button"
              title={polyglot.t('DROPBOX_DOWNLOAD')}
              tag="a"
              href={dropboxFilesUrl(projectId, selected)}
              disabled={archived || !selected.size}
              target="_blank"
              rel="noreferrer noopener"
            >
              <span className="material-icons md-18">download</span>
            </Button>
          </>
        )}
        <div className="flex-grow-1"></div>
        <Button
          size="sm"
          color="transparent"
          className="material-icons md-18 p-1 me-3"
          title={polyglot.t('DROPBOX_REFRESH')}
          onClick={() => {
            setRotate(r => 360 + r);
            doRefresh();
          }}
        >
          <span
            className="material-icons md-18"
            style={{ transition: 'transform linear .5s', transform: `rotate(${rotate}deg)` }}
          >
            refresh
          </span>
        </Button>
        <div className="me-1">
          {count ? `${offset + 1} – ${offset + count} of ${filterCount}` : `0 of ${filterCount}`}
          {filterCount < totalCount ? ` (out of ${totalCount})` : ''}
        </div>
        <Button
          disabled={offset === 0}
          size="sm"
          color="transparent"
          className="material-icons md-18 p-1 ms-2 mini-button"
          title={polyglot.t('DROPBOX_PREVIOUS_PAGE')}
          onClick={() => setOffset(offset - limit)}
        >
          chevron_left
        </Button>
        <Button
          disabled={offset + count >= filterCount}
          size="sm"
          color="transparent"
          className="material-icons md-18 p-1 ms-2 mini-button"
          title={polyglot.t('DROPBOX_NEXT_PAGE')}
          onClick={() => setOffset(offset + limit)}
        >
          chevron_right
        </Button>
      </div>
      <Table className="dropbox-table mb-0">
        <tbody>
          {!objects.length ? (
            <tr className="inactive">
              <td>{polyglot.t(totalCount ? 'DROPBOX_NO_FILES_FILTER' : 'DROPBOX_NO_FILES')}</td>
            </tr>
          ) : (
            objects.map(file => (
              <tr key={file.id}>
                <td
                  style={{ width: '2.75rem', paddingLeft: '1.75rem', paddingRight: 0 }}
                  className="align-middle"
                >
                  <input
                    className="ms-0"
                    checked={selected.has(file.id)}
                    onChange={() => toggleSelected(file.id)}
                    type="checkbox"
                  />
                </td>
                <td
                  style={{ width: '3rem' }}
                  className="align-middle"
                >
                  <span className="material-icons md-24 text-muted">
                    {file.directory ? 'folder' : 'attach_file'}
                  </span>
                </td>
                <td className="p-0">
                  {file.directory ? (
                    <div style={{ padding: '.75rem' }}>
                      <div className="d-flex">
                        {getPath(file.folder).map(dir => (
                          <React.Fragment key={dir.id}>
                            <Link
                              to={
                                dir.archived ? undefined : `/branch/${branchId}/dropbox/${dir.id}`
                              }
                              className="text-truncate d-block text-decoration-none"
                            >
                              <span className={dir.archived ? 'text-muted' : 'linky'}>
                                {dir.fileName}
                              </span>
                            </Link>
                            <span className="text-muted mx-1">/</span>
                          </React.Fragment>
                        ))}
                        <Link
                          to={file.archived ? undefined : `/branch/${branchId}/dropbox/${file.id}`}
                          className="text-truncate d-block text-decoration-none"
                        >
                          <span className={file.archived ? 'text-muted' : 'linky'}>
                            {file.fileName}
                          </span>
                        </Link>
                      </div>
                      <div
                        className="text-muted"
                        style={{ textDecoration: 'none !important' }}
                      >
                        {polyglot.t('DROPBOX_CREATED_BY', {
                          name: file.creator?.fullName ?? 'Unknown Person',
                          date: formatMDYHM(file.created),
                        })}
                      </div>
                    </div>
                  ) : (
                    <div style={{ padding: '.75rem' }}>
                      <div className="d-flex">
                        {getPath(file.folder).map(dir => (
                          <React.Fragment key={dir.id}>
                            <Link
                              to={
                                dir.archived ? undefined : `/branch/${branchId}/dropbox/${dir.id}`
                              }
                              className="text-truncate d-block text-decoration-none"
                            >
                              <span className={dir.archived ? 'text-muted' : 'linky'}>
                                {dir.fileName}
                              </span>
                            </Link>
                            <span className="text-muted mx-1">/</span>
                          </React.Fragment>
                        ))}
                        <a
                          href={dropboxFileUrl(file.project, file.id)}
                          className="text-truncate d-block text-decoration-none"
                          title={file.fileName}
                          target="_blank"
                          rel="noreferrer noopener"
                        >
                          <span className="linky">{file.fileName}</span>
                        </a>
                      </div>
                      <div
                        className="text-muted"
                        style={{ textDecoration: 'none !important' }}
                      >
                        {polyglot.t('DROPBOX_UPLOADED_BY', {
                          name: file.creator?.fullName ?? 'Unknown Person',
                          date: formatMDYHM(file.created),
                        })}
                      </div>
                    </div>
                  )}
                </td>
                <td
                  style={{
                    width: '6rem',
                    paddingRight: '1.75rem',
                    textAlign: 'right',
                    verticalAlign: 'middle',
                  }}
                >
                  {file.directory ? null : formatSize(file.size, polyglot)}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </Table>
      {modalOpen === 'folder' ? (
        <DropboxFolderModal
          createFolder={createFolder}
          closeModal={closeModal}
        />
      ) : modalOpen === 'rename' ? (
        <DropboxRenameModal
          file={firstSelection}
          renameFile={renameFile}
          closeModal={closeModal}
        />
      ) : modalOpen === 'move' ? (
        <DropboxMoveModal
          assetName={assetName}
          files={selectedFiles}
          moveFiles={moveFiles}
          closeModal={closeModal}
        />
      ) : modalOpen === 'archive' ? (
        <DropboxArchiveModal
          files={selectedFiles}
          archiveFiles={archiveFiles}
          closeModal={closeModal}
        />
      ) : null}
    </div>
  );
};

const ROOT_ID = -1; // not supposed to use null as a key

const Dropbox: React.FC<{ assetName?: string; folder: number | null }> = ({
  assetName,
  folder,
}) => {
  const {
    project: { id: projectId, homeNodeName },
    branchId,
    userCanEdit,
  } = useDcmSelector(state => state.layout);
  const [uploading, setUploading] = useState(false);
  const [count, setCount] = useState(0);
  const [total, setTotal] = useState(0);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState<string | undefined>();
  const [files, setFiles] = useState<ApiQueryResults<DropboxFileDto>>();
  const { filter, archived, directories } = useDcmSelector(state => state.dropbox);
  const [refresh, setRefresh] = useState(0);
  const [offset, setOffset] = useState(0);
  const [confirmOverwrite, setConfirmOverwrite] = useState<
    [DropboxFileDto[], File[], File[]] | undefined
  >(undefined);
  const limit = 10;
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const nodeName = assetName ?? homeNodeName;

  const [debouncedFilter] = useDebounce(filter, 300);

  useEffect(() => {
    loadDropboxFiles(projectId, branchId, {
      asset: assetName,
      directory: true,
    })
      .then(({ objects: dirs }) => {
        const directories: Record<number, DropboxDirectory> = {
          [ROOT_ID]: { subdirectories: [], path: [] },
        };
        for (const directory of dirs) {
          directories[directory.id] = { directory, subdirectories: [], path: [] };
        }
        for (const directory of dirs) {
          directories[directory.folder ?? ROOT_ID].subdirectories.push(directory);
        }
        const loop = (directory: DropboxFileDto, context: DropboxFileDto[]) => {
          const { path, subdirectories } = directories[directory.id];
          path.push(...context, directory);
          for (const subdirectory of subdirectories) {
            loop(subdirectory, path);
          }
        };
        for (const directory of directories[ROOT_ID].subdirectories) {
          loop(directory, []);
        }
        dispatch(setDropboxState({ directories }));
      })
      .catch(e => {
        console.warn(e);
        setError('DROPBOX_UNEXPECTED_ERROR');
      });
  }, [projectId, branchId, assetName, refresh]);

  const unarchivedFolders = useMemo(() => {
    const loop = (dir: number | null, acc: Array<number | 'null'>) => {
      acc.push(dir ?? 'null');
      for (const directory of directories[dir ?? ROOT_ID]?.subdirectories ?? []) {
        if (!directory.archived) loop(directory.id, acc);
      }
      return acc;
    };
    return loop(folder, []);
  }, [directories, folder]);

  // If searching then search in all unarchived subfolders
  const folderFilter = debouncedFilter ? unarchivedFolders : folder;

  useEffect(() => setOffset(0), [folderFilter]);

  useEffect(() => {
    loadDropboxFiles(projectId, branchId, {
      asset: assetName,
      folder: folderFilter,
      archived,
      offset,
      limit,
      ...(debouncedFilter
        ? {
            fileName: debouncedFilter,
            nameCmp: 'co',
          }
        : {}),
    })
      .then(files => {
        if (offset && !files.count) {
          setOffset(0);
        } else {
          setFiles(files);
          setFetching(false);
        }
      })
      .catch(e => {
        console.warn(e);
        setError('DROPBOX_UNEXPECTED_ERROR');
      });
  }, [
    projectId,
    branchId,
    assetName,
    folderFilter,
    archived,
    debouncedFilter,
    offset,
    limit,
    refresh,
  ]);

  const doRefresh = () => setRefresh(r => 1 + r);

  const loadExistingFiles = async (
    name: string,
    params: Partial<DropboxParams> = {}
  ): Promise<DropboxFileDto[]> => {
    const { objects } = await loadDropboxFiles(projectId, branchId, {
      asset: assetName,
      folder,
      archived: false,
      fileName: name,
      nameCmp: 'eq',
      ...params,
    });
    return objects;
  };

  const doUploadFiles = async (dupes: DropboxFileDto[], files: File[]) => {
    const prefix = folder ? toFqdn(directories[folder].path) : '';
    const foldersByFqdn: Record<string, DropboxFileDto> = {};
    for (const { path } of Object.values(directories)) {
      if (path.every(p => !p.archived)) foldersByFqdn[toFqdn(path)] = path[path.length - 1];
    }
    try {
      for (const dupe of dupes) {
        await archiveDropboxFile(projectId, dupe.id, true);
      }
      setTotal(files.length);
      for (const file of files) {
        let destFolder = folder;
        const { path } = file as { path?: string };
        if (path?.startsWith('/')) {
          const parts = path.split('/').slice(1, -1); // ['', ...parts, fname]
          let dirname = prefix;
          for (const part of parts) {
            dirname = dirname + '/' + part.toLowerCase();
            let dir = foldersByFqdn[dirname];
            if (!dir) {
              dir = await createDropboxFolder(projectId, branchId, nodeName, destFolder, part);
              foldersByFqdn[dirname] = dir;
            }
            destFolder = dir.id;
          }
        }
        // just clumsily retry up to 3 times
        let aleph = 0;
        do {
          try {
            const upload = await uploadToCampusPack(file);
            await uploadDropboxFile(projectId, branchId, nodeName, destFolder, upload);
            aleph = -1;
          } catch (e) {
            if (++aleph == 3) throw e;
            console.log(e);
            dispatch(openToast(polyglot.t('DROPBOX_RETRY_UPLOAD', { name: file.name }), 'danger'));
          }
        } while (aleph !== -1);
        setCount(count => 1 + count);
      }
      dispatch(openToast(polyglot.t('DROPBOX_UPLOADED', { smart_count: files.length }), 'success'));
    } catch (e) {
      console.warn(e);
      setError('DROPBOX_UPLOAD_ERROR');
    }
    setUploading(false);
    doRefresh();
  };

  const safeUploadFiles = async (files: File[]) => {
    setError(undefined);
    setUploading(true);
    setCount(0);
    setTotal(0);

    try {
      const prefix = folder ? toFqdn(directories[folder].path) : '';
      const foldersByFqdn: Record<string, DropboxFileDto> = {};
      for (const { path } of Object.values(directories)) {
        if (path.every(p => !p.archived)) foldersByFqdn[toFqdn(path)] = path[path.length - 1];
      }

      const dupes = new Array<DropboxFileDto>();
      const safe = new Array<File>();
      for (const file of files) {
        // files in a directory have path property with the form /Dir/File.png
        const { path } = file as { path?: string };
        if (path?.startsWith('/')) {
          const idx = path.lastIndexOf('/');
          const fqdn = prefix + path.substring(0, idx).toLowerCase();
          const dir = foldersByFqdn[fqdn];
          if (dir || fqdn === '/') {
            const existing = await loadExistingFiles(file.name, { folder: dir.id ?? null });
            if (existing.length) {
              dupes.push(...existing);
            } else {
              safe.push(file);
            }
          } else {
            safe.push(file);
          }
        } else {
          const existing = await loadExistingFiles(file.name);
          if (existing.length) {
            dupes.push(...existing);
          } else {
            safe.push(file);
          }
        }
      }
      if (dupes.length) {
        // raise a confirm modal
        setConfirmOverwrite([dupes, files, safe]);
      } else {
        await doUploadFiles(dupes, files);
      }
    } catch (e) {
      console.warn(e);
      setError('DROPBOX_UPLOAD_ERROR');
      setUploading(false);
    }
  };

  const clearValidate = () => {
    setConfirmOverwrite(undefined);
    setUploading(false);
  };

  const doOverwrite = ([dupes, files]: typeof confirmOverwrite) => {
    setConfirmOverwrite(undefined);
    doUploadFiles(dupes, files).then(() => void 0);
  };

  const doSkip = ([, , safe]: typeof confirmOverwrite) => {
    setConfirmOverwrite(undefined);
    doUploadFiles([], safe).then(() => void 0);
  };

  const createFolder = async (name: string) => {
    setError(undefined);
    try {
      const existing = await loadExistingFiles(name);
      if (existing.length) {
        dispatch(openToast(polyglot.t('DROPBOX_FILE_EXISTS', { name }), 'danger'));
      } else {
        await createDropboxFolder(projectId, branchId, nodeName, folder, name);
        dispatch(openToast(polyglot.t('DROPBOX_CREATED_FOLDER', { name }), 'success'));
        doRefresh();
      }
    } catch (e) {
      console.warn(e);
      setError('DROPBOX_UNEXPECTED_ERROR');
    }
  };

  const renameFile = async (id: number, name: string) => {
    setError(undefined);
    try {
      const existing = await loadExistingFiles(name);
      if (existing.length) {
        dispatch(openToast(polyglot.t('DROPBOX_FILE_EXISTS', { name }), 'danger'));
      } else {
        await renameDropboxFile(projectId, id, name);
        dispatch(openToast(polyglot.t('DROPBOX_RENAMED_TO', { name }), 'success'));
        doRefresh();
      }
    } catch (e) {
      console.warn(e);
      setError('DROPBOX_UNEXPECTED_ERROR');
    }
  };

  const moveFiles = async (files: DropboxFileDto[], folder: number | null): Promise<void> => {
    setError(undefined);
    try {
      const dupes = new Array<DropboxFileDto>();
      for (const file of files) {
        const existing = await loadExistingFiles(file.fileName, { folder });
        dupes.push(...existing);
      }
      if (dupes.length) {
        dispatch(
          openToast(
            polyglot.t('DROPBOX_MOVE_CONFLICT', {
              smart_count: files.length,
            }),
            'danger'
          )
        );
      } else {
        for (const file of files) {
          await moveDropboxFile(projectId, file.id, folder);
        }
        dispatch(openToast(polyglot.t('DROPBOX_MOVED', { smart_count: files.length }), 'success'));
      }
    } catch (e) {
      console.warn(e);
      setError('DROPBOX_UNEXPECTED_ERROR');
    }
    doRefresh();
  };

  const archiveFiles = async (files: DropboxFileDto[], archived: boolean): Promise<void> => {
    setError(undefined);
    try {
      const dupes = new Array<DropboxFileDto>();
      if (!archived) {
        // only care about unarchiving name conflicts
        for (const file of files) {
          const existing = await loadExistingFiles(file.fileName);
          dupes.push(...existing);
        }
      }
      if (dupes.length) {
        dispatch(
          openToast(
            polyglot.t('DROPBOX_UNARCHIVE_CONFLICT', {
              smart_count: files.length,
            }),
            'danger'
          )
        );
      } else {
        for (const file of files) {
          await archiveDropboxFile(projectId, file.id, archived);
        }
        dispatch(
          openToast(
            polyglot.t(archived ? 'DROPBOX_ARCHIVED' : 'DROPBOX_UNARCHIVED', {
              smart_count: files.length,
            }),
            'success'
          )
        );
      }
    } catch (e) {
      console.warn(e);
      setError('DROPBOX_UNEXPECTED_ERROR');
    }
    doRefresh();
  };

  return (
    <div>
      {error && (
        <Alert
          color="danger"
          toggle={() => setError(undefined)}
          className="mx-3"
        >
          {polyglot.t(error)}
        </Alert>
      )}
      <Dropzone
        onDrop={files => safeUploadFiles(files)}
        disabled={uploading || !userCanEdit || archived}
      >
        {({ getRootProps, getInputProps, isDragActive, open }) => (
          <div
            className="p-0 dropbox position-relative"
            {...getRootProps()}
          >
            <input
              {...getInputProps()}
              multiple={true}
              name="import-dropzone"
            />
            <Loadable loading={fetching}>
              {() => (
                <DropboxTable
                  {...files}
                  folder={folder}
                  directories={debouncedFilter ? directories : undefined}
                  assetName={assetName}
                  onUpload={open}
                  doRefresh={doRefresh}
                  setOffset={setOffset}
                  createFolder={createFolder}
                  renameFile={renameFile}
                  moveFiles={moveFiles}
                  archiveFiles={archiveFiles}
                  userCanEdit={userCanEdit}
                />
              )}
            </Loadable>
            {uploading ? (
              <div
                className="position-absolute w-100 h-100 d-flex align-items-center justify-content-center border-primary p-4"
                style={{
                  left: 0,
                  top: 0,
                  borderWidth: '2px',
                  borderStyle: 'dashed',
                  backgroundColor: '#007caa60',
                  borderRadius: '4px',
                }}
              >
                <Progress
                  animated
                  color="primary"
                  value={!total ? 100 : count}
                  max={!total ? undefined : total}
                  style={{ height: '2rem', fontSize: '1.2rem' }}
                >
                  {!total ? null : `${count} / ${total}`}
                </Progress>
              </div>
            ) : isDragActive ? (
              <div
                className="position-absolute w-100 h-100 d-flex align-items-center justify-content-center"
                style={{
                  left: 0,
                  top: 0,
                  border: '2px dashed green',
                  backgroundColor: '#80ff8080',
                  borderRadius: '4px',
                }}
              >
                <div
                  className="material-icons md-48"
                  style={{ color: 'green' }}
                >
                  upload
                </div>
              </div>
            ) : null}
          </div>
        )}
      </Dropzone>
      {confirmOverwrite && (
        <Modal
          isOpen
          toggle={clearValidate}
        >
          <ModalHeader toggle={clearValidate}>
            {polyglot.t('DROPBOX_CONFIRM_OVERWRITE')}
          </ModalHeader>
          <ModalBody>
            {polyglot.t('DROPBOX_CONFIRM_OVERWRITE_BODY', {
              smart_count: confirmOverwrite[0].length,
            })}
          </ModalBody>
          <ModalFooter>
            <Button
              outline
              onClick={clearValidate}
            >
              {polyglot.t('CLOSE')}
            </Button>
            <Button
              color="primary"
              onClick={() => doOverwrite(confirmOverwrite)}
            >
              {polyglot.t('OVERWRITE')}
            </Button>
            <Button
              color="primary"
              onClick={() => doSkip(confirmOverwrite)}
            >
              {polyglot.t('SKIP')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </div>
  );
};

export default Dropbox;
