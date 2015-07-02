package io.elastic.sailor;

import io.elastic.api.EventEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallbackContainer {
    private static Logger logger = LoggerFactory.getLogger(CallbackContainer.class);

    public static EventEmitter.Callback newDataCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                /*logger.info(String.format("Message #%s data emitted", message.fields.deliveryTag));
                outgoingMessageHeaders.end = new Date().getTime();
                sailor.amqpConnection.sendData(data, outgoingMessageHeaders);*/
            }
        };
    }

    public static EventEmitter.Callback newErrorCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object err) {
                /*logger.info(String.format("Message #%s error emitted (%s)", message.fields.deliveryTag, err.message));
                outgoingMessageHeaders.end = new Date().getTime();
                sailor.amqpConnection.sendError(err, outgoingMessageHeaders, message.content);*/
            }
        };
    }

    public static EventEmitter.Callback newReboundCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                /*logger.info(String.format("Message #%s rebound (%s)", message.fields.deliveryTag, err.message));
                outgoingMessageHeaders.end = new Date().getTime();
                outgoingMessageHeaders.reboundReason = err.message;
                sailor.amqpConnection.sendRebound(err, message, outgoingMessageHeaders);*/
            }
        };
    }

    public static EventEmitter.Callback newSnapshotCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                /*logger.info(String.format("Message #%s rebound (%s)", message.fields.deliveryTag, err.message));
                outgoingMessageHeaders.end = new Date().getTime();
                outgoingMessageHeaders.reboundReason = err.message;
                sailor.amqpConnection.sendRebound(err, message, outgoingMessageHeaders);*/
            }
        };
    }

    public static EventEmitter.Callback newEndCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object err) {
                /*if (taskExec.errorCount > 0) {
                    sailor.amqpConnection.reject(message);
                } else {
                    sailor.amqpConnection.ack(message);
                }
                sailor.messagesCount -= 1;
                logger.info(String.format("Message #%s processed", message.fields.deliveryTag));*/
            }
        };
    }
}
