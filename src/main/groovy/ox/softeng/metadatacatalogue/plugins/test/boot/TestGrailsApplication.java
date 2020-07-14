package ox.softeng.metadatacatalogue.plugins.test.boot;

import ox.softeng.metadatacatalogue.core.Application;

import grails.boot.config.GrailsApplicationPostProcessor;

/**
 * @since 22/11/2017
 */
public class TestGrailsApplication extends Application {

    @Override
    public GrailsApplicationPostProcessor grailsApplicationPostProcessor() {
        return new GrailsApplicationPostProcessor(this, getApplicationContext(), classes().toArray(new Class[classes().size()]));
    }
}
