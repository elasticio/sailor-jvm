export TASK="{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"newContacts\"}]}}"
export STEP_ID="step_1"
export AMQP_URI="amqp://guest:guest@127.0.0.1:5672"
export LISTEN_MESSAGES_ON="javasailor:test_exec:step_1:messages"
export PUBLISH_MESSAGES_TO="javasailor_exchange"
export DATA_ROUTING_KEY="javasailor.test_exec.step_1.message"
export ERROR_ROUTING_KEY="javasailor.test_exec.step_1.error"
export SNAPSHOT_ROUTING_KEY="javasailor.test_exec.step_1.snapshot"
export REBOUND_ROUTING_KEY="javasailor.test_exec.step_1.rebound"
export SHAPSHOT_ROUTING_KEY="javasailor.test_exec.step_1.rebound"
export COMPONENT_PATH="../component1"

java -cp ./build/classes/main:./lib/*:../component1/build/classes/main/:../component1/lib/* io.elastic.sailor.Sailor