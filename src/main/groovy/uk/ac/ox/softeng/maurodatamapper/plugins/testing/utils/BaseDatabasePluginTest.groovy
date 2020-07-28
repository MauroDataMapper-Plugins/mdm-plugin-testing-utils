/*
 * Copyright 2020 University of Oxford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelImporterProviderServiceParameters

import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test
import org.springframework.validation.FieldError

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

/**
 * @since 08/08/2017
 */
@Slf4j
abstract class BaseDatabasePluginTest<P extends DatabaseDataModelImporterProviderServiceParameters,
    T extends AbstractDatabaseDataModelImporterProviderService<P>>
    extends BaseImportPluginTest<DataModel, P, T> {

    protected int databasePort
    protected String databaseHost

    abstract protected P createDatabaseImportParameters()

    abstract protected String getDatabasePortPropertyName()

    abstract protected int getDefaultDatabasePort()

    @Before
    void setupDatabaseSettings() {
        try {
            databasePort = Integer.parseInt(System.getProperty(getDatabasePortPropertyName()))
        } catch (Exception ignored) {
            databasePort = getDefaultDatabasePort()
        }
        databaseHost = 'localhost'
    }

    @Test
    void testImportDatabase() {
        P params = createDatabaseImportParameters(databaseHost, databasePort)
        importDataModelAndRetrieveFromDatabase(params)
    }

    @Override
    DataModel saveDomain(DataModel domain) {
        getBean(DataModelService).saveWithBatching(domain)
    }

    protected P createDatabaseImportParameters(String host, int port) {
        P params = createDatabaseImportParameters()
        params.setDatabaseHost(host)
        params.setDatabasePort(port)
        params.setFinalised(false)
        params.setFolderId(getTestFolder().getId())
        params.setDatabaseSSL(false)
        params.setImportAsNewDocumentationVersion(false)
        params.setDataModelNameSuffix("")
        params
    }

    protected DataModel importDataModelAndRetrieveFromDatabase(P params) {

        def errors = getBean(ImporterService).validateParameters(params, getImporterInstance().importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            errors.allErrors.each {error ->

                String msg = messageSource ? messageSource.getMessage(error, Locale.default) :
                             "${error.defaultMessage} :: ${Arrays.asList(error.arguments)}"

                if (error instanceof FieldError) msg += " :: [${error.field}]"

                log.error msg
                System.err.println msg
            }
            fail('Import parameters are not valid')
        }

        try {
            getImporterInstance().getConnection(params.databaseNames, params)
        } catch (ApiException e) {
            fail(e.getMessage())
        }

        DataModel importedModel = importDomain(params)

        log.debug('Getting datamodel {} from database to verify', importedModel.getId())
        // Rather than use the one returned from the import, we want to check whats actually been saved into the DB
        DataModel dataModel = DataModel.get(importedModel.getId())
        assertNotNull('DataModel should exist in Database', dataModel)
        dataModel
    }
}
