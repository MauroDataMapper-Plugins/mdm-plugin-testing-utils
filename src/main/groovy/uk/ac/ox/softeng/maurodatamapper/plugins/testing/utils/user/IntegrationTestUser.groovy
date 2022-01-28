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
package uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils.user

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.path.Path
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
    Path getPath() {
        Path.from('cu',emailAddress)
    }

    @Override
    String getDomainType() {
        IntegrationTestUser
    }
}
