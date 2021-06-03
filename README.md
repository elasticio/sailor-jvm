# sailor-jvm
The official elastic.io library for bootstrapping and executing for connectors built on JVM.

### Building:
To build the project run in terminal

    ./gradlew build



### Environment variables


 - `ELASTICIO_AMQP_PUBLISH_CONFIRM_ENABLED` - Enable publish confirm functionality. Default value: `true`
 - `ELASTICIO_CONSUMER_THREAD_POOL_SIZE` - if not specified (by default) it equals Prefetch Count value. Indicates the size of the thread pool for AMQP consumers.