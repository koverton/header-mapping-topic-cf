package com.solacesystems.jms;

import com.solacesystems.common.SolReserved;
import com.solacesystems.common.jndi.JNDINoSuchObjectException;
import com.solacesystems.common.jndi.JNDIObject;
import com.solacesystems.common.jndi.JNDIObjectList;
import com.solacesystems.common.jndi.JNDIObjectType;
import com.solacesystems.common.jndi.JNDISAXParser;
import com.solacesystems.common.jndi.JNDIUtil;
import com.solacesystems.common.property.Property;
import com.solacesystems.common.property.PropertyConversionException;
import com.solacesystems.common.property.PropertyVetoException;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPGlobalProperties;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JndiMessage;
import com.solacesystems.jms.impl.JMSClientInfoProvider;
import com.solacesystems.jms.impl.SolQueueImpl;
import com.solacesystems.jms.impl.SolTopicImpl;
import com.solacesystems.jms.property.JMSConnectionFactoryPropertyBean;
import com.solacesystems.jms.property.JMSProperties;
import com.solacesystems.jms.property.JMSProperty;
import com.solacesystems.jms.property.JMSPropertyBean;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Hashtable;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.InitialContextFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/***
 *  NOT A NECESSARY CLASS.
 *  This was only copied to provide the example; the logic I am suggesting
 *  would be added directly to SolJNDIInitialContextFactory.
 *
 *  The only change to this file is in the method:
 *      private Object createConnectionFactory(JNDIObject obj)...
 */
public class MyJNDIInitialCF implements InitialContextFactory {
    private static final Log log = LogFactory.getLog(MyJNDIInitialCF.class);

