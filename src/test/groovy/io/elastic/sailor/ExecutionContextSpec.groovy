package io.elastic.sailor

import io.elastic.api.JSON
import io.elastic.api.Message
import jakarta.json.JsonObject
import spock.lang.Specification

import jakarta.json.Json

class ExecutionContextSpec extends Specification {


    def "should build default headers properly"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
    }

    def "should build default headers with reply_to"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_REPLY_TO): "_reply_to_this_queue"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        headers[Constants.AMQP_HEADER_REPLY_TO] == '_reply_to_this_queue'
    }

    def "should build default headers with x-eio-meta- headers"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_REPLY_TO): "_reply_to_this_queue",
                (Constants.AMQP_META_HEADER_PREFIX + "lowercase"): "I am lowercase",
                "X-eio-meta-miXeDcAse": "Eventually to become lowercase"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        headers[Constants.AMQP_HEADER_REPLY_TO] == '_reply_to_this_queue'
        headers[Constants.AMQP_META_HEADER_PREFIX + "lowercase"] == 'I am lowercase'
        headers[Constants.AMQP_META_HEADER_PREFIX + "mixedcase"] == 'Eventually to become lowercase'
    }

    def "should build with parent message id"() {
        given:
        def msg = new Message.Builder().build();
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_PARENT_MESSAGE_ID): msg.id.toString()
        ] as Map
        def props = Utils.buildAmqpProperties(originalHeaders)

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                msg,
                props,
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        headers[Constants.AMQP_HEADER_PARENT_MESSAGE_ID] == props.messageId
    }

    def "should build amqp properties successfully"() {
        given:
        def msg = new Message.Builder().build();
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_PARENT_MESSAGE_ID): msg.id.toString()
        ] as Map
        def props = Utils.buildAmqpProperties(originalHeaders)

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                msg,
                props,
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);
        def uuid = UUID.randomUUID()

        when:
        def properties = ctx.buildAmqpProperties(uuid)

        then: "Should have all AMQP properties"
        properties.contentType == "application/json"
        properties.contentEncoding == "utf8"
        properties.deliveryMode == 2
        properties.priority == 1

        then: "Should have all headers"
        properties.headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        properties.headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        properties.headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        properties.headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        properties.headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        properties.headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        properties.headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        properties.headers[Constants.AMQP_HEADER_PARENT_MESSAGE_ID] == props.messageId
        properties.headers[Constants.AMQP_HEADER_MESSAGE_ID] == uuid.toString()
    }

    def "should not create passthrough is step is not configured to handle passthrough "() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map

        def emittedMessage = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .build()

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMessage)
        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{},"headers":{}}'
    }

    def "should not create passthrough is step is not configured to handle passthrough even though component emitted a message with passthrough"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map

        def emittedMessage = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .passthrough(Json.createObjectBuilder().add("msg", "I must not be emitted").build())
                .build()

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new byte[0],
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMessage)

        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{},"headers":{}}'
    }

    def "should create passthrough in the message by putting own message into it"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map

        def body = Json.createObjectBuilder()
                .add("greeting", "Hello, world!")
                .build()

        def emittedMessage = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .body(body)
                .build()

        def step = Json.createObjectBuilder()
                .add("id", "step_1")
                .add("comp_id", "testcomponent")
                .add("function", "test")
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .add("is_passthrough", true)
                .build()

        ExecutionContext ctx = new ExecutionContext(
                new Step(step),
                new byte[0],
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMessage)
        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"greeting":"Hello, world!"},"headers":{},"passthrough":{"step_1":{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"greeting":"Hello, world!"},"headers":{}}}}'
    }

    def "should add own message into existing passthrough"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map


        def triggerBody = Json.createObjectBuilder()
                .add("time", "2017-03-20T18:45:02.122Z")
                .build()

        def triggerMessage = new Message.Builder()
                .id(UUID.fromString("5594bf23-611f-4bc2-acff-d897409614ec"))
                .body(triggerBody)
                .build()

        def triggerMessageAsJson = Utils.pick(triggerMessage.toJsonObject(),
                Message.PROPERTY_ID,
                Message.PROPERTY_HEADERS,
                Message.PROPERTY_BODY,
                Message.PROPERTY_ATTACHMENTS)

        def passthroughSoFar = Json.createObjectBuilder()
                .add("step_0", triggerMessageAsJson)
                .build()

        def body = Json.createObjectBuilder()
                .add("greeting", "Hello, world!")
                .build()

        def emittedMsg = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .body(body)
                .passthrough(Json.createObjectBuilder().add("msg", "I must not be emitted").build())
                .build()

        def step = Json.createObjectBuilder()
                .add("id", "step_1")
                .add("comp_id", "testcomponent")
                .add("function", "test")
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .add("is_passthrough", true)
                .build()

        ExecutionContext ctx = new ExecutionContext(
                new Step(step),
                new byte[0],
                new Message.Builder().passthrough(passthroughSoFar).build(),
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMsg)

        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"greeting":"Hello, world!"},"headers":{},"passthrough":{"step_0":{"id":"5594bf23-611f-4bc2-acff-d897409614ec","attachments":{},"body":{"time":"2017-03-20T18:45:02.122Z"},"headers":{}},"step_1":{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"greeting":"Hello, world!"},"headers":{}}}}'
    }

    def "should add anything to passthrough in the message because the component is a trigger (stepId is missing in AMQP headers)"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map

        def incomingBody = Json.createObjectBuilder()
                .add("greeting", "Hello, world!")
                .build()

        def incomingMessage = new Message.Builder()
                .id(UUID.fromString("2c5ea4c0-4067-11e9-8bad-9b1deb4d3b7d"))
                .body(incomingBody)
                .build()

        def emittedBody = Json.createObjectBuilder()
                .add("answer", "Hello, again!")
                .build()

        def emittedMessage = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .body(emittedBody)
                .build()

        def step = Json.createObjectBuilder()
                .add("id", "step_1")
                .add("comp_id", "testcomponent")
                .add("function", "test")
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .add("is_passthrough", true)
                .build()

        ExecutionContext ctx = new ExecutionContext(
                new Step(step, true),
                new byte[0],
                incomingMessage,
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMessage)

        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"answer":"Hello, again!"},"headers":{},"passthrough":{}}'
    }

    def "should create passthrough in the message by putting incoming message into it"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_STEP_ID): "step_0"
        ] as Map

        def incomingBody = Json.createObjectBuilder()
                .add("greeting", "Hello, world!")
                .build()

        def incomingMessage = new Message.Builder()
                .id(UUID.fromString("2c5ea4c0-4067-11e9-8bad-9b1deb4d3b7d"))
                .body(incomingBody)
                .build()

        def emittedBody = Json.createObjectBuilder()
                .add("answer", "Hello, again!")
                .build()

        def emittedMessage = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .body(emittedBody)
                .build()

        def step = Json.createObjectBuilder()
                .add("id", "step_1")
                .add("comp_id", "testcomponent")
                .add("function", "test")
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .add("is_passthrough", true)
                .build()

        ExecutionContext ctx = new ExecutionContext(
                new Step(step, true),
                new byte[0],
                incomingMessage,
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMessage)

        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"answer":"Hello, again!"},"headers":{},"passthrough":{"step_0":{"id":"2c5ea4c0-4067-11e9-8bad-9b1deb4d3b7d","attachments":{},"body":{"greeting":"Hello, world!"},"headers":{}}}}'
    }

    def "should add incoming message into existing passthrough"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_STEP_ID): "step_1"
        ] as Map

        def triggerBody = Json.createObjectBuilder()
                .add("time", "2017-03-20T18:45:02.122Z")
                .build()

        def triggerMessage = new Message.Builder()
                .id(UUID.fromString("5594bf23-611f-4bc2-acff-d897409614ec"))
                .body(triggerBody)
                .build()

        def triggerMessageAsJson = Utils.pick(triggerMessage.toJsonObject(),
                Message.PROPERTY_ID,
                Message.PROPERTY_HEADERS,
                Message.PROPERTY_BODY,
                Message.PROPERTY_ATTACHMENTS)

        def passthroughSoFar = Json.createObjectBuilder()
                .add("step_0", triggerMessageAsJson)
                .build()


        def incomingBody = Json.createObjectBuilder()
                .add("greeting", "Hello, world!")
                .build()

        def incomingMessage = new Message.Builder()
                .id(UUID.fromString("2c5ea4c0-4067-11e9-8bad-9b1deb4d3b7d"))
                .body(incomingBody)
                .passthrough(passthroughSoFar)
                .build()

        def emittedBody = Json.createObjectBuilder()
                .add("answer", "Hello, again!")
                .build()

        def emittedMessage = new Message.Builder()
                .id(UUID.fromString("df6db9ec-8522-4577-9171-989f0859a249"))
                .body(emittedBody)
                .build()

        def step = Json.createObjectBuilder()
                .add("id", "step_2")
                .add("comp_id", "testcomponent")
                .add("function", "test")
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .add("is_passthrough", true)
                .build()

        ExecutionContext ctx = new ExecutionContext(
                new Step(step, true),
                new byte[0],
                incomingMessage,
                Utils.buildAmqpProperties(originalHeaders),
                new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT);

        when:
        def result = ctx.createPublisheableMessage(emittedMessage)

        then:
        JSON.stringify(result) == '{"id":"df6db9ec-8522-4577-9171-989f0859a249","attachments":{},"body":{"answer":"Hello, again!"},"headers":{},"passthrough":{"step_0":{"id":"5594bf23-611f-4bc2-acff-d897409614ec","attachments":{},"body":{"time":"2017-03-20T18:45:02.122Z"},"headers":{}},"step_1":{"id":"2c5ea4c0-4067-11e9-8bad-9b1deb4d3b7d","attachments":{},"body":{"greeting":"Hello, world!"},"headers":{}}}}'
    }
}
