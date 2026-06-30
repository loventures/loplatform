/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import 'codemirror/mode/javascript/javascript';

import axios from 'axios';
import classnames from 'classnames';
import React, { useEffect, useRef, useState } from 'react';
import CodeMirror from 'react-codemirror';
import { useDispatch } from 'react-redux';
import {
  Alert,
  Button,
  ButtonGroup,
  Col,
  Form,
  Input,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Row,
} from 'reactstrap';
import _ from 'underscore';

import { AdminFormFile } from '../components/adminForm';
import WaitDotGif from '../components/WaitDotGif';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { ComponentsUrl } from '../services/URLs';
import * as filters from './filters';
import LocalProxyCard from './LocalProxyCard';

const SearchDelay = 250; /*ms*/
const DoubleClickDelay = 200; /*ms*/

// Tree nodes are dynamically shaped and mutated in place.
interface ComponentNode {
  id: string;
  name?: string;
  label?: string;
  type?: string;
  disabled?: boolean;
  removable?: boolean;
  active?: boolean;
  toggled?: boolean;
  configuration?: string;
  children?: ComponentNode[];
  [key: string]: any;
}

interface UploadInfo {
  fileName: string;
  [key: string]: unknown;
}

interface LocalProxyEnabled {
  authoring: boolean;
  course: boolean;
}

// A minimal recursive tree, replacing react-treebeard (whose velocity-react
// animations failed to mount children under React 18). Nodes are mutated in
// place (toggled/active) and the page forces a re-render after each click, so
// this stays a simple presentational component that reads the current node state.
const TreeNode: React.FC<{
  node: ComponentNode;
  depth: number;
  onToggle: (node: ComponentNode, toggled: boolean) => void;
}> = ({ node, depth, onToggle }) => {
  const hasChildren = !!node.children?.length;
  return (
    <div>
      <div
        id={`components-${node.id.replace(/ /g, '')}`}
        className={classnames('components-tree-row', {
          'components-disabled': node.disabled,
          'components-tree-active': node.active,
        })}
        style={{ paddingLeft: `${depth * 1.25 + 0.25}rem` }}
        onClick={() => onToggle(node, !node.toggled)}
      >
        <span className="components-tree-toggle">
          {hasChildren ? (node.toggled ? '▾' : '▸') : ''}
        </span>
        {node.name}
      </div>
      {node.toggled &&
        hasChildren &&
        node.children!.map(child => (
          <TreeNode
            key={child.id}
            node={child}
            depth={depth + 1}
            onToggle={onToggle}
          />
        ))}
    </div>
  );
};

