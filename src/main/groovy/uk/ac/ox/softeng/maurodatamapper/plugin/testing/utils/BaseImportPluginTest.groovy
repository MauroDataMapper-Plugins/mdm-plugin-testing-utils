package uk.ac.ox.softeng.maurodatamapper.plugin.testing.utils

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugin.testing.utils.user.IntegrationTestUser
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.hibernate.SessionFactory
import org.springframework.core.GenericTypeResolver

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

/**
 * @since 08/08/2017
 */
@Slf4j
abstract class BaseImportPluginTest<D extends GormEntity, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<D, P>>
    extends BasePluginTest {

    abstract D saveDomain(D domain)

    @SuppressWarnings('unchecked')
    protected T getImporterInstance() {
        Class[] types = GenericTypeResolver.resolveTypeArguments(getClass(), BaseImportPluginTest)

        for (Class clazz : types) {
            if (ImporterProviderService.isAssignableFrom(clazz)) return getBean((Class<T>) clazz)
        }
        null
    }

    protected D importDomain(P params) {
        importDomain(params, true)
    }

    protected D importDomain(P params, boolean validate) {
        SessionFactory sessionFactory = getBean(SessionFactory)
        try {
            T importer = getImporterInstance()
            long startTime = System.currentTimeMillis()

            log.debug('Importing {}', importer.getDisplayName())
            D importedModel = importer.importDomain(IntegrationTestUser.instance, params)

            long endTime = System.currentTimeMillis()
            log.info('Import complete in {}', Utils.getTimeString(endTime - startTime))

            assertNotNull('Domain should be imported', importedModel)

            if (validate) {

                if (importedModel.validate()) saveDomain(importedModel)
                else {
                    GormUtils.outputDomainErrors(getMessageSource(), importedModel)
                    fail('Domain is invalid')
                }
            }

            sessionFactory.getCurrentSession().flush()
            return importedModel
        } finally {
            // sessionFactory.getCurrentSession().clear()
        }
    }

    protected List<D> importDomains(P params, int expectedSize, boolean validate) {
        SessionFactory sessionFactory = getBean(SessionFactory)
        try {
            T importer = getImporterInstance()

            if (!importer.canImportMultipleDomains()) {
                fail("Importer [${importer.getDisplayName()}] cannot handle importing multiple domains")
            }

            long startTime = System.currentTimeMillis()

            log.debug('Importing {}', importer.getDisplayName())
            List<D> importedModels = importer.importDomains(IntegrationTestUser.instance, params)

            long endTime = System.currentTimeMillis()
            log.info('Import complete in {}', Utils.getTimeString(endTime - startTime))

            assertNotNull('Domains should be imported', importedModels)
            assertEquals('Number of domains imported', expectedSize, importedModels.size())

            if (validate) {
                importedModels.each {domain ->
                    if (domain.validate()) saveDomain(domain)
                    else {
                        GormUtils.outputDomainErrors(getMessageSource(), domain)
                        fail('Domain is invalid')
                    }
                }
            }
            sessionFactory.getCurrentSession().flush()
            return importedModels
        } catch (Exception ex) {
            log.error('Something went wrong importing', ex)
            fail(ex.getMessage())
        } finally {
            // sessionFactory.getCurrentSession().clear()
        }
        Collections.emptyList()
    }

    protected List<D> importDomains(P params) {
        importDomains(params, 1)
    }

    protected List<D> importDomains(P params, int expectedSize) {
        importDomains(params, expectedSize, true)
    }
}
