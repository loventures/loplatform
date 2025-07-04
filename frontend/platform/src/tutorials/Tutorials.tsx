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

import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Alert, Button, Form, FormGroup, FormText, Input, Label } from 'reactstrap';

import { AdminPage } from '../adminPortal/types';
import AdminFormTitle from '../components/adminForm/AdminFormTitle';
import * as TutorialApi from './TutorialApi';
import { getTutorials } from './TutorialApi';
import { IoInformationCircleOutline } from 'react-icons/io5';

const Tutorials: React.FC & AdminPage = () => {
  const translations = useSelector<any, Polyglot>(state => state.main.translations);
  const [tutorials, setTutorials] = useState('');
  const [validJson, setValidJson] = useState(true);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const submitBtnLabel = saved ? 'saved' : 'save';

  useEffect(() => {
    getTutorials().then(tut => setTutorials(JSON.stringify(tut, null, 2)));
  }, []);

  const handleSubmit = (event: React.FormEvent<HTMLButtonElement>): void => {
    const parsedTuts = JSON.parse(tutorials);
    TutorialApi.setTutorials(parsedTuts).then(() => setSaved(true));
    event.preventDefault();
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setTutorials(event.target.value);
    setSaved(false);

    try {
      const json = JSON.parse(event.target.value);
      if (typeof json !== 'object') throw Error('Not an object');
      Object.entries(json).forEach(([k, v]) => {
        if (typeof v !== 'object' || !Array.isArray((v as any).steps))
          throw Error(`Key ${k} missing steps.`);
      });
      setValidJson(true);
    } catch (e) {
      setValidJson(false);
      setError((e as any).message);
    }
  };

  return (
    <Form className="container">
      <FormGroup>
        <AdminFormTitle title={translations.t('adminPage.tutorials.name')} />

        <Label for="tutorials">{translations.t('adminPage.tutorials.label.tutorials')}</Label>
        <FormText className="d-block">
          {translations.t('adminPage.tutorials.text.tutorials')}
        </FormText>
        <Input
          id="tutorials"
          type="textarea"
          name="tutorials"
          defaultValue={tutorials}
          onChange={handleChange}
          rows={32}
        />
      </FormGroup>
      {!validJson && <Alert color="danger">{error}</Alert>}
      <Button
        className="px-5"
        color="primary"
        onClick={handleSubmit}
        disabled={!tutorials || !validJson || saved}
      >
        {translations.t(`adminPage.tutorials.label.${submitBtnLabel}`)}
      </Button>
    </Form>
  );
};

Tutorials.pageInfo = {
  identifier: 'tutorials',
  icon: IoInformationCircleOutline,
  link: '/Tutorials',
  group: 'domain',
  right: 'loi.cp.admin.right.AdminRight',
};

export default Tutorials;
