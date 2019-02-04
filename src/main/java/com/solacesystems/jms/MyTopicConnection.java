package com.solacesystems.jms;

import com.solacesystems.jms.impl.SessionTransactionType;
import com.solacesystems.jms.impl.Validator;
import com.solacesystems.jms.property.JMSProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;

class MyTopicConnection extends SolTopicConnection implements TopicConnection {
    private static final Log log = LogFactory.getLog(MyTopicConnection.class);

    MyTopicConnection(JMSProperties properties, boolean connectionTypeXA, String username, String password) throws JMSException {
        super(properties, connectionTypeXA, username, password);
    }

    MyTopicConnection(JMSProperties properties, boolean connectionTypeXA) throws JMSException {
        super(properties, connectionTypeXA);
    }

    @Override
    public synchronized TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {

        super.checkClosed();
        Validator.checkTransactedAndAckMode(transacted ? SessionTransactionType.LocalTransaction : SessionTransactionType.NoTransaction, acknowledgeMode, super.mConnectionProps.getPropertyBean().getDirectTransport());
        Validator.checkTransactedAndLargeMessaging(transacted ? SessionTransactionType.LocalTransaction : SessionTransactionType.NoTransaction, super.mJCSMPProperties.getBooleanProperty("large_messaging"));
        super.mHasBeenAccessed = true;
        SolTopicSession newSession = new MyTopicSession(this, transacted, acknowledgeMode, super.mState);
        super.mSessions.add(newSession);

        return newSession;
    }
}
