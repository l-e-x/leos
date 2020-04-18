package eu.europa.ec.leos.services.messaging.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;

import javax.jms.ConnectionFactory;

import static javax.jms.Session.CLIENT_ACKNOWLEDGE;

@EnableJms
@Configuration
public class JmsConsumerConfig {

    @Bean
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> jmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                                                    JmsErrorHandler errorHandler,
                                                                                                    MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setErrorHandler(errorHandler);
        factory.setSessionAcknowledgeMode(CLIENT_ACKNOWLEDGE);

        return factory;
    }

}
