package uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils

import uk.ac.ox.softeng.maurodatamapper.core.Application
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.boot.GrailsApp
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
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.orm.hibernate5.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager

import static org.junit.Assert.assertNotNull

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

        System.setProperty(Environment.KEY, 'test')
        System.setProperty('mdm.env', 'plugin.test')
        if (System.getProperty('server.port') == null) System.setProperty('server.port', '8181')

        applicationContext = GrailsApp.run(Application) // TODO(adjl): Investigate and refactor to allow passing of different Applications

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
