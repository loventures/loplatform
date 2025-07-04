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

import 'codemirror/mode/clike/clike';
import 'codemirror/mode/meta';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/hint/sql-hint';
import 'codemirror/addon/hint/show-hint.css'; // without this css hints won't show

/* eslint-disable indent */
import axios from 'axios';
import PropTypes from 'prop-types';
import React from 'react';
import CodeMirror from 'react-codemirror';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import {
  Button,
  ButtonGroup,
  Col,
  FormGroup,
  Input,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Row,
} from 'reactstrap';
import { bindActionCreators } from 'redux';

import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { ContentTypeURLEncoded } from '../services';
import Console from './react-console';

const titleRegex = /^-t (.*)$/;
const RpcBase = '/control/component/loi.cp.script.ScriptServlet';

const isScala = language => language.match(/scala/i);
const isSQL = language => language.match(/sql/i);
const isRedshift = language => language.match(/redshift/i);

const storageKey = pathname => {
  switch (pathname) {
    case isSQL(pathname):
      return 'overlord:script/sql';
    case isScala(pathname):
      return 'overlord:script/scala';
    case isRedshift(pathname):
      return 'overlord:script/redshift';
    default:
      return 'overlord:script/scala';
  }
};

class SysScript extends React.Component {
  state = {
    language: this.props.location.pathname.replace(/.*\//, ''),
    code: window.localStorage.getItem(storageKey(this.props.location.pathname)) || '',
    status: '',
    fontSize: '13',
    extendedTimeout: false,
    domains: [],
    domainId: '',
    hints: {},
    help: false,
  };

  componentDidMount() {
    const {
      state: { language },
      props: {
        lo_platform: { domain },
        translations: T,
        setPortalAlertStatus,
      },
    } = this;
    if (isScala(language) && domain.type === 'overlord') {
      axios
        .get('/api/v2/domains;order=name:asc;filter=state:eq(Normal)')
        .then(({ data }) => {
          this.setState({ domains: data.objects });
        })
        .catch(e => {
          console.log(e);
          setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
        });
    }
    if (isSQL(language)) {
      axios
        .get(`${RpcBase}/sqlHints`)
        .then(({ data }) => {
          this.setState({ hints: data });
        })
        .catch(e => {
          console.log(e);
          setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
        });
    }
    this.welcome();
  }

  componentDidUpdate(prevProps) {
    const {
      console: output,
      props: { location },
    } = this;
    if (prevProps.location.pathname !== location.pathname) {
      const language = location.pathname.replace(/.*\//, '');
      const code = window.localStorage.getItem(storageKey(language)) || '';
      this.setState({ language, code });
      setTimeout(() => {
        this.welcome();
        output.return();
      }, 0);
    }
  }

  welcome = () => {
    const {
      console: output,
      props: { lo_platform, translations: T },
      state: { language: lang },
    } = this;
    const language = isScala(lang) ? 'Scala' : isRedshift(lang) ? 'Redshift' : 'SQL';
    output.log({
      className: 'welcome',
      text: T.t('sysScript.welcome', { ...lo_platform, language }),
    });
  };

  autoComplete = cm => {
    if (isSQL(this.state.language) || isRedshift(this.state.language)) {
      const codeMirror = this.cm.getCodeMirrorInstance();
      const hintOptions = {
        tables: this.state.hints,
        disableKeywords: false,
        completeSingle: true,
        completeOnSingleClick: false,
      };
      codeMirror.showHint(cm, codeMirror.hint.sql, hintOptions);
    }
  };

  setCode = code => {
    this.setState({ code });
    window.localStorage.setItem(storageKey(this.state.language), code);
  };

  log = (text, className) => {
    this.console.log({ className, text: text.trimEnd() });
  };

  onResponse = ({ data }) => {
    const {
      state: { language },
    } = this;
    if (data.stdlog) {
      this.log(data.stdlog, 'stdlog');
    }
    if (data.stderr) {
      this.log(data.stderr, 'stderr');
    }
    if (data.stdout) {
      this.log(data.stdout, 'stdout');
    }
    if (data.filename) {
      this.log('Downloading "' + data.filename + '" (' + data.filesize + ' bytes)\n', 'download');
      this.downloadify(`${RpcBase}/download?language=${language}`, '');
    }
    if (data.complete) {
      this.setState({ status: 'success' });
      this.console.return();
    } else {
      setTimeout(
        () =>
          axios
            .post(`${RpcBase}/poll`, `language=${language}`, ContentTypeURLEncoded)
            .then(this.onResponse)
            .catch(this.onError),
        300
      );
    }
  };

  onError = error => {
    console.log(error);
    const {
      console: output,
      props: { translations: T, setPortalAlertStatus },
    } = this;
    setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
    this.setState({ status: 'failure' });
    output.return();
  };

  onCommand = cmd => {
    if (/^\s*$/.test(cmd)) {
      this.console.return();
    } else if (titleRegex.test(cmd)) {
      const match = titleRegex.exec(cmd);
      document.title = match[1];
      this.console.return();
    } else {
      const { extendedTimeout, domainId, language } = this.state;
      this.setState({ status: 'busy' });
      const data = `language=${language}&extendedTimeout=${extendedTimeout}&domainId=${domainId}&script=${encodeURIComponent(
        cmd
      )}`;
      axios
        .post(`${RpcBase}/execute`, data, ContentTypeURLEncoded)
        .then(this.onResponse)
        .catch(this.onError);
    }
  };

  downloadify = (url, fname) => {
    const a = document.createElement('a');
    a.download = fname;
    a.innerHTML = 'dl';
    a.href = url;
    a.onclick = event => document.body.removeChild(event.target);
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
  };

  downloadConsole = () => {
    const text = this.console.child.container.innerText;
    const blob = new Blob([text], { type: 'text/plain' });
    const urly = window.URL || window.webkitURL;
    this.downloadify(urly.createObjectURL(blob), 'sysScript.txt');
  };

  playCode = () => {
    const {
      state: { code },
    } = this;
    this.console.setBusy();
    this.log('sc# ' + code, 'play');
    this.onCommand(code);
  };

  onFontSize = e => this.setState({ fontSize: e.target.value });

  onExtendedTimeout = e => this.setState({ extendedTimeout: e.target.checked });

  onDomain = e => this.setState({ domainId: e.target.value });

  render() {
    const {
      props: { lo_platform, translations: T },
      state: { code, domains, fontSize, help, language, status },
    } = this;
    const scala = isScala(language);
    const sql = isSQL(language);
    const redshift = isRedshift(language);
    const options = {
      lineNumbers: true,
      mode: scala ? 'text/x-scala' : 'text/x-plsql',
      extraKeys: {
        'Cmd-Enter': this.playCode,
        Tab: this.autoComplete,
      },
    };
    const onComplete = (words, idx) => {
      // just in case someone wants to do something one day
      if (words[idx] === 'learning') {
        return ['learningobjects'];
      } else {
        return [];
      }
    };
    const prompt = scala ? 'sc# ' : redshift ? 'rs# ' : 'sql# ';
    const lc = language.match(/^[a-z]*$/);
    const close = () => this.setState({ help: false });
    return (
      <>
        {help && (
          <Modal
            id="script-help-modal"
            isOpen={true}
            size="lg"
            toggle={close}
          >
            <ModalHeader tag="h2">Please Help Me</ModalHeader>
            <ModalBody>
              <h3>Editor</h3>
              <p>
                <strong>Control-Enter</strong> or <strong>Command-Enter</strong> executes the
                script.
                <br />
                <strong>Tab</strong> autocompletes keywords and table names in SQL mode. If your are
                on the <code>.</code> after an expression such as{' '}
                <code>SELECT * FROM UserFinder u WHERE u.</code> then it will autocomplete column
                names.
              </p>
              <h3>Console</h3>
              <p>
                Quite a few of the usual control keys, such as <strong>Control-L</strong> to clear
                the output, <strong>Control-U</strong> to clear the input line etc.
                <br />
                <code>-t newtitle</code> to change the window title.
                <br />
                <code>\d table</code> to describe a table.
                <br />
                <code>\download SELECT * FROM ...</code> to download results.
                <br />
                <code>\?</code> for more help.
                <br />
                In Scala, evaluate a <code>java.io.File</code> to download the corresponding file.
              </p>
            </ModalBody>
            <ModalFooter>
              <Button
                color="secondary"
                onClick={close}
              >
                Close
              </Button>
            </ModalFooter>
          </Modal>
        )}
        <div className="container-fluid sys-script">
          <Row>
            <Col className="d-flex align-items-center">
              <ButtonGroup className="actionButtons">
                <Button
                  color="success"
                  disabled={code === ''}
                  onClick={this.playCode}
                >
                  {T.t('sysScript.execute')}
                </Button>
                <Button
                  aria-label={T.t('sysScript.download')}
                  onClick={this.downloadConsole}
                >
                  <i
                    className="material-icons md-18"
                    aria-hidden="true"
                  >
                    file_download
                  </i>
                </Button>
                <Button
                  aria-label={T.t('sysScript.help')}
                  onClick={() => this.setState({ help: true })}
                >
                  <i
                    className="material-icons md-18"
                    aria-hidden="true"
                  >
                    info
                  </i>
                </Button>
              </ButtonGroup>
              {!!domains.length && (
                <>
                  <Label
                    id="script-domain-label"
                    for="script-font-size"
                    className="ms-3 mb-3"
                  >
                    {T.t('sysScript.domain')}
                  </Label>
                  <Input
                    id="script-domain"
                    type="select"
                    name="domain"
                    className="me-3 mb-3"
                    onChange={this.onDomain}
                  >
                    <option
                      key={lo_platform.domain.id}
                      value={lo_platform.domain.id}
                    >
                      {lo_platform.domain.name}
                    </option>
                    {domains.map(d => (
                      <option
                        key={d.id}
                        value={d.id}
                      >
                        {d.name}
                      </option>
                    ))}
                  </Input>
                </>
              )}
              {sql && (
                <FormGroup
                  switch
                  inline
                  className="mb-3 ms-3"
                >
                  <Input
                    type="switch"
                    role="switch"
                    id="script-time-out"
                    name="timeout"
                    defaultValue={false}
                    onChange={this.onExtendedTimeout}
                  />
                  <Label for="script-time-out">{T.t('sysScript.timeOut')}</Label>
                </FormGroup>
              )}
              <Label
                id="script-font-size-label"
                for="script-font-size"
                className="mb-3"
              >
                {T.t('sysScript.fontSize')}
              </Label>
              <Input
                id="script-font-size"
                type="range"
                className="font-size mb-3 me-3"
                name="customRange"
                min="8"
                max="18"
                defaultValue={fontSize}
                onChange={this.onFontSize}
              />
              <ButtonGroup className="actionButtons ms-auto">
                <Link
                  className={`btn btn-${scala ? 'primary' : 'secondary'}`}
                  to={lc ? 'scala' : 'Scala'}
                >
                  Scala
                </Link>
                <Link
                  className={`btn btn-${sql ? 'primary' : 'secondary'}`}
                  to={lc ? 'sql' : 'SQL'}
                >
                  SQL
                </Link>
                <Link
                  className={`btn btn-${redshift ? 'primary' : 'secondary'}`}
                  to={lc ? 'redshift' : 'Redshift'}
                >
                  Redshift
                </Link>
              </ButtonGroup>
              <i
                className={`material-icons mb-3 md-24 cloudy ${status}`}
                aria-hidden="true"
              >
                cloud
              </i>
            </Col>
          </Row>
          <Row className="g-0">
            <Col
              lg={6}
              className="pe-2"
              style={{ fontSize: fontSize + 'px' }}
            >
              <CodeMirror
                key={`cm-${language}`}
                ref={cm => (this.cm = cm)}
                value={code}
                className="sys-script-editor"
                onChange={this.setCode}
                options={options}
              />
            </Col>
            <Col
              lg={6}
              className="ps-2"
              style={{ fontSize: fontSize + 'px' }}
            >
              <Console
                complete={onComplete}
                ref={c => (this.console = c)}
                promptLabel={prompt}
                handler={this.onCommand}
              />
            </Col>
          </Row>
        </div>
      </>
    );
  }
}

SysScript.propTypes = {
  lo_platform: LoPropTypes.lo_platform,
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  location: PropTypes.object.isRequired,
};

function mapStateToProps(state) {
  return {
    lo_platform: state.main.lo_platform,
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(SysScript);