const App: React.FC = () => {
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();
  const clusterType = lo_platform.clusterType;

  const [loaded, setLoaded] = useState(false);
  const [tree, setTree] = useState<ComponentNode>({} as ComponentNode);
  const [data, setData] = useState<ComponentNode[]>([]);
  const [cursor, setCursor] = useState<ComponentNode | null>(null);
  const [modal, setModal] = useState(false);
  const [configModal, setConfigModal] = useState(false);
  const [uploadInfo, setUploadInfo] = useState<UploadInfo | null>(null);
  const [code, setCode] = useState<string | null>(null);
  const [installError, setInstallError] = useState(false);
  const [setConfigError, setSetConfigError] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [localProxyEnabled, setLocalProxyEnabled] = useState<LocalProxyEnabled>({
    authoring: false,
    course: false,
  });

  const dblClickTimeout = useRef(0);
  // A ref mirror of the tree so callbacks read the current value without
  // re-creating the debounced filter on every render (matches the class field).
  const treeRef = useRef<ComponentNode>(tree);
  treeRef.current = tree;

  const buildTree = (node: ComponentNode): ComponentNode => {
    node.name = node.label;
    if (!node.id) node.id = `${node.name}`;
    if (node.configuration)
      node.configuration = JSON.stringify(JSON.parse(node.configuration), undefined, 2);
    if (node.children) {
      node.children.forEach(child => buildTree(child));
    }
    return node;
  };

  const findComponent = (label: string, t?: ComponentNode): ComponentNode | undefined =>
    (t ?? treeRef.current)?.children?.[0]?.children?.find(c => c.id === label)?.children?.[0]
      ?.children?.[0];

  const proxyEnabled = (label: string, t?: ComponentNode): boolean => {
    const node = findComponent(label, t);
    const config = JSON.parse(node?.configuration ?? 'null');
    return config?.localProxyEnabled ?? false;
  };

  const refresh = () => {
    axios.get(`${ComponentsUrl}/nodes`).then(res => {
      const rings = res.data.objects.map((ring: ComponentNode) => ({
        ...ring,
        id: ring.name || ring.label,
      }));
      const masterNode: ComponentNode = {
        id: 'Master',
        label: 'Master',
        children: rings,
      };
      const newTree = buildTree(masterNode);
      const proxy: LocalProxyEnabled = {
        authoring: proxyEnabled('loi.authoring', newTree),
        course: proxyEnabled('loi.courseware', newTree),
      };
      setTree(newTree);
      setLoaded(true);
      setData(newTree.children ?? []);
      setLocalProxyEnabled(proxy);
    });
  };

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const postConfig = (node: ComponentNode, config: string): Promise<void> => {
    const requestData = { id: node.id, config };
    setSubmitting(true);
    return axios
      .post(`${ComponentsUrl}/setConfig`, requestData)
      .then(() => {
        const message = T.t('adminPage.components.setConfig.successMessage', { name: node.name });
        node.configuration = config;
        setCursor(node);
        setSubmitting(false);
        dispatch(setPortalAlertStatus(false, true, message));
      })
      .catch(err => {
        console.log(err);
        setSetConfigError(true);
        setSubmitting(false);
        return Promise.reject();
      });
  };

  const setProxyEnabled = (label: string, url: string) => (value: boolean) => {
    const node = findComponent(label);
    if (!node) return;
    const config = JSON.parse(node.configuration ?? 'null') ?? {};
    config.localProxyEnabled = value;
    config.localProxyUrl = url;
    postConfig(node, JSON.stringify(config)).then(() => refresh());
  };

  const setNodeActive = (node: ComponentNode) => {
    // horrid side effects bring shame upon our team
    if (cursor) cursor.active = false;
    node.active = !node.active;
    setCursor(node);
    // useState bails out when `node` is the same reference as the current cursor
    // (re-clicking the same row), so the in-place node mutations above wouldn't
    // re-render the tree. The original class setState always re-rendered; force a
    // new `data` reference so the tree re-renders the toggle/active change.
    setData(prev => [...prev]);
  };

  const onToggle = (node: ComponentNode, toggled: boolean) => {
    if (node.children) node.toggled = toggled;
    setNodeActive(node);
  };

  const onRowClick = (node: ComponentNode, toggled: boolean) => {
    if (node.type !== 'component') {
      // just normal click behaviour
      onToggle(node, toggled);
    } else if (dblClickTimeout.current) {
      /* multiple clicks received within `DoubleClickDelay` ms;
       * consider this a double-click and open config modal
       */
      window.clearTimeout(dblClickTimeout.current);
      dblClickTimeout.current = 0;
      setNodeActive(node);
      toggleConfigModal();
    } else {
      /* either a single click or the first click of a double click
       * wait for `DoubleClickDelay` ms and then assume single-clickage
       */
      dblClickTimeout.current = window.setTimeout(() => {
        onToggle(node, toggled);
        dblClickTimeout.current = 0;
      }, DoubleClickDelay);
    }
  };

  const toggleComponent = () => {
    if (!cursor) return;
    const requestData = {
      enabled: cursor.disabled,
      identifier: cursor.id,
    };
    axios
      .post(`${ComponentsUrl}/toggle`, requestData)
      .then(() => {
        const action = requestData.enabled ? 'enable' : 'disable';
        const message = T.t(`adminPage.components.${action}.${cursor.type}.successMessage`, {
          ...cursor,
        });
        cursor.disabled = !cursor.disabled;
        setCursor(cursor);
        dispatch(setPortalAlertStatus(false, true, message));
      })
      .catch(() => {
        dispatch(
          setPortalAlertStatus(
            true,
            false,
            T.t(`adminPage.components.toggle.${cursor.type}.errorMessage`, { ...cursor })
          )
        );
      });
  };

  const deleteComponent = () => {
    if (!cursor) return;
    axios
      .post(`${ComponentsUrl}/delete/${cursor.id}`, {})
      .then(() => {
        const message = T.t(`adminPage.components.delete.${cursor.type}.successMessage`, {
          ...cursor,
        });
        refresh();
        dispatch(setPortalAlertStatus(false, true, message));
        setCursor(cursor);
      })
      .catch(() => {
        dispatch(
          setPortalAlertStatus(
            true,
            false,
            T.t(`adminPage.components.delete.${cursor.type}.errorMessage`, { ...cursor })
          )
        );
      });
  };

  const toggleModal = () => {
    setModal(!modal);
    setInstallError(false);
    setUploadInfo(null);
  };

  const onArchiveChange = (changed: { error?: string; value?: UploadInfo }) => {
    if (changed.error) {
      console.log(changed.error);
    } else {
      setUploadInfo(changed.value ?? null);
    }
  };

  const installArchive = () => {
    if (uploadInfo) {
      const requestData = {
        uploadInfo,
        uninstall: false,
      };
      setSubmitting(true);
      axios
        .post(`${ComponentsUrl}/install`, requestData)
        .then(() => {
          const message = T.t('adminPage.components.install.archive.successMessage', {
            fileName: requestData.uploadInfo.fileName,
          });
          toggleModal();
          refresh();
          dispatch(setPortalAlertStatus(false, true, message));
          setSubmitting(false);
        })
        .catch(err => {
          console.log(err);
          setInstallError(true);
          setSubmitting(false);
        });
    }
  };

  const updateCode = (newCode: string) => setCode(newCode);

  const toggleConfigModal = () => {
    setConfigModal(prev => !prev);
    setSetConfigError(false);
    setCode('');
  };

  const setConfig = () => {
    if (!cursor) return;
    let config = code ?? '';
    try {
      config = JSON.stringify(JSON.parse(code ?? ''));
    } catch (e) {
      /* ignore, the server will error */
    }
    postConfig(cursor, config).then(() => {
      toggleConfigModal();
    });
  };

  const renderConfigModal = () => {
    const options = {
      lineNumbers: true,
      lineWrapping: true,
      mode: { name: 'javascript', json: true },
    };
    return (
      <Modal
        isOpen={configModal}
        toggle={toggleConfigModal}
        id="components-config-modal"
        size="xl"
      >
        <ModalHeader
          toggle={toggleConfigModal}
          tag="h2"
        >
          {T.t('adminPage.components.configModal.header')}
        </ModalHeader>
        <ModalBody>
          {setConfigError && (
            <Alert
              color="danger"
              id="components-config-modal-alert-error"
            >
              {T.t('adminPage.components.configModal.errorMessage')}
            </Alert>
          )}
          <CodeMirror
            value={code || (cursor && cursor.configuration) || ''}
            autoFocus
            className="components-config-modal-editor"
            onChange={updateCode}
            options={options}
            style={{ minHeight: '25rem' }}
          />
        </ModalBody>
        <ModalFooter>
          <Button
            color="secondary"
            disabled={submitting}
            onClick={toggleConfigModal}
            id="components-config-modal-close"
          >
            {T.t('adminPage.components.configModal.footer.cancel')}
          </Button>{' '}
          <Button
            color="success"
            disabled={submitting}
            onClick={setConfig}
            id="components-config-modal-submit"
          >
            {T.t('adminPage.components.configModal.footer.submit')}
            {submitting && (
              <WaitDotGif
                className="ms-2 waiting"
                color="light"
                size={16}
              />
            )}
          </Button>
        </ModalFooter>
      </Modal>
    );
  };

  const renderInstallModal = () => {
    return (
      <Modal
        isOpen={modal}
        toggle={toggleModal}
        id="components-install-modal"
      >
        <ModalHeader toggle={toggleModal}>
          {T.t('adminPage.components.installModal.header')}
        </ModalHeader>
        <ModalBody>
          {installError && (
            <Alert
              color="danger"
              id="components-install-modal-alert-error"
            >
              {T.t('adminPage.components.installModal.errorMessage')}
            </Alert>
          )}
          <AdminFormFile
            key="archive"
            entity="components"
            field="archive"
            T={T}
            label={false}
            onChange={onArchiveChange}
          />
        </ModalBody>
        <ModalFooter>
          <Button
            color="secondary"
            disabled={submitting}
            onClick={toggleModal}
            id="components-install-modal-close"
          >
            {T.t('adminPage.components.installModal.footer.cancel')}
          </Button>{' '}
          <Button
            color="success"
            disabled={submitting || !uploadInfo}
            onClick={installArchive}
            id="components-install-modal-submit"
          >
            {T.t('adminPage.components.installModal.footer.install')}
            {submitting && (
              <WaitDotGif
                className="ms-2 waiting"
                color="white"
                size={16}
              />
            )}
          </Button>
        </ModalFooter>
      </Modal>
    );
  };

  const renderButtonBar = () => {
    return (
      <ButtonGroup style={{ marginBottom: '15px' }}>
        <Button
          color="success"
          onClick={toggleModal}
          id="components-install-btn"
        >
          {T.t('adminPage.components.buttonBar.installArchive')}
        </Button>
        <Button
          disabled={!cursor || cursor.type !== 'component'}
          onClick={toggleConfigModal}
          id="components-edit-btn"
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            create
          </i>
        </Button>
        <Button
          disabled={!cursor || (cursor.type !== 'archive' && cursor.type !== 'component')}
          onClick={toggleComponent}
          id="components-toggle-btn"
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            {!cursor || !cursor.disabled ? 'not_interested' : 'check'}
          </i>
        </Button>
        <Button
          disabled={!cursor || cursor.type !== 'archive' || !cursor.removable}
          color="danger"
          onClick={deleteComponent}
          id="components-delete-btn"
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            delete
          </i>
        </Button>
      </ButtonGroup>
    );
  };

  const onFilterMouseUp = useRef(
    (() => {
      const debounced = _.debounce((filter: string) => {
        const currentTree = treeRef.current;
        if (!filter) {
          setData(currentTree.children ?? []);
          return;
        }
        let filtered = filters.filterTree(currentTree as filters.TreeNode, filter);
        filtered = filters.expandFilteredNodes(filtered, filter);
        setData((filtered.children as ComponentNode[]) ?? []);
      }, SearchDelay);
      // need to call e.target because React nulls it out too soon
      return (e: React.KeyboardEvent<HTMLInputElement>) =>
        debounced((e.target as HTMLInputElement).value.trim());
    })()
  ).current;

  if (!loaded) return null;
  return (
    <React.Fragment>
      {renderInstallModal()}
      {renderConfigModal()}
      <div className="container-fluid">
        <Row>
          <Col>{renderButtonBar()}</Col>
          <Col>
            <Input
              type="text"
              onKeyUp={onFilterMouseUp}
              id="components-search"
              placeholder="Filter"
            />
          </Col>
        </Row>
        {clusterType === 'Local' && (
          <Form
            inline
            className="d-flex justify-content-around"
          >
            <LocalProxyCard
              title="Authoring"
              isEnabled={localProxyEnabled.authoring}
              setEnabled={setProxyEnabled('loi.authoring', 'https://localhost:5173/')}
            />
            <LocalProxyCard
              title="Course"
              isEnabled={localProxyEnabled.course}
              setEnabled={setProxyEnabled('loi.courseware', 'https://localhost:5174/')}
            />
          </Form>
        )}
        <div className="components-tree">
          {data.map(node => (
            <TreeNode
              key={node.id}
              node={node}
              depth={0}
              onToggle={onRowClick}
            />
          ))}
        </div>
      </div>
    </React.Fragment>
  );
};

export default App;
