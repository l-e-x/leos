package eu.europa.ec.leos.services.messaging.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

import javax.jms.ConnectionFactory;

import static java.util.concurrent.TimeUnit.DAYS;

@Configuration
public class JmsProducerConfig {

    private static final long TIME_TO_LIVE = DAYS.toMillis(7);

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setTimeToLive(TIME_TO_LIVE);
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setMessageConverter(messageConverter);
        return jmsTemplate;
    }

}
