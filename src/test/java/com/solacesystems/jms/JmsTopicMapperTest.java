package com.solacesystems.jms;

import com.solacesystems.jms.message.SolTextMessage;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.junit.Assert.assertEquals;

public class JmsTopicMapperTest {
    private Message getMessage() throws JMSException {
        Message msg = new SolTextMessage();
        msg.setStringProperty("flavor", "Raspberry");
        msg.setIntProperty("count", 5);
        return msg;
    }

    @Test
    public void emptyTest() throws JMSException {
        JmsTopicMapper mapper = new JmsTopicMapper("");

        String tstr = mapper.mapTopic(getMessage());
        assertEquals("", tstr);
    }

    @Test
    public void literalsTest() throws JMSException {
        JmsTopicMapper mapper = new JmsTopicMapper("literal/strings/man");

        assertEquals("literal/strings/man", mapper.mapTopic(getMessage()));
    }

    @Test
    public void fieldNotInMessageTest() throws JMSException {
        JmsTopicMapper mapper = new JmsTopicMapper("literal/[whiskers]/not/there");

        assertEquals("literal/_whiskers_/not/there", mapper.mapTopic(getMessage()));
    }

    @Test
    public void oneFieldTest() throws JMSException {
        JmsTopicMapper mapper = new JmsTopicMapper("[flavor]");

        assertEquals("Raspberry", mapper.mapTopic(getMessage()));
    }

    @Test
    public void oneIntFieldTest() throws JMSException {
        JmsTopicMapper mapper = new JmsTopicMapper("[count]");

        assertEquals("5", mapper.mapTopic(getMessage()));
    }

    @Test
    public void multiFieldTest() throws JMSException {
        JmsTopicMapper mapper = new JmsTopicMapper("literal/[flavor]/[count]");

        assertEquals("literal/Raspberry/5", mapper.mapTopic(getMessage()));
    }
}
