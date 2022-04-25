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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.database.summarymetadata.DateIntervalHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.database.summarymetadata.DecimalIntervalHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.database.summarymetadata.IntegerIntervalHelper

import grails.util.Pair
import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test
import org.springframework.validation.FieldError

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import static org.junit.Assert.assertEquals
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

    @Test
    void testIntegerIntervalHelper() {
        //Simple interval
        IntegerIntervalHelper iih = new IntegerIntervalHelper(1, 500)
        assertEquals 100, iih.intervalLength
        LinkedHashMap expectedIntervals = new LinkedHashMap()
        expectedIntervals['0 - 100'] = new Pair(0, 100)
        expectedIntervals['100 - 200'] = new Pair(100, 200)
        expectedIntervals['200 - 300'] = new Pair(200, 300)
        expectedIntervals['300 - 400'] = new Pair(300, 400)
        expectedIntervals['400 - 500'] = new Pair(400, 500)
        expectedIntervals['500 - 600'] = new Pair(500, 600)
        assertEquals expectedIntervals, iih.intervals

        //Negative minimum left of boundary
        iih = new IntegerIntervalHelper(-30000001, 19999999)
        assertEquals 10000000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-40000000 - -30000000'] = new Pair(-40000000, -30000000)
        expectedIntervals['-30000000 - -20000000'] = new Pair(-30000000, -20000000)
        expectedIntervals['-20000000 - -10000000'] = new Pair(-20000000, -10000000)
        expectedIntervals['-10000000 - 0'] = new Pair(-10000000, 0)
        expectedIntervals['0 - 10000000'] = new Pair(0, 10000000)
        expectedIntervals['10000000 - 20000000'] = new Pair(10000000, 20000000)
        assertEquals expectedIntervals, iih.intervals

        //Negative minimum on boundary
        iih = new IntegerIntervalHelper(-30000000, 19999999)
        assertEquals 10000000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-30000000 - -20000000'] = new Pair(-30000000, -20000000)
        expectedIntervals['-20000000 - -10000000'] = new Pair(-20000000, -10000000)
        expectedIntervals['-10000000 - 0'] = new Pair(-10000000, 0)
        expectedIntervals['0 - 10000000'] = new Pair(0, 10000000)
        expectedIntervals['10000000 - 20000000'] = new Pair(10000000, 20000000)
        assertEquals expectedIntervals, iih.intervals

        //Negative minimum right of boundary
        iih = new IntegerIntervalHelper(-29999999, 19999999)
        assertEquals 10000000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-30000000 - -20000000'] = new Pair(-30000000, -20000000)
        expectedIntervals['-20000000 - -10000000'] = new Pair(-20000000, -10000000)
        expectedIntervals['-10000000 - 0'] = new Pair(-10000000, 0)
        expectedIntervals['0 - 10000000'] = new Pair(0, 10000000)
        expectedIntervals['10000000 - 20000000'] = new Pair(10000000, 20000000)
        assertEquals expectedIntervals, iih.intervals

        //Negative max, left of boundary
        iih = new IntegerIntervalHelper(-5100, -1001)
        assertEquals 1000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-6000 - -5000'] = new Pair(-6000, -5000)
        expectedIntervals['-5000 - -4000'] = new Pair(-5000, -4000)
        expectedIntervals['-4000 - -3000'] = new Pair(-4000, -3000)
        expectedIntervals['-3000 - -2000'] = new Pair(-3000, -2000)
        expectedIntervals['-2000 - -1000'] = new Pair(-2000, -1000)
        assertEquals expectedIntervals, iih.intervals

        //Negative mae, onboundary
        iih = new IntegerIntervalHelper(-5100, -1000)
        assertEquals 1000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-6000 - -5000'] = new Pair(-6000, -5000)
        expectedIntervals['-5000 - -4000'] = new Pair(-5000, -4000)
        expectedIntervals['-4000 - -3000'] = new Pair(-4000, -3000)
        expectedIntervals['-3000 - -2000'] = new Pair(-3000, -2000)
        expectedIntervals['-2000 - -1000'] = new Pair(-2000, -1000)
        expectedIntervals['-1000 - 0'] = new Pair(-1000, 0)
        assertEquals expectedIntervals, iih.intervals

        //Negative max, right of boundary
        iih = new IntegerIntervalHelper(-5100, -999)
        assertEquals 1000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-6000 - -5000'] = new Pair(-6000, -5000)
        expectedIntervals['-5000 - -4000'] = new Pair(-5000, -4000)
        expectedIntervals['-4000 - -3000'] = new Pair(-4000, -3000)
        expectedIntervals['-3000 - -2000'] = new Pair(-3000, -2000)
        expectedIntervals['-2000 - -1000'] = new Pair(-2000, -1000)
        expectedIntervals['-1000 - 0'] = new Pair(-1000, 0)
        assertEquals expectedIntervals, iih.intervals

        //Zero interval
        iih = new IntegerIntervalHelper(83, 83)
        assertEquals 1, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['83 - 84'] = new Pair(83, 84)
        assertEquals expectedIntervals, iih.intervals

        //Positive min and max, both left of boundary
        iih = new IntegerIntervalHelper(999, 5999)
        assertEquals 1000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['0 - 1000'] = new Pair(0, 1000)
        expectedIntervals['1000 - 2000'] = new Pair(1000, 2000)
        expectedIntervals['2000 - 3000'] = new Pair(2000, 3000)
        expectedIntervals['3000 - 4000'] = new Pair(3000, 4000)
        expectedIntervals['4000 - 5000'] = new Pair(4000, 5000)
        expectedIntervals['5000 - 6000'] = new Pair(5000, 6000)
        assertEquals expectedIntervals, iih.intervals

        //Positive min and max, both on boundary
        iih = new IntegerIntervalHelper(1000, 6000)
        assertEquals 1000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['1000 - 2000'] = new Pair(1000, 2000)
        expectedIntervals['2000 - 3000'] = new Pair(2000, 3000)
        expectedIntervals['3000 - 4000'] = new Pair(3000, 4000)
        expectedIntervals['4000 - 5000'] = new Pair(4000, 5000)
        expectedIntervals['5000 - 6000'] = new Pair(5000, 6000)
        expectedIntervals['6000 - 7000'] = new Pair(6000, 7000)
        assertEquals expectedIntervals, iih.intervals

        //Positive min and max, both right of boundary
        iih = new IntegerIntervalHelper(1001, 6001)
        assertEquals 1000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['1000 - 2000'] = new Pair(1000, 2000)
        expectedIntervals['2000 - 3000'] = new Pair(2000, 3000)
        expectedIntervals['3000 - 4000'] = new Pair(3000, 4000)
        expectedIntervals['4000 - 5000'] = new Pair(4000, 5000)
        expectedIntervals['5000 - 6000'] = new Pair(5000, 6000)
        expectedIntervals['6000 - 7000'] = new Pair(6000, 7000)
        assertEquals expectedIntervals, iih.intervals

        //Beyond defined intervals
        iih = new IntegerIntervalHelper(123, 558000000)
        assertEquals 100000000, iih.intervalLength
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['0 - 100000000'] = new Pair(0, 100000000)
        expectedIntervals['100000000 - 200000000'] = new Pair(100000000, 200000000)
        expectedIntervals['200000000 - 300000000'] = new Pair(200000000, 300000000)
        expectedIntervals['300000000 - 400000000'] = new Pair(300000000, 400000000)
        expectedIntervals['400000000 - 500000000'] = new Pair(400000000, 500000000)
        expectedIntervals['500000000 - 600000000'] = new Pair(500000000, 600000000)
        assertEquals expectedIntervals, iih.intervals
    }

    @Test
    void testDecimalIntervalHelper() {
        //Simple interval
        DecimalIntervalHelper dih = new DecimalIntervalHelper(1.0, 500.0)
        assertEquals 100.0, dih.intervalLength, 0
        LinkedHashMap expectedIntervals = new LinkedHashMap()
        expectedIntervals['0.0 - 100.0'] = new Pair(0.0, 100.0)
        expectedIntervals['100.0 - 200.0'] = new Pair(100.0, 200.0)
        expectedIntervals['200.0 - 300.0'] = new Pair(200.0, 300.0)
        expectedIntervals['300.0 - 400.0'] = new Pair(300.0, 400.0)
        expectedIntervals['400.0 - 500.0'] = new Pair(400.0, 500.0)
        expectedIntervals['500.0 - 600.0'] = new Pair(500.0, 600.0)
        assertEquals expectedIntervals, dih.intervals

        //Negative minimum left of boundary
        dih = new DecimalIntervalHelper(-30000001.0, 19999999.0)
        assertEquals 10000000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-40000000.0 - -30000000.0'] = new Pair(-40000000.0, -30000000.0)
        expectedIntervals['-30000000.0 - -20000000.0'] = new Pair(-30000000.0, -20000000.0)
        expectedIntervals['-20000000.0 - -10000000.0'] = new Pair(-20000000.0, -10000000.0)
        expectedIntervals['-10000000.0 - 0.0'] = new Pair(-10000000.0, 0.0)
        expectedIntervals['0.0 - 10000000.0'] = new Pair(0.0, 10000000.0)
        expectedIntervals['10000000.0 - 20000000.0'] = new Pair(10000000.0, 20000000.0)
        assertEquals expectedIntervals, dih.intervals

        //Negative minimum on boundary
        dih = new DecimalIntervalHelper(-30000000.0, 19999999.0)
        assertEquals 10000000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-30000000.0 - -20000000.0'] = new Pair(-30000000.0, -20000000.0)
        expectedIntervals['-20000000.0 - -10000000.0'] = new Pair(-20000000.0, -10000000.0)
        expectedIntervals['-10000000.0 - 0.0'] = new Pair(-10000000.0, 0.0)
        expectedIntervals['0.0 - 10000000.0'] = new Pair(0.0, 10000000.0)
        expectedIntervals['10000000.0 - 20000000.0'] = new Pair(10000000.0, 20000000.0)
        assertEquals expectedIntervals, dih.intervals

        //Negative minimum right of boundary
        dih = new DecimalIntervalHelper(-29999999.0, 19999999.0)
        assertEquals 10000000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-30000000.0 - -20000000.0'] = new Pair(-30000000.0, -20000000.0)
        expectedIntervals['-20000000.0 - -10000000.0'] = new Pair(-20000000.0, -10000000.0)
        expectedIntervals['-10000000.0 - 0.0'] = new Pair(-10000000.0, 0.0)
        expectedIntervals['0.0 - 10000000.0'] = new Pair(0.0, 10000000.0)
        expectedIntervals['10000000.0 - 20000000.0'] = new Pair(10000000.0, 20000000.0)
        assertEquals expectedIntervals, dih.intervals

        //Negative max, left of boundary
        dih = new DecimalIntervalHelper(-5100.0, -1001.0)
        assertEquals 1000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-6000.0 - -5000.0'] = new Pair(-6000.0, -5000.0)
        expectedIntervals['-5000.0 - -4000.0'] = new Pair(-5000.0, -4000.0)
        expectedIntervals['-4000.0 - -3000.0'] = new Pair(-4000.0, -3000.0)
        expectedIntervals['-3000.0 - -2000.0'] = new Pair(-3000.0, -2000.0)
        expectedIntervals['-2000.0 - -1000.0'] = new Pair(-2000.0, -1000.0)
        assertEquals expectedIntervals, dih.intervals

        //Negative max, onboundary
        dih = new DecimalIntervalHelper(-5100.0, -1000.0)
        assertEquals 1000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-6000.0 - -5000.0'] = new Pair(-6000.0, -5000.0)
        expectedIntervals['-5000.0 - -4000.0'] = new Pair(-5000.0, -4000.0)
        expectedIntervals['-4000.0 - -3000.0'] = new Pair(-4000.0, -3000.0)
        expectedIntervals['-3000.0 - -2000.0'] = new Pair(-3000.0, -2000.0)
        expectedIntervals['-2000.0 - -1000.0'] = new Pair(-2000.0, -1000.0)
        expectedIntervals['-1000.0 - 0.0'] = new Pair(-1000.0, 0.0)
        assertEquals expectedIntervals, dih.intervals

        //Negative max, right of boundary
        dih = new DecimalIntervalHelper(-5100.0, -999.0)
        assertEquals 1000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['-6000.0 - -5000.0'] = new Pair(-6000.0, -5000.0)
        expectedIntervals['-5000.0 - -4000.0'] = new Pair(-5000.0, -4000.0)
        expectedIntervals['-4000.0 - -3000.0'] = new Pair(-4000.0, -3000.0)
        expectedIntervals['-3000.0 - -2000.0'] = new Pair(-3000.0, -2000.0)
        expectedIntervals['-2000.0 - -1000.0'] = new Pair(-2000.0, -1000.0)
        expectedIntervals['-1000.0 - 0.0'] = new Pair(-1000.0, 0.0)
        assertEquals expectedIntervals, dih.intervals

        //Zero interval
        dih = new DecimalIntervalHelper(83.0, 83.0)
        assertEquals 1.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['83.0 - 84.0'] = new Pair(83.0, 84.0)
        assertEquals expectedIntervals, dih.intervals

        //Positive min and max, both left of boundary
        dih = new DecimalIntervalHelper(999.0, 5999.0)
        assertEquals 1000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['0.0 - 1000.0'] = new Pair(0.0, 1000.0)
        expectedIntervals['1000.0 - 2000.0'] = new Pair(1000.0, 2000.0)
        expectedIntervals['2000.0 - 3000.0'] = new Pair(2000.0, 3000.0)
        expectedIntervals['3000.0 - 4000.0'] = new Pair(3000.0, 4000.0)
        expectedIntervals['4000.0 - 5000.0'] = new Pair(4000.0, 5000.0)
        expectedIntervals['5000.0 - 6000.0'] = new Pair(5000.0, 6000.0)
        assertEquals expectedIntervals, dih.intervals

        //Positive min and max, both on boundary
        dih = new DecimalIntervalHelper(1000.0, 6000.0)
        assertEquals 1000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['1000.0 - 2000.0'] = new Pair(1000.0, 2000.0)
        expectedIntervals['2000.0 - 3000.0'] = new Pair(2000.0, 3000.0)
        expectedIntervals['3000.0 - 4000.0'] = new Pair(3000.0, 4000.0)
        expectedIntervals['4000.0 - 5000.0'] = new Pair(4000.0, 5000.0)
        expectedIntervals['5000.0 - 6000.0'] = new Pair(5000.0, 6000.0)
        expectedIntervals['6000.0 - 7000.0'] = new Pair(6000.0, 7000.0)
        assertEquals expectedIntervals, dih.intervals

        //Positive min and max, both right of boundary
        dih = new DecimalIntervalHelper(1001.0, 6001.0)
        assertEquals 1000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['1000.0 - 2000.0'] = new Pair(1000.0, 2000.0)
        expectedIntervals['2000.0 - 3000.0'] = new Pair(2000.0, 3000.0)
        expectedIntervals['3000.0 - 4000.0'] = new Pair(3000.0, 4000.0)
        expectedIntervals['4000.0 - 5000.0'] = new Pair(4000.0, 5000.0)
        expectedIntervals['5000.0 - 6000.0'] = new Pair(5000.0, 6000.0)
        expectedIntervals['6000.0 - 7000.0'] = new Pair(6000.0, 7000.0)
        assertEquals expectedIntervals, dih.intervals

        //Beyond defined intervals
        dih = new DecimalIntervalHelper(123.0, 558000000.0)
        assertEquals 100000000.0, dih.intervalLength, 0
        expectedIntervals = new LinkedHashMap()
        expectedIntervals['0.0 - 100000000.0'] = new Pair(0.0, 100000000.0)
        expectedIntervals['100000000.0 - 200000000.0'] = new Pair(100000000.0, 200000000.0)
        expectedIntervals['200000000.0 - 300000000.0'] = new Pair(200000000.0, 300000000.0)
        expectedIntervals['300000000.0 - 400000000.0'] = new Pair(300000000.0, 400000000.0)
        expectedIntervals['400000000.0 - 500000000.0'] = new Pair(400000000.0, 500000000.0)
        expectedIntervals['500000000.0 - 600000000.0'] = new Pair(500000000.0, 600000000.0)
        assertEquals expectedIntervals, dih.intervals
    }

    @Test
    void testDateIntervalHelper() {
        //Simple interval
        LocalDateTime from = LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        LocalDateTime to = LocalDateTime.parse('2019-12-30T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        DateIntervalHelper dih = new DateIntervalHelper(from, to)
        assertEquals 2, dih.intervalLengthSize
        assertEquals ChronoUnit.DAYS, dih.intervalLengthDimension
        TreeMap expectedIntervals = new TreeMap()
        expectedIntervals['01/12/2019 - 03/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-03T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['03/12/2019 - 05/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-03T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-05T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['05/12/2019 - 07/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-05T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-07T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['07/12/2019 - 09/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-07T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-09T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['09/12/2019 - 11/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-09T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-11T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['11/12/2019 - 13/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-11T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-13T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['13/12/2019 - 15/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-13T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-15T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['15/12/2019 - 17/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-15T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-17T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['17/12/2019 - 19/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-17T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-19T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['19/12/2019 - 21/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-19T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-21T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['21/12/2019 - 23/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-21T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-23T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['23/12/2019 - 25/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-23T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-25T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['25/12/2019 - 27/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-25T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-27T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['27/12/2019 - 29/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-27T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-29T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['29/12/2019 - 31/12/2019'] = new Pair(
                LocalDateTime.parse('2019-12-29T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-31T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        assertEquals expectedIntervals, dih.intervals


        //Zero interval
        from = LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        to = LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dih = new DateIntervalHelper(from, to)
        assertEquals 1, dih.intervalLengthSize
        assertEquals ChronoUnit.MINUTES, dih.intervalLengthDimension

        expectedIntervals = new TreeMap()
        expectedIntervals['2019-12-01 00:00:00 - 2019-12-01 00:01:00'] = new Pair(
                LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse('2019-12-01T00:01:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    @Override
    DataModel saveDomain(DataModel domain) {
        getBean(DataModelService).saveModelWithContent(domain)
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
