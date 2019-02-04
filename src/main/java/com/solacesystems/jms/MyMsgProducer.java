package com.solacesystems.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;

class MyMsgProducer extends SolMessageProducer {
    private static final Log log = LogFactory.getLog(MyMsgProducer.class);
    private JmsTopicMapper mapper;

    MyMsgProducer(SolSessionIF session, Destination destination) throws JMSException {
        super(session, destination);
        mapper = new JmsTopicMapper(destination.toString());
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
