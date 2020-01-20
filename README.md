# sailor-jvm
The official elastic.io library for bootstrapping and executing for connectors built on JVM.

### Building:
To build the project run in terminal

    ./gradlew build
    
> The official elastic.io library for bootstrapping and executing for Java connectors.

[Maven Repository](https://mvnrepository.com/artifact/io.elastic/sailor-jvm)

`sailor-jvm` is a **required dependency for all components build for [elastic.io platform](http://www.elastic.io) in Java**. Add the dependency in the `build.gradle` file in the following way:

```groovy
    compile "io.elastic:sailor-jvm:2.1.1"
```

## Building components in Java

If you plan to build a component for [elastic.io platform](http://www.elastic.io) in Java then you can visit our dedicated documentation page which describes [building a component in Java
](https://docs.elastic.io/guides/building-java-component.html).

### Before you start

Before you can deploy any code into our system **you must be a registered elastic.io platform user**. Please see our home page at [http://www.elastic.io](http://www.elastic.io) to learn how.

> Any attempt to deploy a code into our platform without a registration would fail.

After the registration and opening of the account you must **[upload your SSH Key](https://support.elastic.io/support/solutions/articles/14000038794-manage-your-ssh-keys)** into our platform.

> If you fail to upload you SSH Key you will get **permission denied** error during the deployment.

### Getting Started

After registration and uploading of your SSH Key you can proceed to deploy it into our system. At this stage we suggest you to:
*   [Create a team](https://docs.elastic.io/guides/teams-and-repos.html#creating-a-developer-team) to work on your new component. This is not required but will be automatically created using random naming by our system so we suggest you name your team accordingly.
*   [Create a repository](https://docs.elastic.io/guides/teams-and-repos.html#create-a-component-repository) inside the team to deploy your new component code.

### Examples of Java components

Here is a list of components build on Java:

*   [petstore-component-java](https://github.com/elasticio/petstore-component-java) to build your first component
*   [salesforce-component-java](https://github.com/elasticio/salesforce-component-java) to connect to Salesforce API

## Sailor logging

Sailor uses [SLF4J](https://www.slf4j.org/manual.html) logging framework. 
 
Supported log levels are:

- `FATAL`
- `ERROR`
- `WARN`
- `INFO`
- `DEBUG`
- `TRACE`

Default log level is `INFO`. You can change default log level with environment variable `LOG_LEVEL`.

Sailor logger adds the following extra context to log messages:

- `ELASTICIO_API_USERNAME`
- `ELASTICIO_COMP_ID`
- `ELASTICIO_COMP_NAME`
- `ELASTICIO_CONTAINER_ID`
- `ELASTICIO_CONTRACT_ID`
- `ELASTICIO_EXEC_ID`
- `ELASTICIO_EXEC_TYPE`
- `ELASTICIO_EXECUTION_RESULT_ID`
- `ELASTICIO_FLOW_ID`
- `ELASTICIO_FLOW_VERSION`
- `ELASTICIO_FUNCTION`
- `ELASTICIO_STEP_ID`
- `ELASTICIO_TASK_USER_EMAIL`
- `ELASTICIO_TENANT_ID`
- `ELASTICIO_USER_ID`
- `ELASTICIO_WORKSPACE_ID`

## Component logging

Sailor contains org.slf4j.Logger to make logs.

Component logger usage example:

```Java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionClass.class);

    void someMethod() {
        LOGGER.fatal('message');
        LOGGER.error('message');
        LOGGER.warn('message');
        LOGGER.info('message');
        LOGGER.debug('message');
        LOGGER.trace('message');
    }
}
```

## Using http-reply emit

This functionality is using to send http response for request made to 
previous [webhook trigger](https://docs.elastic.io/getting-started/webhooks-flow.html) flow step.
More information about HTTP reply [here](https://support.elastic.io/support/solutions/articles/14000052551-http-reply-enabling-the-two-way-messaging-conversation-through-request-reply-pattern). 

Using component as request-reply step requires to add to component.json root:
 
```
    "service": "request-reply"
```

Also requires specifying of `X-EIO-Routing-Key` header which must contain request-reply key.
Service need `Content-Type` to resolve output structure.

Example of request-reply usage:

```Java
import io.elastic.api.ExecutionParameters;
import io.elastic.api.HttpReply;

public class ActionClass {

  private static final Logger logger = LoggerFactory.getLogger(ActionClass.class);

  public void execute(final ExecutionParameters parameters) {
      final JsonObject headers = parameters.getMessage().getHeaders();
      ...
    
      logger.info("Emitting httpReply...");
      parameters
          .getEventEmitter()
          .emitHttpReply(new HttpReply.Builder()
              .status(200)
              .header("X-EIO-Routing-Key", headers.getString("reply_to"))
              .header("Content-Type", "text/xml")
              .content(new ByteArrayInputStream(dataString))
              .build());
  }
}
```