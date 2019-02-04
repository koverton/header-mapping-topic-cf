package com.solacesystems.jms;

import com.solacesystems.jms.impl.JMSState;
import com.solacesystems.jms.impl.Validator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;

class MyTopicSession extends SolTopicSession implements TopicSession {
    private static final Log log = LogFactory.getLog(MyTopicSession.class);

    MyTopicSession(SolConnectionIF connection, boolean transacted, int acknowledgeMode, JMSState state) throws JMSException {
        super(connection, transacted, acknowledgeMode, state);
    }

    @Override
    public TopicPublisher createPublisher(Topic topic) throws JMSException {

        Validator.checkClosed(super.mState, "TopicSession");
        Validator.checkProducerDestination(super.mSessionProps.getConnectionProperties().getRouterCapabilities().crMode(), topic);
        SolTopicPublisher publisher = new MyTopicPublisher(this, topic);
        super.mProducers.add(publisher);

        return publisher;
    }

    @Override
    public synchronized MessageProducer createProducer(Destination destination) throws JMSException {

        Validator.checkClosed(super.mState, "Session");
        Validator.checkProducerDestination(super.mSessionProps.getConnectionProperties().getRouterCapabilities().crMode(), destination);
        MyMsgProducer prod = new MyMsgProducer(this, destination);
        super.mProducers.add(prod);

        return prod;
    }
}
