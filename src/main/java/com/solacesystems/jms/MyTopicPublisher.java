package com.solacesystems.jms;

import com.solacesystems.jms.impl.Validator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;

class MyTopicPublisher extends SolTopicPublisher {
    private static final Log log = LogFactory.getLog(MyTopicPublisher.class);
    private JmsTopicMapper mapper;

    MyTopicPublisher(SolSessionIF session, Topic topic) throws JMSException {
        super(session, topic);
        mapper = new JmsTopicMapper(topic.getTopicName());
    }

    @Override
    public void publish(Message message) throws JMSException {
        String topicString = mapper.mapTopic(message);
        Topic mappedTopic = super.mSession.createTopic(topicString);
        super.publish(mappedTopic, message);
    }

    @Override
    public void publish(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        String topicString = mapper.mapTopic(message);
        Topic mappedTopic = super.mSession.createTopic(topicString);
        Validator.checkSendDestination(super.mSessionProps.getConnectionProperties().getRouterCapabilities().crMode(), super.mDestination, mappedTopic);
        super.publish(message, deliveryMode, priority, timeToLive);
    }

    @Override
    public void send(Message message) throws JMSException {
        String topicString = mapper.mapTopic(message);
        Topic mappedTopic = super.mSession.createTopic(topicString);
        super.send(mappedTopic, message);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        String topicString = mapper.mapTopic(message);
        Topic mappedTopic = super.mSession.createTopic(topicString);
        super.send(mappedTopic, message, deliveryMode, priority, timeToLive);
    }
}
