package ox.softeng.metadatacatalogue.plugins.test.boot;

import grails.boot.config.GrailsApplicationPostProcessor;
import grails.core.GrailsApplicationLifeCycle;
import org.grails.config.PropertySourcesConfig;
import org.grails.core.cfg.GroovyConfigPropertySourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.URL;

/**
 * @since 14/11/2017
 */
public class GroovyConfigGrailsApplicationPostProcessor extends GrailsApplicationPostProcessor {

    public static final String APPLICATION_CONFIGURATION_PROPERTIES_PROPERTY_SOURCE = "applicationConfigurationProperties";
    private static final Logger logger = LoggerFactory.getLogger(GroovyConfigGrailsApplicationPostProcessor.class);

    public GroovyConfigGrailsApplicationPostProcessor(GrailsApplicationLifeCycle lifeCycle, ApplicationContext applicationContext, Class[] classes) {
        super(lifeCycle, applicationContext, classes);
    }

    @Override
    protected void loadApplicationConfig() {
        super.loadApplicationConfig();

        PropertySourcesConfig config = (PropertySourcesConfig) getGrailsApplication().getConfig();
        MutablePropertySources propertySources = (MutablePropertySources) config.getPropertySources();


        URL url = getClass().getResource("/application.groovy");

        if (url != null) {

            Resource resource = new UrlResource(url);
            PropertySourceLoader propertySourceLoader = new GroovyConfigPropertySourceLoader();
            PropertySource<?> propertySource = ((GroovyConfigPropertySourceLoader) propertySourceLoader)
                .load("application.groovy", resource, null);

            propertySources.addAfter(APPLICATION_CONFIGURATION_PROPERTIES_PROPERTY_SOURCE, propertySource);
            //  propertySources.addAfter(APPLICATION_CONFIGURATION_PROPERTIES_PROPERTY_SOURCE, prefixedPropertySource);
            config.refresh();
            logger.info("Updated property sources to include properties from application.groovy");

        } else logger.warn("Specified configuration file application.groovy cannot be found");
    }
}
