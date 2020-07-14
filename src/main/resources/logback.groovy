import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

String nonAnsiPattern = '%d{ISO8601} [%10.10thread] %-5level %-40.40logger{39} : %msg%n'

def buildDir = new File('.', 'build').canonicalFile
def logDir = new File(buildDir, 'logs').canonicalFile
if (!logDir) logDir.mkdirs()

String logFileName = buildDir.parentFile.name

String logMsg = "==> Log File available at ${logDir}/${logFileName}.log <=="

println(logMsg)

appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern = nonAnsiPattern
    }

    filter(ThresholdFilter) {
        level = INFO
    }
    filter(HibernateMappingFilter)
    filter HibernateDeprecationFilter
    filter HibernateNarrowingFilter
}

appender("FILE", FileAppender) {
    file = "${logDir}/${logFileName}.log"
    append = true

    encoder(PatternLayoutEncoder) {
        pattern = nonAnsiPattern
    }
    filter(HibernateMappingFilter)
    filter HibernateDeprecationFilter
    filter HibernateNarrowingFilter

    filter(ThresholdFilter) {
        level = TRACE
    }
}

root(INFO, ['STDOUT', 'FILE'])

if (Environment.current != Environment.TEST) {
    logger('ox.softeng', DEBUG)
    logger('db.migration', DEBUG)

    logger('org.springframework.jdbc.core.JdbcTemplate', DEBUG)

    logger('org.apache.lucene', DEBUG)
    logger('org.hibernate.search.fulltext_query', DEBUG)
    logger('org.hibernate.search.batchindexing.impl.BatchIndexingWorkspace', DEBUG)
    // logger('org.hibernate.SQL', DEBUG)
    // logger 'org.hibernate.type', TRACE
}

logger('org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory', ERROR)
logger('org.springframework.context.support.PostProcessorRegistrationDelegate', WARN)
logger('org.hibernate.cache.ehcache.AbstractEhcacheRegionFactory', ERROR)
logger 'org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl', ERROR
logger 'org.hibernate.engine.jdbc.spi.SqlExceptionHelper', ERROR

logger 'org.springframework.mock.web.MockServletContext', ERROR
logger 'StackTrace', OFF

class HibernateMappingFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /.*Specified config option \[importFrom\].*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}

class HibernateDeprecationFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /HHH90000022.*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}

class HibernateNarrowingFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /HHH000179.*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}