    public MyJNDIInitialCF() {
    }

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new MyJNDIInitialCF.SolJNDIInitialContext(environment);
    }

    public class SolJNDIInitialContext extends InitialContext {
        public SolJNDIInitialContext() throws NamingException {
        }

        public SolJNDIInitialContext(Hashtable<?, ?> environment) throws NamingException {
            super(environment);
        }

        protected Context getDefaultInitCtx() throws NamingException {
            if (!this.gotDefault) {
                MyJNDIInitialCF.SolJNDIInitialContextImpl internalImplementation = MyJNDIInitialCF.this.new SolJNDIInitialContextImpl(this.myProps);
                this.defaultInitCtx = internalImplementation;
                this.gotDefault = true;
            }

            return this.defaultInitCtx;
        }

        public String getJNDIHostList() {
            return ((MyJNDIInitialCF.SolJNDIInitialContextImpl)this.defaultInitCtx).getJNDIHostList();
        }

        public String getDataHostList(int port) {
            return ((MyJNDIInitialCF.SolJNDIInitialContextImpl)this.defaultInitCtx).getDataHostList(port);
        }

        public String getUsername() {
            return ((MyJNDIInitialCF.SolJNDIInitialContextImpl)this.defaultInitCtx).getUsername();
        }

        public String getPassword() {
            return ((MyJNDIInitialCF.SolJNDIInitialContextImpl)this.defaultInitCtx).getPassword();
        }

        @SolReserved
        public JCSMPProperties getJCSMPProperties() {
            return ((MyJNDIInitialCF.SolJNDIInitialContextImpl)this.defaultInitCtx).getJCSMPProperties();
        }

        @SolReserved
        public JMSProperties getJMSProperties() {
            return ((MyJNDIInitialCF.SolJNDIInitialContextImpl)this.defaultInitCtx).getJMSProperties();
        }
    }

    protected class SolJNDIInitialContextImpl implements Context {
        private JCSMPProperties mJCSMPProperties;
        private JMSProperties mJMSProperties;
        private final String NOT_SUPPORTED = " is not supported";

        private String getMethodName() {
            return Thread.currentThread().getStackTrace()[3].getMethodName();
        }

        public String getStackTrace(Throwable aThrowable) {
            Writer result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            aThrowable.printStackTrace(printWriter);
            return result.toString();
        }

        private NamingException makeNamingException(String message, Throwable t) {
            NamingException ne = new NamingException(message);

            try {
                ne.initCause(t);
            } catch (IllegalStateException var5) {
            } catch (IllegalArgumentException var6) {
            }

            return ne;
        }

        public SolJNDIInitialContextImpl(Hashtable<?, ?> environment) throws NamingException {
            this.init(environment);
        }

        private void init(Hashtable<?, ?> environment) throws NamingException {
            if (environment == null) {
                throw new NamingException("Null environment passed in to " + MyJNDIInitialCF.class.getName());
            } else {
                try {
                    this.mJMSProperties = new JMSProperties(environment);
                    this.mJMSProperties.initialize();
                    if (MyJNDIInitialCF.log.isDebugEnabled()) {
                        MyJNDIInitialCF.log.debug(this.mJMSProperties);
                    }
                } catch (Exception var3) {
                    throw new NamingException(var3.getMessage());
                }

                this.mJCSMPProperties = this.toJCSMPProperties();
            }
        }

        public String getJNDIHostList() {
            return (String)((Property)this.mJMSProperties.getProperties().get(JMSProperty.JNDIHostlist.toString())).getValue();
        }

        public String getDataHostList(int port) {
            JMSPropertyBean bean = new JMSPropertyBean(this.mJMSProperties.getProperties().values());
            JMSConnectionFactoryPropertyBean cfBean = new JMSConnectionFactoryPropertyBean(this.mJMSProperties.getProperties().values());

            try {
                cfBean.setPort(port);
            } catch (PropertyConversionException var5) {
                var5.printStackTrace();
            } catch (PropertyVetoException var6) {
                var6.printStackTrace();
            }

            JCSMPProperties jcsmpProps = SolConnection.toJCSMPProperties(bean, false);
            return jcsmpProps.getStringProperty("host");
        }

        public String getUsername() {
            return (String)((Property)this.mJMSProperties.getProperties().get(JMSProperty.JNDIUsername.toString())).getValue();
        }

        public String getPassword() {
            return (String)((Property)this.mJMSProperties.getProperties().get(JMSProperty.JNDIPassword.toString())).getValue();
        }

        @SolReserved
        public JCSMPProperties getJCSMPProperties() {
            return this.mJCSMPProperties;
        }

        @SolReserved
        public JMSProperties getJMSProperties() {
            return this.mJMSProperties;
        }

        public Object lookup(String name) throws NamingException {
            if (MyJNDIInitialCF.log.isDebugEnabled()) {
                MyJNDIInitialCF.log.debug("Entering lookup.  JNDI lookup of " + name);
            }

            if (name != null && name.length() != 0) {
                JCSMPSession jcsmpSession = null;

                Object var9;
                try {
                    jcsmpSession = JCSMPFactory.onlyInstance().createSession(this.mJCSMPProperties);
                    JndiMessage request = new JndiMessage();
                    request.setPayload(JNDISAXParser.createLookupRequest(name).getBytes());
                    JndiMessage response = jcsmpSession.executeJndiQuery(request);
                    String responseStr = new String(response.getPayload());
                    JNDISAXParser jndiParser = new JNDISAXParser();
                    JNDIObjectList jndiObjs = jndiParser.parse(responseStr);
                    JNDIObject obj = jndiObjs.getJNDIObject();
                    if (obj == null) {
                        throw new NameNotFoundException("JNDI lookup of \"" + name + "\" failed - not found");
                    }

                    var9 = this.createJNDIObject(obj);
                } catch (InvalidPropertiesException var17) {
                    String message = var17.getMessage();
                    if (var17.getCause() != null) {
                        message = message + " - Caused by " + var17.getCause().getClass().getName();
                        if (var17.getCause().getMessage() != null && var17.getCause().getMessage().length() > 0) {
                            message = message + " - " + var17.getCause().getMessage();
                        }
                    }

                    throw this.makeNamingException(message, var17);
                } catch (JNDINoSuchObjectException var18) {
                    throw new NameNotFoundException("JNDI lookup of \"" + name + "\" failed - not found");
                } catch (JCSMPErrorResponseException var19) {
                    if (var19.getResponseCode() == 404) {
                        if (var19.getSubcodeEx() == 1) {
                            throw new NamingException("JNDI lookup of \"" + name + "\" failed - client username \"" + this.mJCSMPProperties.getProperty("username") + "\" not found");
                        }

                        throw new NameNotFoundException("JNDI lookup of \"" + name + "\" failed - not found");
                    }

                    throw this.makeNamingException("JNDI lookup failed - " + var19.getMessage(), var19);
                } catch (NamingException var20) {
                    throw var20;
                } catch (Throwable var21) {
                    throw this.makeNamingException("JNDI lookup failed - " + var21.getMessage(), var21);
                } finally {
                    if (jcsmpSession != null) {
                        jcsmpSession.closeSession();
                    }

                }

                return var9;
            } else {
                return this;
            }
        }

        public void close() throws NamingException {
        }

        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void bind(Name name, Object obj) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void bind(String name, Object obj) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Name composeName(Name name, Name prefix) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public String composeName(String name, String prefix) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Context createSubcontext(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Context createSubcontext(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void destroySubcontext(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void destroySubcontext(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Hashtable<?, ?> getEnvironment() throws NamingException {
            Hashtable<?, ?> retEnv = (Hashtable)this.mJMSProperties.getEnvironment().clone();
            return retEnv;
        }

        public String getNameInNamespace() throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public NameParser getNameParser(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public NameParser getNameParser(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Object lookup(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Object lookupLink(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Object lookupLink(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void rebind(Name name, Object obj) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void rebind(String name, Object obj) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public Object removeFromEnvironment(String propName) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void rename(Name oldName, Name newName) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void rename(String oldName, String newName) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void unbind(Name name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        public void unbind(String name) throws NamingException {
            String exceptionMsg = this.getMethodName() + " is not supported";
            if (MyJNDIInitialCF.log.isErrorEnabled()) {
                MyJNDIInitialCF.log.error(exceptionMsg + "\n" + this.getStackTrace(new Exception()));
            }

            throw new OperationNotSupportedException(exceptionMsg);
        }

        private Object createJNDIObject(JNDIObject obj) throws NamingException {
            if (obj.getType().equals(JNDIObjectType.ConnectionFactory)) {
                return this.createConnectionFactory(obj);
            } else if (obj.getType().equals(JNDIObjectType.Queue)) {
                return this.createQueue(obj);
            } else if (obj.getType().equals(JNDIObjectType.Topic)) {
                return this.createTopic(obj);
            } else {
                throw new NamingException("Internal Error creating JNDI object");
            }
        }


        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// THIS IS THE ONLY REASON I DUPLICATED THIS ENTIRE CLASS!!!
        /// BECAUSE THIS IS PRIVATE I COULD NOT JUST INHERIT AND OVERLOAD!!!
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        private Object createConnectionFactory(JNDIObject obj) throws NamingException {
            HeaderMappingTopicConnectionFactory myCF = new HeaderMappingTopicConnectionFactory(this.mJMSProperties);
            myCF.fromJNDIProperties(obj);
            return myCF;
        }
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


        private Queue createQueue(JNDIObject obj) throws NamingException {
            try {
                String physicalName = JNDIUtil.getProperty(obj, "physical-name");
                return new SolQueueImpl(JCSMPFactory.onlyInstance().createQueue(physicalName));
            } catch (Throwable var3) {
                throw new NamingException("Error looking up \"physical-name\" property");
            }
        }

        private Topic createTopic(JNDIObject obj) throws NamingException {
            try {
                String physicalName = JNDIUtil.getProperty(obj, "physical-name");
                return new SolTopicImpl(JCSMPFactory.onlyInstance().createTopic(physicalName));
            } catch (Throwable var3) {
                throw new NamingException("Error looking up \"physical-name\" property");
            }
        }

        private JCSMPProperties toJCSMPProperties() throws NamingException {
            JMSPropertyBean bean = new JMSPropertyBean(this.mJMSProperties.getProperties().values());
            if (!bean.isSetJNDIHostlist()) {
                throw new NamingException("URL must be specified");
            } else if (!bean.getJNDIAuthenticationScheme().equals("AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE") && !bean.getJNDIAuthenticationScheme().equals("AUTHENTICATION_SCHEME_GSS_KRB") && !bean.isSetJNDIUsername()) {
                throw new NamingException("Username must be specified");
            } else {
                JCSMPProperties jcsmpProps = new JCSMPProperties();
                JCSMPChannelProperties ccProps = (JCSMPChannelProperties)jcsmpProps.getProperty("client_channel");
                jcsmpProps.setProperty("host", bean.getJNDIHostlist());
                if (bean.getJNDIUsername() != null) {
                    jcsmpProps.setProperty("username", bean.getJNDIUsername());
                }

                jcsmpProps.setProperty("password", bean.getJNDIPassword());
                if (bean.isSetJNDIAuthenticationScheme()) {
                    jcsmpProps.setProperty("AUTHENTICATION_SCHEME", bean.getJNDIAuthenticationScheme());
                }

                if (bean.isSetJNDIKRBMutualAuthentication()) {
                    jcsmpProps.setBooleanProperty("KRB_MUTUAL_AUTHENTICATION", bean.getJNDIKRBMutualAuthentication());
                }

                if (bean.isSetJNDIKRBServiceName()) {
                    jcsmpProps.setProperty("KRB_SERVICE_NAME", bean.getJNDIKRBServiceName());
                }

                jcsmpProps.setProperty("client_info_provider", new JMSClientInfoProvider());
                if (bean.isSetJNDIVPN()) {
                    jcsmpProps.setProperty("vpn_name", bean.getJNDIVPN());
                }

                if (bean.isSetJNDIClientID() && bean.getJNDIClientID().length() > 0) {
                    jcsmpProps.setProperty("client_name", bean.getJNDIClientID());
                }

                jcsmpProps.setProperty("application_description", bean.getJNDIClientDescription());
                ccProps.setConnectTimeoutInMillis(bean.getJNDIConnectTimeoutInMillis());
                ccProps.setReadTimeoutInMillis(bean.getJNDIReadTimeoutInMillis());
                ccProps.setConnectRetriesPerHost(bean.getJNDIConnectRetriesPerHost());
                ccProps.setReconnectRetries(bean.getJNDIReconnectRetries());
                ccProps.setConnectRetries(bean.getJNDIConnectRetries());
                ccProps.setReconnectRetryWaitInMillis(bean.getJNDIReconnectRetryWaitInMillis());
                ccProps.setCompressionLevel(bean.getJNDICompressionLevel());
                if (bean.isSetLocalhost()) {
                    jcsmpProps.setProperty("localhost", bean.getLocalhost());
                }

                if (bean.isSetJNDISSLCipherSuites()) {
                    jcsmpProps.setProperty("SSL_CIPHER_SUITES", bean.getJNDISSLCipherSuites());
                }

                if (bean.isSetJNDISSLExcludedProtocols()) {
                    jcsmpProps.setProperty("SSL_EXCLUDED_PROTOCOLS", bean.getJNDISSLExcludedProtocols());
                }

                if (bean.isSetJNDISSLConnectionDowngradeTo()) {
                    jcsmpProps.setProperty("SSL_CONNECTION_DOWNGRADE_TO", bean.getJNDISSLConnectionDowngradeTo());
                }

                if (bean.isSetJNDISSLProtocol()) {
                    jcsmpProps.setProperty("SSL_PROTOCOL", bean.getJNDISSLProtocol());
                }

                if (bean.isSetJNDISSLTrustedCommonNameList()) {
                    jcsmpProps.setProperty("SSL_TRUSTED_COMMON_NAME_LIST", bean.getJNDISSLTrustedCommonNameList());
                }

                if (bean.isSetJNDISSLTrustStore()) {
                    jcsmpProps.setProperty("SSL_TRUST_STORE", bean.getJNDISSLTrustStore());
                }

                if (bean.isSetJNDISSLTrustStoreFormat()) {
                    jcsmpProps.setProperty("SSL_TRUST_STORE_FORMAT", bean.getJNDISSLTrustStoreFormat());
                }

                if (bean.isSetJNDISSLTrustStorePassword()) {
                    jcsmpProps.setProperty("SSL_TRUST_STORE_PASSWORD", bean.getJNDISSLTrustStorePassword());
                }

                if (bean.isSetJNDISSLKeyStore()) {
                    jcsmpProps.setProperty("SSL_KEY_STORE", bean.getJNDISSLKeyStore());
                }

                if (bean.isSetJNDISSLKeyStoreFormat()) {
                    jcsmpProps.setProperty("SSL_KEY_STORE_FORMAT", bean.getJNDISSLKeyStoreFormat());
                }

                if (bean.isSetJNDISSLKeyStoreNormalizedFormat()) {
                    jcsmpProps.setProperty("SSL_KEY_STORE_NORMALIZED_FORMAT", bean.getJNDISSLKeyStoreNormalizedFormat());
                }

                if (bean.isSetJNDISSLKeyStorePassword()) {
                    jcsmpProps.setProperty("SSL_KEY_STORE_PASSWORD", bean.getJNDISSLKeyStorePassword());
                }

                if (bean.isSetJNDISSLPrivateKeyAlias()) {
                    jcsmpProps.setProperty("SSL_PRIVATE_KEY_ALIAS", bean.getJNDISSLPrivateKeyAlias());
                }

                if (bean.isSetJNDISSLPrivateKeyPassword()) {
                    jcsmpProps.setProperty("SSL_PRIVATE_KEY_PASSWORD", bean.getJNDISSLPrivateKeyPassword());
                }

                if (bean.isSetJNDISSLValidateCertificate()) {
                    jcsmpProps.setBooleanProperty("SSL_VALIDATE_CERTIFICATE", bean.getJNDISSLValidateCertificate());
                }

                if (bean.isSetJNDISSLValidateCertificateDate()) {
                    jcsmpProps.setBooleanProperty("SSL_VALIDATE_CERTIFICATE_DATE", bean.getJNDISSLValidateCertificateDate());
                }

                JCSMPGlobalProperties jcsmpGlobalProperties = new JCSMPGlobalProperties();
                if (bean.isSetConsumerDefaultFlowCongestionLimit()) {
                    jcsmpGlobalProperties.setConsumerDefaultFlowCongestionLimit(bean.getConsumerDefaultFlowCongestionLimit());
                }

                if (bean.isSetConsumerDispatcherQueueSize()) {
                    jcsmpGlobalProperties.setConsumerDispatcherQueueSize(bean.getConsumerDispatcherQueueSize());
                }

                if (bean.isSetProducerDispatcherQueueSize()) {
                    jcsmpGlobalProperties.setProducerDispatcherQueueSize(bean.getProducerDispatcherQueueSize());
                }

                if (bean.isSetFrequencyManagerMaxReconnects()) {
                    jcsmpGlobalProperties.setReconnectFreqManagerMaxReconnects(bean.getFrequencyManagerMaxReconnects());
                }

                try {
                    JCSMPFactory.onlyInstance().setGlobalProperties(jcsmpGlobalProperties);
                } catch (IllegalStateException var6) {
                }

                if (bean.isSetJaasConfigFileReloadEnabled()) {
                    jcsmpProps.setBooleanProperty("JaasConfigFileReloadEnabled", bean.getJaasConfigFileReloadEnabled());
                }

                if (bean.isSetJaasLoginContext()) {
                    jcsmpProps.setProperty("JaasLoginContext", bean.getJaasLoginContext());
                }

                return jcsmpProps;
            }
        }
    }
}
