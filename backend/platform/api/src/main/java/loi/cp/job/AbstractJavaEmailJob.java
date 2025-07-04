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

package loi.cp.job;

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.email.EmailService;
import com.learningobjects.cpxp.service.facade.FacadeService;

import javax.inject.Inject;

/**
 * A class containing boiler-plate logic for email jobs written in Java.
 * Extending classes need only implement "public GeneratedReport generateReport()"
 */
public abstract class AbstractJavaEmailJob<T extends EmailJob<T>> extends AbstractEmailJob<T> {

    @Instance
    private EmailJobFacade _self;

    @Inject
    private EmailService _emailService;

    @Inject
    private FacadeService _facadeService;

    @Infer
    private ComponentInstance _componentInstance;

    @Override
    public EmailJobFacade self() {
        return _self;
    }

    @Override
    public EmailService es() {
        return _emailService;
    }

    @Override
    public FacadeService fs() {
        return _facadeService;
    }

    @Override
    public ComponentInstance componentInstance() {
        return _componentInstance;
    }

    @Override
    public <J extends ComponentInterface> J asComponent(Class<J> iface, Object... args){
        return (_componentInstance == null) ? null : _componentInstance.getInstance(iface, args);
    }

}
