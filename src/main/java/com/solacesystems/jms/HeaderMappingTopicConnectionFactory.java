package com.solacesystems.jms;

import com.solacesystems.jms.impl.Validator;
import com.solacesystems.jms.property.JMSProperties;

import javax.jms.*;

/**
 * Only overloading the bare minimum to produce the example.
 */
class HeaderMappingTopicConnectionFactory extends SolXAConnectionFactoryImpl {

    HeaderMappingTopicConnectionFactory(JMSProperties mJMSProperties) {
        super(mJMSProperties);
    }


    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        Validator.checkClientId(super.mBean.getJNDIClientID(), super.mBean.getClientID());
        return new MyTopicConnection(super.mProperties, false);
    }
}
