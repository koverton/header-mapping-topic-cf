/**
 *  Copyright 2012-2018 Solace Corporation. All rights reserved.
 *
 *  http://www.solace.com
 *
 *  This source is distributed under the terms and conditions
 *  of any contract or contracts between Solace and you or
 *  your company. If there are no contracts in place use of
 *  this source is not authorized. No support is provided and
 *  no distribution, sharing with others or re-use of this
 *  source is authorized unless specifically stated in the
 *  contracts referred to above.
 *
 * SolJMSHelloWorldPub
 *
 * This sample shows the basics of creating session, connecting a session,
 * and publishing a direct message to a topic. This is meant to be a very
 * basic example for demonstration purposes.
 */

package com.solacesystems.jms.sample;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.solacesystems.jms.SolJmsUtility;
import com.solacesystems.jms.SupportedProperty;

/**
 * Example of an idea I have for a JMS 'Header Mapping TopicConnectionFactory'.
 * The idea is that by allowing expression of header fields in a publisher's
 * topic, then publisher code can publish to these topics and the specialized
 * TopicPublisher code can fill in the appropriate topic variables from the
 * message header.
 *
 * If a header is not found then the literal header name is put into the topic.
 *
 * For example, if the topic is:
 *      transactions/[region]/[acctid]
 *
 * and there is no 'region' header, the resulting topic published to may be
 *      transactions/_region_/937590
 *
 * For topic mapping implementation, @see JmsTopicMapper
 */
public class SampleTopicPublisher {

    public static void main(String... args) throws JMSException, NamingException {
        if (args.length < 4) {
            System.out.println("Usage: SampleTopicPublisher <jndi-provider-url> <vpn> <client-username> <connection-factory>");
            System.exit(-1);
        }
        System.out.println("SolJMSHelloWorldPub initializing...");

        // The client needs to specify all of the following properties:
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(InitialContext.INITIAL_CONTEXT_FACTORY, "com.solacesystems.jms.MyJNDIInitialCF");
        env.put(InitialContext.PROVIDER_URL, args[0]);
        env.put(SupportedProperty.SOLACE_JMS_VPN, args[1]);
        env.put(Context.SECURITY_PRINCIPAL, args[2]);

        InitialContext ctx = new InitialContext(env);
        TopicConnectionFactory tcf = (TopicConnectionFactory)ctx.lookup(args[3]);
        TopicConnection conn = tcf.createTopicConnection();
        TopicSession sess = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        final AtomicInteger counter = new AtomicInteger(0);
        TopicSubscriber sub = sess.createSubscriber(sess.createTopic("transactions/LOAN"));
        sub.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                counter.incrementAndGet();
                System.out.println(SolJmsUtility.dumpMessage(msg));
            }
        });
        conn.start();

        Topic destination = sess.createTopic("transactions/[AcctType]");
        TopicPublisher pub = sess.createPublisher(destination);
        TextMessage msg = sess.createTextMessage("Hello world!");
        msg.setStringProperty("AcctType", "LOAN");

        System.out.printf("Connected. About to send message '%s' to topic '%s'...%n", msg.getText(), destination.toString());

        System.out.println("Message publish()...");
        pub.publish(msg);
        System.out.println("Message send()...");
        pub.send(msg);
        System.out.println("DONE sending. Waiting for listeners.");

        while(2 > counter.intValue()) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                System.out.println("ERROR sleeping; exiting sleep loop...");
                counter.addAndGet(2);
            }
        }
        System.out.println("All messages received; exiting.");

        conn.close();
        ctx.close();
    }
}
