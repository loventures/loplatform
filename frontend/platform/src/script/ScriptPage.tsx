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

import 'codemirror/mode/clike/clike';
import 'codemirror/mode/meta';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/hint/sql-hint';
import 'codemirror/addon/hint/show-hint.css'; // without this css hints won't show

 
import axios from 'axios';
import React, { useEffect, useRef, useState } from 'react';
import CodeMirrorImpl from 'react-codemirror';
import { Link, useLocation } from 'react-router-dom';
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

import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useThunkDispatch, useTranslations } from '../redux/state';
import { ContentTypeURLEncoded } from '../services';
import Console from './react-console';

// react-codemirror's legacy typing models it as a function component, so it
// rejects refs; the runtime component does expose getCodeMirrorInstance().
const CodeMirror = CodeMirrorImpl as unknown as React.ComponentClass<
  React.ComponentProps<typeof CodeMirrorImpl>
>;

type ConsoleHandle = InstanceType<typeof Console>;

interface DomainOption {
  id: number;
  name: string;
}

const titleRegex = /^-t (.*)$/;
const RpcBase = '/control/component/loi.cp.script.ScriptServlet';

const isScala = (language: string) => language.match(/scala/i);
const isSQL = (language: string) => language.match(/sql/i);
const isRedshift = (language: string) => language.match(/redshift/i);

const storageKey = (pathname: string) => {
  switch (pathname) {
    case isSQL(pathname) as unknown as string:
      return 'overlord:script/sql';
    case isScala(pathname) as unknown as string:
      return 'overlord:script/scala';
    case isRedshift(pathname) as unknown as string:
      return 'overlord:script/redshift';
    default:
      return 'overlord:script/scala';
  }
};

