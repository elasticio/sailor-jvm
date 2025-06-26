#!/bin/bash
# This script builds the Sailor project and the component, then runs the Sailor application with the necessary environment variables set.

# Default value
JAVA_EXECUTABLE="java"

# Help message
print_help() {
    echo "This script builds the Sailor, places it into the component as ./lib/sailor-jvm.jar,"
    echo "builds the component, and runs the Sailor application with the necessary environment variables set."
    echo
    echo "Make sure that the component has the following in its build.gradle file:"
    echo "dependencies {"
    echo "  implementation files(\"./lib/sailor-jvm.jar\")"
    echo "}"
    echo
    echo "Usage: $0 <component path> [--java <java executable>]"
    echo
    echo "Arguments:"
    echo "  <component path>     (required) Path to the component directory."
    echo "  --java <executable>  (optional) Java executable to use (default: java)."
    exit 1
}

# First, check if --help is anywhere
for arg in "$@"; do
    if [[ "$arg" == "--help" || "$arg" == "-h" ]]; then
        print_help
    fi
done

# Ensure at least 1 argument (component path)
if [[ $# -lt 1 ]]; then
    echo "Error: Missing required <component path> argument."
    print_help
fi

# First argument is the required component path
COMPONENT_PATH="$1"
shift

# Parse optional named arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --java)
            if [[ -z "$2" ]]; then
                echo "Error: --java requires an argument"
                exit 1
            fi
            JAVA_EXECUTABLE="$2"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1"
            print_help
            ;;
    esac
done

"$JAVA_EXECUTABLE" -version || {
    echo "Java executable not found or not working. Please check the path to your Java installation."
    exit 1
}
echo

if [ ! -d "$COMPONENT_PATH" ]; then
    echo "Component path $COMPONENT_PATH does not exist. Please check the path."
    exit 1
fi

echo "Building Sailor..."
./gradlew shadowJar || {
    echo "Failed to build Sailor. Please check the build logs."
    exit 1
}

# Get sailor version
SAILOR_VERSION=$(./gradlew -q printVersion | grep "Sailor JVM version:" | sed -E 's/.*: //')
echo "Sailor version: $SAILOR_VERSION"

# Get the path to the Sailor JAR Failed
SAILOR_JAR=$(find build/libs -name "sailor-jvm-$SAILOR_VERSION.jar" | head -n 1)
if [ -z "$SAILOR_JAR" ]; then
    echo "Sailor JAR not found. Please check the build process."
    exit 1
fi

# Copy the Sailor JAR to the component Path
cp "$SAILOR_JAR" "$COMPONENT_PATH/lib/sailor-jvm.jar" || {
    echo "Failed to copy Sailor JAR to component path. Please check the paths."
    exit 1
}
echo "Sailor JAR copied to component path: $COMPONENT_PATH/lib/sailor-jvm.jar"

echo "Building the component..."
"$COMPONENT_PATH/gradlew" -p "$COMPONENT_PATH" assemble || {
    echo "Failed to build the component. Please check the build logs."
    echo
    echo "Make sure that component dependencies have \"implementation files(\"./lib/sailor-jvm.jar\")\" in the build.gradle file in order to use local Sailor."
    exit 1
}

# export LOG_LEVEL="DEBUG"
export ELASTICIO_AMQP_URI="amqp://guest:guest@localhost:5672"
export ELASTICIO_API_URI="https://api-sparrow.elastic.io"
export ELASTICIO_LISTEN_MESSAGES_ON="59d341e9037f7200184a408b:6849ad693a3c2100124ebecb/ordinary:step_1:messages"
export ELASTICIO_PUBLISH_MESSAGES_TO="59d341e9037f7200184a408b_org"
export ELASTICIO_DATA_ROUTING_KEY="59d341e9037f7200184a408b.6849ad693a3c2100124ebecb/ordinary.step_2.message"
export ELASTICIO_ERROR_ROUTING_KEY="59d341e9037f7200184a408b.6849ad693a3c2100124ebecb/ordinary.step_2.error"
export ELASTICIO_REBOUND_ROUTING_KEY="59d341e9037f7200184a408b.6849ad693a3c2100124ebecb/ordinary.step_2.rebound"
export ELASTICIO_SNAPSHOT_ROUTING_KEY="59d341e9037f7200184a408b.6849ad693a3c2100124ebecb/ordinary.step_2.snapshot"
export ELASTICIO_MESSAGE_CRYPTO_PASSWORD="k8HO8UurPfKUNjECNAbvLRjBHWkIWz"
export ELASTICIO_MESSAGE_CRYPTO_IV="1gmpybK4iLRRyyu0"
export ELASTICIO_NO_SELF_PASSTRHOUGH="false"
export ELASTICIO_API_REQUEST_RETRY_ATTEMPTS="5"
export ELASTICIO_API_KEY="c316abf1-5016-4a20-96fa-133a45103bf6"
export ELASTICIO_API_USERNAME="task-6849ad693a3c2100124ebecb"
export ELASTICIO_COMP_ID="68494af0c53ea500123dbc7c"
export ELASTICIO_CONTAINER_ID="testcomponent_container"
export ELASTICIO_EXEC_ID="testcomponent_exec"
export ELASTICIO_FLOW_ID="6849ad693a3c2100124ebecb"
export ELASTICIO_FUNCTION="getPetsByStatus"
export ELASTICIO_STEP_ID="step_1"
export ELASTICIO_USER_ID="testcomponent_user"
export ELASTICIO_WORKSPACE_ID="testcomponent_workspace"

"$JAVA_EXECUTABLE" -cp "$COMPONENT_PATH/lib/*:$COMPONENT_PATH/:$COMPONENT_PATH/build/classes/main" io.elastic.sailor.Sailor | bunyan
