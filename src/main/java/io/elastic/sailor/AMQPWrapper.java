package io.elastic.sailor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class AMQPWrapper {
    public static class ConnectionWrapper {
        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ConnectionWrapper.class);

        private final Map<String, String> settings;
        private Connection amqp;
        private Channel subscribeChannel;
        private Channel publishChannel;

        public ConnectionWrapper(Map<String, String> settings) {
            this.settings = settings;
        }

        public ConnectionWrapper connect(URI uri) {
            return openConnection(uri)
                    .openPublishChannel()
                    .openSubscribeChannel();
        }

        public void disconnect() {
            logger.info("About to disconnect from AMQP");
            try {
                subscribeChannel.close();
            } catch (IOException e) {
                logger.info("Subscription channel is already closed: " + e);
            }
            try {
                publishChannel.close();
            } catch (IOException e) {
                logger.info("Publish channel is already closed: " + e);
            }
            try {
                amqp.close();
            } catch (IOException e) {
                logger.info("AMQP connection is already closed: " + e);
            }
            logger.info("Successfully disconnected from AMQP");
        }

        private ConnectionWrapper openConnection(URI uri) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(uri);
                amqp = factory.newConnection();
                logger.info("Connected to AMQP");
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private ConnectionWrapper openPublishChannel() {
            try {
                publishChannel = amqp.createChannel();
                logger.info("Opened publish channel");
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private ConnectionWrapper openSubscribeChannel() {
            try {
                subscribeChannel = amqp.createChannel();
                logger.info("Opened subscribe channel");
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
