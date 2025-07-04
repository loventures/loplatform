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

import 'codemirror/mode/javascript/javascript';

import axios from 'axios';
import classnames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import CodeMirror from 'react-codemirror';
import { connect } from 'react-redux';
import { Treebeard, decorators as defaultDecorators } from 'react-treebeard';
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
import { bindActionCreators } from 'redux';
import _ from 'underscore';

import { AdminFormFile } from '../components/adminForm';
import WaitDotGif from '../components/WaitDotGif';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { ComponentsUrl } from '../services/URLs';
import * as filters from './filters';
import LocalProxyCard from './LocalProxyCard';
import { style } from './TreeStyle';

const SearchDelay = 250; /*ms*/
const DoubleClickDelay = 200; /*ms*/

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      tree: {},
      data: {},
      cursor: null,
      modal: false,
      configModal: false,
      uploadInfo: null,
      code: null,
      installError: false,
      setConfigError: false,
      submitting: false,
      localProxy: {},
    };
    this.dblClickTimeout = 0;
  }

  componentDidMount() {
    this.refresh();
  }

  refresh = () => {
    axios.get(`${ComponentsUrl}/nodes`).then(res => {
      const rings = res.data.objects.map(ring => ({ ...ring, id: ring.name || ring.label }));
      const masterNode = {
        label: 'Master',
        children: rings,
      };
      const tree = this.buildTree(masterNode);
      const localProxyEnabled = {
        authoring: this.proxyEnabled('loi.authoring', tree),
        course: this.proxyEnabled('loi.courseware', tree),
      };
      this.setState({ tree: tree, loaded: true, data: tree.children, localProxyEnabled });
    });
  };

  findComponent = (label, tree) =>
    (tree ?? this.state.tree)?.children[0]?.children?.find(c => c.id === label)?.children[0]
      ?.children[0];

  proxyEnabled = (label, tree) => {
    const node = this.findComponent(label, tree);
    const config = JSON.parse(node?.configuration);
    return config?.localProxyEnabled ?? false;
  };

  setProxyEnabled = (label, url) => value => {
    const node = this.findComponent(label);
    const config = JSON.parse(node?.configuration) ?? {};
    config.localProxyEnabled = value;
    config.localProxyUrl = url;
    this.postConfig(node, JSON.stringify(config)).then(() => this.refresh());
  };

  onRowClick = (node, toggled) => {
    if (node.type !== 'component') {
      // just normal click behaviour
      this.onToggle(node, toggled);
    } else if (this.dblClickTimeout) {
      /* multiple clicks received within `DoubleClickDelay` ms;
       * consider this a double-click and open config modal
       */
      window.clearTimeout(this.dblClickTimeout);
      this.dblClickTimeout = 0;
      this.setNodeActive(node);
      this.toggleConfigModal();
    } else {
      /* either a single click or the first click of a double click
       * wait for `DoubleClickDelay` ms and then assume single-clickage
       */
      this.dblClickTimeout = window.setTimeout(() => {
        this.onToggle(node, toggled);
        this.dblClickTimeout = 0;
      }, DoubleClickDelay);
    }
  };

  onToggle = (node, toggled) => {
    if (node.children) node.toggled = toggled;
    this.setNodeActive(node);
  };

  setNodeActive = node => {
    const { cursor } = this.state;
    // horrid side effects bring shame upon our team
    if (cursor) cursor.active = false;
    node.active = !node.active;
    this.setState({ cursor: node });
  };

  buildTree = node => {
    node.name = node.label;
    if (!node.id) node.id = `${node.name}`;
    if (node.configuration)
      node.configuration = JSON.stringify(JSON.parse(node.configuration), undefined, 2);
    if (node.children) {
      node.children.forEach(child => this.buildTree(child));
    }
    return node;
  };

  toggleComponent = () => {
    const { setPortalAlertStatus, translations: T } = this.props;
    const { cursor } = this.state;
    const data = {
      enabled: cursor.disabled,
      identifier: cursor.id,
    };
    axios
      .post(`${ComponentsUrl}/toggle`, data)
      .then(() => {
        const action = data.enabled ? 'enable' : 'disable';
        const message = T.t(`adminPage.components.${action}.${cursor.type}.successMessage`, {
          ...cursor,
        });
        cursor.disabled = !cursor.disabled;
        this.setState({ cursor: cursor });
        setPortalAlertStatus(false, true, message);
      })
      .catch(() => {
        setPortalAlertStatus(
          true,
          false,
          T.t(`adminPage.components.toggle.${cursor.type}.errorMessage`, { ...cursor })
        );
      });
  };

  deleteComponent = () => {
    const { setPortalAlertStatus, translations: T } = this.props;
    const { cursor } = this.state;
    axios
      .post(`${ComponentsUrl}/delete/${cursor.id}`, {})
      .then(() => {
        const message = T.t(`adminPage.components.delete.${cursor.type}.successMessage`, {
          ...cursor,
        });
        this.refresh();
        setPortalAlertStatus(false, true, message);
        this.setState({ cursor: cursor });
      })
      .catch(() => {
        setPortalAlertStatus(
          true,
          false,
          T.t(`adminPage.components.delete.${cursor.type}.errorMessage`, { ...cursor })
        );
      });
  };

  toggleModal = () =>
    this.setState({ modal: !this.state.modal, installError: false, uploadInfo: null });

  onArchiveChange = data => {
    if (data.error) {
      console.log(data.error);
    } else {
      this.setState({ uploadInfo: data.value });
    }
  };

  installArchive = () => {
    const { setPortalAlertStatus, translations: T } = this.props;
    if (this.state.uploadInfo) {
      const data = {
        uploadInfo: this.state.uploadInfo,
        uninstall: false,
      };
      this.setState({ submitting: true });
      axios
        .post(`${ComponentsUrl}/install`, data)
        .then(() => {
          const message = T.t('adminPage.components.install.archive.successMessage', {
            fileName: data.uploadInfo.fileName,
          });
          this.toggleModal();
          this.refresh();
          setPortalAlertStatus(false, true, message);
          this.setState({ submitting: false });
        })
        .catch(err => {
          console.log(err);
          this.setState({ installError: true, submitting: false });
        });
    }
  };

  updateCode = code => this.setState({ code: code });

  toggleConfigModal = () =>
    this.setState({
      configModal: !this.state.configModal,
      setConfigError: false,
      code: '',
    });

  setConfig = () => {
    const { code, cursor } = this.state;
    let config = code;
    try {
      config = JSON.stringify(JSON.parse(code));
    } catch (e) {
      /* ignore, the server will error */
    }
    this.postConfig(cursor, config).then(() => {
      this.toggleConfigModal();
    });
  };

  postConfig = (node, config) => {
    const { setPortalAlertStatus, translations: T } = this.props;
    const data = { id: node.id, config };
    this.setState({ submitting: true });
    return axios
      .post(`${ComponentsUrl}/setConfig`, data)
      .then(() => {
        const message = T.t('adminPage.components.setConfig.successMessage', { name: node.name });
        node.configuration = config;
        this.setState({ cursor: node, submitting: false });
        setPortalAlertStatus(false, true, message);
      })
      .catch(err => {
        console.log(err);
        this.setState({ setConfigError: true, submitting: false });
        return Promise.reject();
      });
  };

  renderConfigModal = () => {
    const { translations: T } = this.props;
    const { code, cursor, setConfigError, submitting } = this.state;
    const options = {
      lineNumbers: true,
      lineWrapping: true,
      mode: { name: 'javascript', json: true },
    };
    return (
      <Modal
        isOpen={this.state.configModal}
        toggle={this.toggleConfigModal}
        id="components-config-modal"
        size="xl"
      >
        <ModalHeader
          toggle={this.toggleConfigModal}
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
            onChange={this.updateCode}
            options={options}
            style={{ minHeight: '25rem' }}
          />
        </ModalBody>
        <ModalFooter>
          <Button
            color="secondary"
            disabled={submitting}
            onClick={this.toggleConfigModal}
            id="components-config-modal-close"
          >
            {T.t('adminPage.components.configModal.footer.cancel')}
          </Button>{' '}
          <Button
            color="success"
            disabled={submitting}
            onClick={this.setConfig}
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

  renderInstallModal = () => {
    const { translations: T } = this.props;
    const { installError, submitting, uploadInfo } = this.state;
    return (
      <Modal
        isOpen={this.state.modal}
        toggle={this.toggleModal}
        id="components-install-modal"
      >
        <ModalHeader toggle={this.toggleModal}>
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
            onChange={this.onArchiveChange}
          />
        </ModalBody>
        <ModalFooter>
          <Button
            color="secondary"
            disabled={submitting}
            onClick={this.toggleModal}
            id="components-install-modal-close"
          >
            {T.t('adminPage.components.installModal.footer.cancel')}
          </Button>{' '}
          <Button
            color="success"
            disabled={submitting || !uploadInfo}
            onClick={this.installArchive}
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

  renderButtonBar = () => {
    const { cursor } = this.state;
    const { translations: T } = this.props;
    return (
      <ButtonGroup style={{ marginBottom: '15px' }}>
        <Button
          color="success"
          onClick={this.toggleModal}
          id="components-install-btn"
        >
          {T.t('adminPage.components.buttonBar.installArchive')}
        </Button>
        <Button
          disabled={!cursor || cursor.type !== 'component'}
          onClick={this.toggleConfigModal}
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
          onClick={this.toggleComponent}
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
          onClick={this.deleteComponent}
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

  onFilterMouseUp = (() => {
    const debounced = _.debounce(filter => {
      const { tree } = this.state;
      if (!filter) {
        return this.setState({ data: tree.children });
      }
      let filtered = filters.filterTree(tree, filter);
      filtered = filters.expandFilteredNodes(filtered, filter);
      this.setState({ data: filtered.children });
    }, SearchDelay);
    // need to call e.target because React nulls it out too soon
    return e => debounced(e.target.value.trim());
  })();

  render() {
    const { loaded, data } = this.state;
    if (!loaded) return null;
    const decorators = {
      ...defaultDecorators,
      Header: ({ node, style }) => {
        return (
          <div style={style.base}>
            <div
              style={style.title}
              className={classnames({ 'components-disabled': node.disabled })}
              id={`components-${node.id.replace(/ /g, '')}`}
            >
              {node.name}
            </div>
          </div>
        );
      },
    };
    return (
      <React.Fragment>
        {this.renderInstallModal()}
        {this.renderConfigModal()}
        <div className="container-fluid">
          <Row>
            <Col>{this.renderButtonBar()}</Col>
            <Col>
              <Input
                type="text"
                onKeyUp={this.onFilterMouseUp}
                id="components-search"
                placeholder="Filter"
              />
            </Col>
          </Row>
          {this.props.clusterType === 'Local' && (
            <Form
              inline
              className="d-flex justify-content-around"
            >
              <LocalProxyCard
                title="Authoring"
                isEnabled={this.state.localProxyEnabled.authoring}
                setEnabled={this.setProxyEnabled('loi.authoring', 'https://localhost:5173/')}
              />
              <LocalProxyCard
                title="Course"
                isEnabled={this.state.localProxyEnabled.course}
                setEnabled={this.setProxyEnabled('loi.courseware', 'https://localhost:5174/')}
              />
            </Form>
          )}
          <Treebeard
            style={style}
            data={data}
            decorators={decorators}
            onToggle={this.onRowClick}
          />
        </div>
      </React.Fragment>
    );
  }
}

App.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  clusterType: PropTypes.string,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
    adminPageError: state.main.adminPageError,
    adminPageMessage: state.main.adminPageMessage,
    adminPageSuccess: state.main.adminPageSuccess,
    clusterType: state.main.lo_platform.clusterType,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
