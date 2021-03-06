/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.sjms.jms.ConnectionResource;
import org.apache.camel.component.sjms.jms.DefaultDestinationCreationStrategy;
import org.apache.camel.component.sjms.jms.DestinationCreationStrategy;
import org.apache.camel.component.sjms.jms.DestinationNameParser;
import org.apache.camel.component.sjms.jms.KeyFormatStrategy;
import org.apache.camel.component.sjms.jms.SessionAcknowledgementType;
import org.apache.camel.component.sjms.producer.InOnlyProducer;
import org.apache.camel.component.sjms.producer.InOutProducer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JMS Endpoint
 */
@UriEndpoint(scheme = "sjms", title = "Simple JMS", syntax = "sjms:destinationType:destinationName", consumerClass = SjmsConsumer.class, label = "messaging")
public class SjmsEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean topic;

    @UriPath(enums = "queue,topic", defaultValue = "queue", description = "The kind of destination to use")
    private String destinationType;
    @UriPath @Metadata(required = "true")
    private String destinationName;
    @UriParam(label = "consumer", defaultValue = "true")
    private boolean synchronous = true;
    @UriParam
    private boolean transacted;
    @UriParam(label = "producer")
    private String namedReplyTo;
    @UriParam(defaultValue = "AUTO_ACKNOWLEDGE", enums = "SESSION_TRANSACTED,CLIENT_ACKNOWLEDGE,AUTO_ACKNOWLEDGE,DUPS_OK_ACKNOWLEDGE")
    private SessionAcknowledgementType acknowledgementMode = SessionAcknowledgementType.AUTO_ACKNOWLEDGE;
    @UriParam(defaultValue = "1")
    private int sessionCount = 1;
    @UriParam(label = "producer", defaultValue = "1")
    private int producerCount = 1;
    @UriParam(defaultValue = "1")
    private int consumerCount = 1;
    @UriParam(defaultValue = "-1")
    private long ttl = -1;
    @UriParam(label = "producer", defaultValue = "true")
    private boolean persistent = true;
    @UriParam(label = "consumer")
    private String durableSubscriptionId;
    @UriParam(label = "producer", defaultValue = "5000")
    private long responseTimeOut = 5000;
    @UriParam(label = "consumer")
    private String messageSelector;
    @UriParam(label = "consumer", defaultValue = "-1")
    private int transactionBatchCount = -1;
    @UriParam(label = "consumer", defaultValue = "5000")
    private long transactionBatchTimeout = 5000;
    @UriParam
    private boolean asyncStartListener;
    @UriParam
    private boolean asyncStopListener;
    @UriParam(label = "producer", defaultValue = "true")
    private boolean prefillPool = true;
    @UriParam(label = "producer", defaultValue = "true")
    private boolean allowNullBody = true;
    @UriParam
    private TransactionCommitStrategy transactionCommitStrategy;
    @UriParam
    private DestinationCreationStrategy destinationCreationStrategy = new DefaultDestinationCreationStrategy();

    public SjmsEndpoint() {
    }

    public SjmsEndpoint(String uri, Component component, String remaining) {
        super(uri, component);
        DestinationNameParser parser = new DestinationNameParser();
        this.topic = parser.isTopic(remaining);
        this.destinationName = parser.getShortName(remaining);
    }

    @Override
    public SjmsComponent getComponent() {
        return (SjmsComponent) super.getComponent();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @Override
    public Producer createProducer() throws Exception {
        SjmsProducer producer;
        if (getExchangePattern().equals(ExchangePattern.InOnly)) {
            producer = new InOnlyProducer(this);
        } else {
            producer = new InOutProducer(this);
        }
        return producer;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        SjmsConsumer answer = new SjmsConsumer(this, processor);
        configureConsumer(answer);
        return answer;
    }

    @Override
    public boolean isMultipleConsumersSupported() {
        return true;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * DestinationName is a JMS queue or topic name. By default, the destinationName is interpreted as a queue name.
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public ConnectionResource getConnectionResource() {
        return getComponent().getConnectionResource();
    }

    public HeaderFilterStrategy getSjmsHeaderFilterStrategy() {
        return getComponent().getHeaderFilterStrategy();
    }

    public KeyFormatStrategy getJmsKeyFormatStrategy() {
        return getComponent().getKeyFormatStrategy();
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * Sets whether synchronous processing should be strictly used or Camel is allowed to use asynchronous processing (if supported).
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public SessionAcknowledgementType getAcknowledgementMode() {
        return acknowledgementMode;
    }

    /**
     * The JMS acknowledgement name, which is one of: SESSION_TRANSACTED, CLIENT_ACKNOWLEDGE, AUTO_ACKNOWLEDGE, DUPS_OK_ACKNOWLEDGE
     */
    public void setAcknowledgementMode(SessionAcknowledgementType acknowledgementMode) {
        this.acknowledgementMode = acknowledgementMode;
    }

    /**
     * Flag set by the endpoint used by consumers and producers to determine if
     * the endpoint is a JMS Topic.
     */
    public boolean isTopic() {
        return topic;
    }

    /**
     * Returns the number of Session instances expected on this endpoint.
     */
    @Deprecated
    public int getSessionCount() {
        return sessionCount;
    }

    /**
     * Sets the number of Session instances used for this endpoint. Value is
     * ignored for endpoints that require a dedicated session such as a
     * transacted or InOut endpoint.
     *
     * @param sessionCount the number of Session instances, default is 1
     */
    @Deprecated
    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public int getProducerCount() {
        return producerCount;
    }

    /**
     * Sets the number of producers used for this endpoint.
     */
    public void setProducerCount(int producerCount) {
        this.producerCount = producerCount;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    /**
     * Sets the number of consumer listeners used for this endpoint.
     */
    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    public long getTtl() {
        return ttl;
    }

    /**
     * Flag used to adjust the Time To Live value of produced messages.
     */
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Flag used to enable/disable message persistence.
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public String getDurableSubscriptionId() {
        return durableSubscriptionId;
    }

    /**
     * Sets the durable subscription Id required for durable topics.
     */
    public void setDurableSubscriptionId(String durableSubscriptionId) {
        this.durableSubscriptionId = durableSubscriptionId;
    }

    public long getResponseTimeOut() {
        return responseTimeOut;
    }

    /**
     * Sets the amount of time we should wait before timing out a InOut response.
     */
    public void setResponseTimeOut(long responseTimeOut) {
        this.responseTimeOut = responseTimeOut;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    /**
     * Sets the JMS Message selector syntax.
     */
    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    public int getTransactionBatchCount() {
        return transactionBatchCount;
    }

    /**
     * If transacted sets the number of messages to process before committing a transaction.
     */
    public void setTransactionBatchCount(int transactionBatchCount) {
        this.transactionBatchCount = transactionBatchCount;
    }

    public long getTransactionBatchTimeout() {
        return transactionBatchTimeout;
    }

    /**
     * Sets timeout (in millis) for batch transactions, the value should be 1000 or higher.
     */
    public void setTransactionBatchTimeout(long transactionBatchTimeout) {
        if (transactionBatchTimeout >= 1000) {
            this.transactionBatchTimeout = transactionBatchTimeout;
        }
    }

    public TransactionCommitStrategy getTransactionCommitStrategy() {
        return transactionCommitStrategy;
    }

    /**
     * Sets the commit strategy.
     */
    public void setTransactionCommitStrategy(TransactionCommitStrategy transactionCommitStrategy) {
        this.transactionCommitStrategy = transactionCommitStrategy;
    }

    public boolean isTransacted() {
        return transacted;
    }

    /**
     * Specifies whether to use transacted mode
     */
    public void setTransacted(boolean transacted) {
        if (transacted) {
            setAcknowledgementMode(SessionAcknowledgementType.SESSION_TRANSACTED);
        }
        this.transacted = transacted;
    }

    public String getNamedReplyTo() {
        return namedReplyTo;
    }

    /**
     * Sets the reply to destination name used for InOut producer endpoints.
     */
    public void setNamedReplyTo(String namedReplyTo) {
        this.namedReplyTo = namedReplyTo;
        this.setExchangePattern(ExchangePattern.InOut);
    }

    /**
     * Whether to startup the consumer message listener asynchronously, when starting a route.
     * For example if a JmsConsumer cannot get a connection to a remote JMS broker, then it may block while retrying
     * and/or failover. This will cause Camel to block while starting routes. By setting this option to true,
     * you will let routes startup, while the JmsConsumer connects to the JMS broker using a dedicated thread
     * in asynchronous mode. If this option is used, then beware that if the connection could not be established,
     * then an exception is logged at WARN level, and the consumer will not be able to receive messages;
     * You can then restart the route to retry.
     */
    public void setAsyncStartListener(boolean asyncStartListener) {
        this.asyncStartListener = asyncStartListener;
    }

    /**
     * Whether to stop the consumer message listener asynchronously, when stopping a route.
     */
    public void setAsyncStopListener(boolean asyncStopListener) {
        this.asyncStopListener = asyncStopListener;
    }

    public boolean isAsyncStartListener() {
        return asyncStartListener;
    }

    public boolean isAsyncStopListener() {
        return asyncStopListener;
    }

    public boolean isPrefillPool() {
        return prefillPool;
    }

    /**
     * Whether to prefill the producer connection pool on startup, or create connections lazy when needed.
     */
    public void setPrefillPool(boolean prefillPool) {
        this.prefillPool = prefillPool;
    }

    public DestinationCreationStrategy getDestinationCreationStrategy() {
        return destinationCreationStrategy;
    }

    /**
     * To use a custom DestinationCreationStrategy.
     */
    public void setDestinationCreationStrategy(DestinationCreationStrategy destinationCreationStrategy) {
        this.destinationCreationStrategy = destinationCreationStrategy;
    }

    public boolean isAllowNullBody() {
        return allowNullBody;
    }

    /**
     * Whether to allow sending messages with no body. If this option is false and the message body is null, then an JMSException is thrown.
     */
    public void setAllowNullBody(boolean allowNullBody) {
        this.allowNullBody = allowNullBody;
    }
}
