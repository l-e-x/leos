package eu.europa.ec.leos.services.messaging;

import eu.europa.ec.leos.model.messaging.UpdateInternalReferencesMessage;
import eu.europa.ec.leos.services.messaging.conf.Base64Serializer;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;

import static eu.europa.ec.leos.services.messaging.conf.JmsDestinations.QUEUE_UPDATE_INTERNAL_REFERENCE;

@Component
public class UpdateInternalReferencesProducer {

    private static final Logger logger = LoggerFactory.getLogger(UpdateInternalReferencesProducer.class);
    private static final ActiveMQQueue UPDATE_INTER_REF_QUEUE = new ActiveMQQueue(QUEUE_UPDATE_INTERNAL_REFERENCE);
    private final JmsTemplate jmsTemplate;

    public UpdateInternalReferencesProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void send(UpdateInternalReferencesMessage message) {
        logger.info("Sending message: " + message);
        jmsTemplate.convertAndSend(UPDATE_INTER_REF_QUEUE, message, this::attachAuthenticationContext);
    }

    private Message attachAuthenticationContext(Message message) throws JMSException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String serialized = Base64Serializer.serialize(authentication);
        message.setStringProperty("authcontext", serialized);
        return message;
    }

}
