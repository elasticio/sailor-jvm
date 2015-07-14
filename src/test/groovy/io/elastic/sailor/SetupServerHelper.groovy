package io.elastic.sailor

import org.eclipse.jetty.server.Server
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class SetupServerHelper extends Specification {

    Server server

    def setup () {

        server = new Server(10000);
        server.setHandler(getHandler());
        server.start();
    }


    def cleanup () {
        server.stop()
    }
}