const SysScript: React.FC = () => {
  const lo_platform = useLoPlatform();
  const T = useTranslations();
  const dispatch = useThunkDispatch();
  const location = useLocation();

  const pathname = location?.pathname ?? '';
  const [language, setLanguage] = useState(pathname.replace(/.*\//, ''));
  const [code, setCodeState] = useState(
    window.localStorage.getItem(storageKey(pathname)) || ''
  );
  const [status, setStatus] = useState('');
  const [fontSize, setFontSize] = useState('13');
  const [extendedTimeout, setExtendedTimeout] = useState(false);
  const [domains, setDomains] = useState<DomainOption[]>([]);
  const [domainId, setDomainId] = useState('');
  const [hints, setHints] = useState<Record<string, unknown>>({});
  const [help, setHelp] = useState(false);

  const cmRef = useRef<any>(null);
  const consoleRef = useRef<ConsoleHandle>(null);
  const prevPathname = useRef(pathname);

  // Mutable mirrors so the async polling callbacks read live values, as the
  // class component read this.state.* directly.
  const languageRef = useRef(language);
  languageRef.current = language;
  const extendedTimeoutRef = useRef(extendedTimeout);
  extendedTimeoutRef.current = extendedTimeout;
  const domainIdRef = useRef(domainId);
  domainIdRef.current = domainId;

  const welcome = () => {
    const output = consoleRef.current!;
    const lang = languageRef.current;
    const langLabel = isScala(lang) ? 'Scala' : isRedshift(lang) ? 'Redshift' : 'SQL';
    output.log({
      className: 'welcome',
      text: T.t('sysScript.welcome', { ...lo_platform, language: langLabel }),
    });
  };

  useEffect(() => {
    const lang = languageRef.current;
    if (isScala(lang) && (lo_platform.domain as any).type === 'overlord') {
      axios
        .get('/api/v2/domains;order=name:asc;filter=state:eq(Normal)')
        .then(({ data }) => {
          setDomains(data.objects);
        })
        .catch(e => {
          console.log(e);
          dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
        });
    }
    if (isSQL(lang)) {
      axios
        .get(`${RpcBase}/sqlHints`)
        .then(({ data }) => {
          setHints(data);
        })
        .catch(e => {
          console.log(e);
          dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
        });
    }
    welcome();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (prevPathname.current !== pathname) {
      prevPathname.current = pathname;
      const lang = pathname.replace(/.*\//, '');
      const newCode = window.localStorage.getItem(storageKey(lang)) || '';
      setLanguage(lang);
      setCodeState(newCode);
      setTimeout(() => {
        welcome();
        consoleRef.current?.return();
      }, 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  const autoComplete = (cm: any) => {
    if (isSQL(language) || isRedshift(language)) {
      const codeMirror = cmRef.current.getCodeMirrorInstance();
      const hintOptions = {
        tables: hints,
        disableKeywords: false,
        completeSingle: true,
        completeOnSingleClick: false,
      };
      codeMirror.showHint(cm, codeMirror.hint.sql, hintOptions);
    }
  };

  const setCode = (newCode: string) => {
    setCodeState(newCode);
    window.localStorage.setItem(storageKey(languageRef.current), newCode);
  };

  const log = (text: string, className: string) => {
    consoleRef.current!.log({ className, text: text.trimEnd() });
  };

  const onError = (error: unknown) => {
    console.log(error);
    dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
    setStatus('failure');
    consoleRef.current!.return();
  };

  const downloadify = (url: string, fname: string) => {
    const a = document.createElement('a');
    a.download = fname;
    a.innerHTML = 'dl';
    a.href = url;
    a.onclick = event => document.body.removeChild(event.target as Node);
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
  };

  const onResponse = ({ data }: { data: any }) => {
    const lang = languageRef.current;
    if (data.stdlog) {
      log(data.stdlog, 'stdlog');
    }
    if (data.stderr) {
      log(data.stderr, 'stderr');
    }
    if (data.stdout) {
      log(data.stdout, 'stdout');
    }
    if (data.filename) {
      log('Downloading "' + data.filename + '" (' + data.filesize + ' bytes)\n', 'download');
      downloadify(`${RpcBase}/download?language=${lang}`, '');
    }
    if (data.complete) {
      setStatus('success');
      consoleRef.current!.return();
    } else {
      setTimeout(
        () =>
          axios
            .post(`${RpcBase}/poll`, `language=${lang}`, ContentTypeURLEncoded)
            .then(onResponse)
            .catch(onError),
        300
      );
    }
  };

  const onCommand = (cmd: string) => {
    if (/^\s*$/.test(cmd)) {
      consoleRef.current!.return();
    } else if (titleRegex.test(cmd)) {
      const match = titleRegex.exec(cmd)!;
      document.title = match[1];
      consoleRef.current!.return();
    } else {
      const lang = languageRef.current;
      setStatus('busy');
      const data = `language=${lang}&extendedTimeout=${extendedTimeoutRef.current}&domainId=${
        domainIdRef.current
      }&script=${encodeURIComponent(cmd)}`;
      axios
        .post(`${RpcBase}/execute`, data, ContentTypeURLEncoded)
        .then(onResponse)
        .catch(onError);
    }
  };

  const downloadConsole = () => {
    const text = consoleRef.current!.child.container!.innerText;
    const blob = new Blob([text], { type: 'text/plain' });
    const urly = window.URL || (window as any).webkitURL;
    downloadify(urly.createObjectURL(blob), 'sysScript.txt');
  };

  const playCode = () => {
    consoleRef.current!.setBusy();
    log('sc# ' + code, 'play');
    onCommand(code);
  };

  const onFontSize = (e: React.ChangeEvent<HTMLInputElement>) => setFontSize(e.target.value);

  const onExtendedTimeout = (e: React.ChangeEvent<HTMLInputElement>) =>
    setExtendedTimeout(e.target.checked);

  const onDomain = (e: React.ChangeEvent<HTMLInputElement>) => setDomainId(e.target.value);

  const scala = isScala(language);
  const sql = isSQL(language);
  const redshift = isRedshift(language);
  const options = {
    lineNumbers: true,
    mode: scala ? 'text/x-scala' : 'text/x-plsql',
    extraKeys: {
      'Cmd-Enter': playCode,
      Tab: autoComplete,
    },
  };
  const prompt = scala ? 'sc# ' : redshift ? 'rs# ' : 'sql# ';
  const lc = language.match(/^[a-z]*$/);
  const close = () => setHelp(false);
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
                onClick={playCode}
              >
                {T.t('sysScript.execute')}
              </Button>
              <Button
                aria-label={T.t('sysScript.download')}
                onClick={downloadConsole}
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
                onClick={() => setHelp(true)}
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
                  onChange={onDomain}
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
                  defaultValue={undefined}
                  onChange={onExtendedTimeout}
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
              onChange={onFontSize}
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
              ref={cmRef}
              value={code}
              className="sys-script-editor"
              onChange={setCode}
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
              ref={consoleRef}
              promptLabel={prompt}
              handler={onCommand}
            />
          </Col>
        </Row>
      </div>
    </>
  );
};

const onComplete = (words: string[], idx: number) => {
  // just in case someone wants to do something one day
  if (words[idx] === 'learning') {
    return ['learningobjects'];
  } else {
    return [];
  }
};

export default SysScript;
