package ox.softeng.metadatacatalogue.plugins.test

import ox.softeng.metadatacatalogue.core.feature.Folder
import ox.softeng.metadatacatalogue.core.user.CatalogueUser
import ox.softeng.metadatacatalogue.core.util.DataBootstrap
import ox.softeng.metadatacatalogue.plugins.test.boot.TestGrailsApplication

import grails.boot.GrailsApp
import grails.util.Environment
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.orm.hibernate5.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean

import static org.junit.Assert.assertNotNull

/**
 * @since 08/08/2017
 */
abstract class BasePluginTest implements DataBootstrap {
    protected static ApplicationContext applicationContext
    private static PlatformTransactionManager transactionManager
    private TransactionStatus transactionStatus
    protected CatalogueUser catalogueUser

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            getLogger().warn('--- {} ---', description.getDisplayName())
        }
    }

    @After
    void rollback() {
        if (transactionStatus != null) endTransaction(transactionStatus)
    }

    @Before
    void setup() {
        transactionStatus = beginTransaction()

        catalogueUser = editor
        assertNotNull('We must have a catalogue user', catalogueUser)

        if (!Folder.countByLabel('Test Folder')) {
            checkAndSave(new Folder(label: 'Test Folder', createdBy: editor).addToReadableByUsers(reader2))
        }

        assertNotNull("We must have a test folder", testFolder)
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

        // assume SLF4J is bound to logback in the current environment
        //LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory()
        // print logback's internal status
        // StatusPrinter.print(lc)

        System.setProperty(Environment.KEY, 'test')
        System.setProperty('mc.env', 'plugin.test')
        if (System.getProperty('server.port') == null) System.setProperty('server.port', '8181')

        applicationContext = GrailsApp.run(TestGrailsApplication)

        assertNotNull('We must have an applicationContext', applicationContext)

        HibernateDatastore hibernateDatastore = applicationContext.getBean(HibernateDatastore)

        assertNotNull('We must have a hibernateDatastore', hibernateDatastore)

        TransactionSynchronizationManager.bindResource(hibernateDatastore.getSessionFactory(),
                                                       new SessionHolder(hibernateDatastore.openSession()))

        transactionManager = applicationContext.getBean(PlatformTransactionManager)

        assertNotNull('We must have a transactionManager', hibernateDatastore)

        outputRuntimeArgs()
    }

    @AfterClass
    static void shutdownGorm() throws IOException {
        if (applicationContext != null) GrailsApp.exit(applicationContext)
    }

    @Override
    MessageSource getMessageSource() {
        getBean(MessageSource)
    }

    static void outputRuntimeArgs() {
        Logger logger = LoggerFactory.getLogger(BasePluginTest)
        try {
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean()
            List<String> arguments = runtimeMxBean.getInputArguments()

            logger.warn("Running with JVM args : {}", arguments.size())
            Map<String, String> map = arguments.collectEntries {arg ->
                arg.split('=').toList()
            }.sort() as Map<String, String>

            map.findAll {k, v ->
                k.startsWith('-Denv') ||
                k.startsWith('-Dgrails') ||
                k.startsWith('-Dinfo') ||
                k.startsWith('-Djava.version') ||
                k.startsWith('-Dspring') ||
                k.startsWith('-Duser.timezone') ||
                k.startsWith('-X')
            }.each {k, v ->
                if (v) {
                    println "${k}=${v}"
                    logger.warn('{}={}', k, v)
                } else {
                    println "${k}"
                    logger.warn('{}', k)
                }
            }
        } catch (Exception ex) {
            logger.error('Errr', ex)
        }
    }
}
