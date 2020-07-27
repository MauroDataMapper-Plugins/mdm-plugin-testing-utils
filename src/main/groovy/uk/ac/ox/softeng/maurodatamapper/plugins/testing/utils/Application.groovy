package uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.springframework.context.annotation.ComponentScan

@CompileStatic
@ComponentScan(basePackages = ['uk.ac.ox.softeng.maurodatamapper'])
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        Utils.outputRuntimeArgs(Application)
        GrailsApp.run(Application, args)
    }

}
