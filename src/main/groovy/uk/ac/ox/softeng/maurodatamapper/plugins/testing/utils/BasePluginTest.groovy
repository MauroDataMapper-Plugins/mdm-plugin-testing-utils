/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils.Application
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import grails.boot.GrailsApp
import grails.core.GrailsApplication
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.transaction.GrailsTransactionAttribute
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.orm.hibernate5.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * @since 08/08/2017
 */
@Slf4j
abstract class BasePluginTest {

    protected static ApplicationContext applicationContext
    private static PlatformTransactionManager transactionManager
    private TransactionStatus transactionStatus

    Folder testFolder

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.warn('--- {} ---', description.getDisplayName())
        }
    }

    @After
    void rollback() {
        if (transactionStatus != null) endTransaction(transactionStatus)
    }

    @Before
    void setup() {
        transactionStatus = beginTransaction()

        testFolder = Folder.findByLabel('Plugin Test Folder')
        if (!testFolder) {
            testFolder = new Folder(label: 'Plugin Test Folder', createdBy: StandardEmailAddress.INTEGRATION_TEST)
            GormUtils.checkAndSave(getMessageSource(), testFolder)
        }

        assertNotNull("We must have a test folder", testFolder)

        GrailsApplication grailsApplication = getBean(GrailsApplication)
        AuthorityService authorityService = getBean(AuthorityService)

        if (!authorityService.defaultAuthorityExists()) {
            Authority authority = new Authority(label: grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY),
                                                url: grailsApplication.config.getProperty(Authority.DEFAULT_URL_CONFIG_PROPERTY),
                                                createdBy: StandardEmailAddress.ADMIN,
                                                readableByEveryone: true,
                                                defaultAuthority: true)
            GormUtils.checkAndSave(getMessageSource(), authority)
        }
        assertTrue("We must have a default authority folder", authorityService.defaultAuthorityExists())
    }

    MessageSource getMessageSource() {
        getBean(MessageSource)
    }

    protected <T> T getBean(Class<T> beanClass) {
        applicationContext.getBean(beanClass)
    }

    private TransactionStatus beginTransaction() {
        transactionManager.getTransaction(new GrailsTransactionAttribute())
    }

    private void endTransaction(TransactionStatus transactionStatus) {
        transactionManager.rollback(transactionStatus)
    }

    @BeforeClass
    static void setupGorm() {
        final LoggerContext loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        StatusPrinter.print loggerContext // Print Logback's internal status

        System.setProperty(Environment.KEY, 'test')
        System.setProperty('mdm.env', 'plugin.test')
        if (System.getProperty('server.port') == null) System.setProperty('server.port', '8181')

        applicationContext = GrailsApp.run(Application)

        assertNotNull('We must have an applicationContext', applicationContext)

        HibernateDatastore hibernateDatastore = applicationContext.getBean(HibernateDatastore)

        assertNotNull('We must have a hibernateDatastore', hibernateDatastore)

        TransactionSynchronizationManager.bindResource(hibernateDatastore.getSessionFactory(),
                                                       new SessionHolder(hibernateDatastore.openSession()))

        transactionManager = applicationContext.getBean(PlatformTransactionManager)

        assertNotNull('We must have a transactionManager', hibernateDatastore)

        Utils.outputRuntimeArgs(getClass())
    }

    @AfterClass
    static void shutdownGorm() throws IOException {
        if (applicationContext != null) GrailsApp.exit(applicationContext)
    }
}
