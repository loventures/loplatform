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

package com.learningobjects.cpxp.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.service.ServiceException;

public class OperationStatus {
    @JsonProperty
    public final String message;
    @JsonProperty
    public final boolean complete;
    @JsonProperty
    public final boolean error;
    @JsonIgnore
    public final ServiceException exception;

    public OperationStatus(String message, boolean complete, ServiceException exception) {
        this.message = message;
        this.complete = complete;
        this.error = exception != null;
        this.exception = exception;
    }
    public static OperationStatus status(String message) {
        return new OperationStatus(message,false,null);
    }
    public static OperationStatus completed() {
        return new OperationStatus("Complete",true,null);
    }
    public static OperationStatus error(ServiceException error) {
        return new OperationStatus(error.getMessage(),true,error);
    }
    public static OperationStatus error(Exception error) {
        ServiceException wrappedException = new ServiceException("Internal Error", error);
        return error(wrappedException);
    }
}
