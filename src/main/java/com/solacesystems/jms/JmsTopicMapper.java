package com.solacesystems.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Class in charge of mapping JMS message headers onto a
 * template hierarchical topic definition.
 */
class JmsTopicMapper {
    enum Kind { LITERAL, FIELD }
    class Mapping {
        private String value;
        private Kind kind;

        Mapping(Kind kind, String value) {
            this.value = value;
            this.kind = kind;
        }

        String getValue() {
            return value;
        }

        Kind getKind() {
            return kind;
        }
    }

    final private static String DELIM = "/";
    JmsTopicMapper(String map) {
        orderedMapping = new ArrayList<>();
        if (map == null || map.length() == 0) return;
        for(String v : map.split(DELIM)) {
            Kind kind = Kind.LITERAL;
            String value = v;
            if (v.startsWith("[") && v.endsWith("]")) {
                kind = Kind.FIELD;
                value = v.substring(1, v.indexOf(']'));
            }
            orderedMapping.add(new Mapping(kind, value));
        }
    }

    /**
     * Maps headers from the JMS message onto the ordered topic mapping
     * that was provided in the class constructor.
     *
     * @param msg JMS message to be mapped to a topic
     * @return generated topic string based on the ordered mapping definition and the input message.
     */
    String mapTopic(Message msg) {
        StringBuilder bldr = new StringBuilder();
        boolean isFirst = true;
        for(Mapping m : orderedMapping) {
            if (!isFirst) bldr.append(DELIM);
            try {
                String value = m.getValue();
                if (m.getKind() == Kind.LITERAL) {
                    bldr.append(value);
                }
                else {
                    Object o = msg.getObjectProperty(value);
                    if (o == null) {
                        bldr.append('_').append(value).append('_');
                    }
                    else {
                        bldr.append(o.toString());
                    }
                }
            }
            catch(JMSException jex) {
                // TODO: handle properly!
                bldr.append("_");
                jex.printStackTrace();
            }
            finally {
                isFirst = false;
            }
        }
        return bldr.toString();
    }

    final private List<Mapping> orderedMapping;
}
