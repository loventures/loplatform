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

import { ConnectedRouter } from 'connected-react-router';
import { AnnouncementsProvider } from './components/announcements/Announcements';
import ClientScriptingContext from './components/ClientScriptingContext';
import StickiesManager from './components/stickies/StickiesManager';
import { MajickLayoutProvider } from './landmarks/appNavBar/MajickLayoutContext';
import ERAppContainer from './landmarks/ERAppContainer';
import { ERLandmarkProvider } from './landmarks/ERLandmarkProvider';
import { ScrollToTopProvider } from './landmarks/ScrollToTopProvider';
import { QueryClientProvider, queryClient } from './resources/queryClient';
import { courseReduxStore } from './loRedux';
import { TranslationProvider, useTranslation } from './i18n/translationContext';
import { history } from './utilities/history';
import React from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { Helmet } from 'react-helmet';
import { Provider as ReduxProvider } from 'react-redux';
import { ErrorBoundary } from 'react-error-boundary';

import ERAppRoutes from './ERAppRoutes';
import { Alert } from 'reactstrap';

// import $ from 'jquery';
//
// window.$ = window.jQuery = $;

const ERAppRoot: React.FC = () => (
  <ReduxProvider store={courseReduxStore}>
    <ConnectedRouter history={history}>
      <QueryClientProvider client={queryClient}>
        <TranslationProvider>
          <StickiesManager>
            <DndProvider backend={HTML5Backend}>
              <ERLandmarkProvider>
                <ScrollToTopProvider>
                  <MajickLayoutProvider>
                    <AnnouncementsProvider>
                      <ERAppContainer>
                        <ErrorBoundary
                          fallback={
                            <div className="p-4">
                              <Alert color="danger">Something went wrong.</Alert>
                            </div>
                          }
                        >
                          <ERAppRoutes />
                        </ErrorBoundary>
                        <ClientScriptingContext />
                      </ERAppContainer>
                    </AnnouncementsProvider>
                  </MajickLayoutProvider>
                </ScrollToTopProvider>
              </ERLandmarkProvider>
            </DndProvider>
          </StickiesManager>
          <LanguageHelmet />
        </TranslationProvider>
      </QueryClientProvider>
    </ConnectedRouter>
  </ReduxProvider>
);

export default ERAppRoot;

const LanguageHelmet = () => {
  //  If the locale is unknown to the server, we serve English. So
  //       we must ask the translations themselves to tell us who they are.
  // set Helmet to translations.data['IETF Language Tag']
  const translation = useTranslation()('IETF language tag');
  const lang = translation == 'IETF language tag' ? 'en' : translation;
  return (
    <Helmet>
      <html lang={lang} />
    </Helmet>
  );
};
