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

import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Nav, Navbar } from 'reactstrap';

import Crumb from '../components/crumbRoute';
import NavigationBar from '../components/navigationBar';
import LoginRequired from '../errors/LoginRequired';
import { setLoPlatform, setTranslations } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { getPlatform, getTranslations } from '../services';
import CourseList from './CourseList';
import Profile from './Profile';

const DomainRoutes: React.FC = () => {
  const dispatch = useDispatch();
  const T = useTranslations();
  const lop = useLoPlatform();
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    Promise.all([getPlatform(), getTranslations(window.locale)]).then(
      ([platformRes, translationsRes]) => {
        dispatch(
          setTranslations(
            new Polyglot({
              locale: platformRes.data.domain.locale,
              phrases: translationsRes.data,
            })
          )
        );
        dispatch(setLoPlatform(platformRes.data));
        document.getElementById('domain-loading')?.remove();
        setLoaded(true);
      }
    );
  }, [dispatch]);

  const {
    domain: { name, image },
    user,
  } = lop;
  return !loaded ? (
    <>
      {image && (
        <img
          className="domain-image fade"
          src={image.url}
          alt={name}
          aria-hidden
        />
      )}
      {user && (
        <div id="main-nav-bar">
          <Navbar
            id="main-nav-bar-base"
            light
            expand
            className="navbar-toggleable-xl px-3"
          >
            <Nav />
          </Navbar>
        </div>
      )}
    </>
  ) : (
    <React.Fragment>
      {image && (
        <img
          className="domain-image fade"
          src={image.url}
          alt={name}
          aria-hidden
        />
      )}
      {user ? (
        <React.Fragment>
          <NavigationBar
            nonAdmin
            domainApp
          />
          <Routes>
            <Route
              path="/"
              element={
                <Crumb
                  title={T.t('page.courseList.name')}
                  documentTitle={`${name} - ${T.t('page.courseList.name')}`}
                >
                  <CourseList />
                </Crumb>
              }
            />
            <Route
              path="/Profile"
              element={
                <Crumb
                  title={T.t('page.profile.name')}
                  documentTitle={`${name} - ${T.t('page.profile.name')}`}
                >
                  <Profile />
                </Crumb>
              }
            />
            <Route
              path="*"
              element={
                <Navigate
                  to="/"
                  replace
                />
              }
            />
          </Routes>
        </React.Fragment>
      ) : (
        <LoginRequired
          T={T}
          lo_platform={lop}
        />
      )}
    </React.Fragment>
  );
};

export default DomainRoutes;
