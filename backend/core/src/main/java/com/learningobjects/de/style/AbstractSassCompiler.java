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

package com.learningobjects.de.style;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AbstractSassCompiler {

    private final static Logger logger = Logger.getLogger(AbstractSassCompiler.class.getName());

    protected static final String RELATIVE_OUTPUT_PATH = "out";
    protected static final String RELATIVE_SERVER_IMPORTS_PATH = "server_imports";
    protected static final String SERVER_VARIABLES_FILE_NAME = "_variables.sass";

    protected void writeVariableFile(Map<String, String> variables, String serverImportsPath) throws FileNotFoundException, UnsupportedEncodingException {
        writeVariablesToFile(variables, serverImportsPath, SERVER_VARIABLES_FILE_NAME, new FileVariableWriter() {
            public void writeFile(Map<String, String> vars, PrintWriter fileWriter) {
                for (Map.Entry<String, String> variableEntry : vars.entrySet()) {
                    fileWriter.println("$" + variableEntry.getKey() + ": " + variableEntry.getValue());
                }
            }
        });
    }


    protected File writeVariablesToFile(Map<String, String> vars, String outputFolder, String fileName, FileVariableWriter writer) throws FileNotFoundException, UnsupportedEncodingException {
        if(vars == null) {
            vars = new HashMap<>();
        }
        File folder = new File(outputFolder);
        if(!folder.exists()){
            folder.mkdir();
        }
        String outputFilePath = outputFolder + File.separator + fileName;
        logger.info("writing variable file in: " + outputFilePath);
        PrintWriter variablesWriter = new PrintWriter(outputFilePath, "UTF-8");
        writer.writeFile(vars, variablesWriter);
        variablesWriter.close();
        return FileUtils.getFile(outputFilePath);
    }

    protected interface FileVariableWriter {
        public void writeFile(Map<String, String> vars, PrintWriter fileWriter);
    }
}
