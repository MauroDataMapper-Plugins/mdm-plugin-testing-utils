package uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils.user

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.User

@Singleton
class IntegrationTestUser implements User {

    String firstName = 'Integration Test'
    String lastName = 'User'
    String emailAddress = StandardEmailAddress.INTEGRATION_TEST
    String tempPassword = ''

    @Override
    UUID getId() {
        UUID.randomUUID()
    }

    UUID ident() {
        id
    }

    @Override
    String getDomainType() {
        IntegrationTestUser
    }
}
