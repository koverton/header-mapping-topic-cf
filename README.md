# header-mapping-topic-cf
Experimental JMS TopicConnectionFactory that maps event headers onto a logical topicspace.

## Background: Topics and Queues in Solace Messaging

[Solace Event Brokers](https://dev.solace.com/tech/) provide streaming and messaging brokers
for distributed computing via [several native and standard APIs](https://dev.solace.com/get-started/send-receive-messages/), such as JMS.

One unique difference between Solace and most JMS providers is that Topics
in the Solace platform are _just a destination on an event_. They are
not first-order objects on the broker that must be created, managed and
monitored. In Solace, they are just an addressing mechanism.

A second powerful differentiator is that Solace supports
[topic to queue mapping](https://dev.solace.com/samples/solace-samples-jms/topic-to-queue-mapping/)
again directly as subscriptions on the queues rather than as first-class
entities that must be created, managed and monitored on the event broker.

Many JMS applications and platforms are designed with the assumption that
these are first-order entities and so often either ration or outright
avoid using them. For Solace's JMS customers, it would be useful if our
JMS layer provided access to some of these features as workarounds for
applications that are limited by these assumptions.

This is one example of some possible library features.

## Background: Hierarchical Topics

Solace topics support hierarchical structure, where different levels
of the topic are separated from each other via a '/' delimiter and can
be filtered on via wildcards. As long as publishers and consumers agree
on the expected hierarchy, they can provide extremely powerful and
flexible means of distributing events.

For example, if an application establishes the following topic hierarchy:

    [event-type] / [event-action] / [account-type] / [account-id]

Publishers populate the topic for each event with those particulars
for each event. Then downstream consumers can filter on each field
separately, as a multi-key filter. Applications interested in only
transactions can filter on event-type, applications interested in only
events for particular accounts filter on account-id, etc.

    transactions/>
    accounts/change/*
    transactions/*/1*

This is what becomes an issue for some applications that were pre-coded
with assumptions about topics as scarce resources. For these apps, they
expect to publish to a statically-configured topic 'transactions' or
'accounts'.

## Sample solution: Header-mapped topics

This solution is to support Topic definitions that represent combinations
of the underlying message headers. In my example, a topic can be constructed
as a mixture of literal strings with header-mapped values. For example,
if we have a transaction publisher that expects to publish to a single
topic, if the Solace TopicPublisher has the ability to populate the
Solace topic from the JMS topic definition, then the static JMS topic
can map to an extensible multi-key topicspace.

    transactions/[acct-type]/[account-id]

The sample allows publisher code like this:

```Java
TopicConnectionFactory tcf = (TopicConnectionFactory)ctx.lookup("my/hdrmapped/tcf");
TopicConnection connection = tcf.createTopicConnection();
TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

Topic destination = session.createTopic("transactions/[AcctType]/[AcctID]");
TopicPublisher publisher = session.createPublisher(destination);

TextMessage msg = sess.createTextMessage("Fill msg content");
msg.setStringProperty("AcctType", "LOAN");
msg.setStringProperty("AcctID", curID);
publisher.publish(msg);
```

But the dynamic topic is filled in with actual content from the JMS
msg headers. So for the following message headers we expect the corresponding
output topic:

| JMS Headers                 | Output Topic            |
+-----------------------------|-------------------------+
| AcctType=LOAN, AcctID=54321 | transactions/LOAN/54321 |
| AcctType=BANK, AcctID=11111 | transactions/BANK/11111 |
+-------------------------------------------------------+

Which consumers can take advantage of for filtering and fan-out.