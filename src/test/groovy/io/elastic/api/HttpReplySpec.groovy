package io.elastic.api

import spock.lang.Specification

class HttpReplySpec extends Specification {

    def "throw exception if content null"() {
        when:
        new HttpReply.Builder().build()

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "HttpReply content must not be null"
    }

    def "build successful reply with HTTP 202"() {
        when:
        def stream = new ByteArrayInputStream("hello".getBytes())

        def reply = new HttpReply.Builder()
                .content(stream)
                .status(HttpReply.Status.ACCEPTED)
                .header('X-Powered-By', 'elastic.io')
                .build()

        then:
        reply.status == HttpReply.Status.ACCEPTED.statusCode
        reply.content == stream
        reply.headers == ['x-eio-status-code': '202', 'X-Powered-By': 'elastic.io']
    }

    def "build failed reply with HTTP 400"() {
        when:
        def stream = new ByteArrayInputStream("hello".getBytes())

        def reply = new HttpReply.Builder()
                .content(stream)
                .status(HttpReply.Status.BAD_REQUEST)
                .header('X-Powered-By', 'elastic.io')
                .build()

        then:
        reply.status == HttpReply.Status.BAD_REQUEST.statusCode
        reply.content == stream
        reply.headers == ['x-eio-status-code': '400', 'X-Powered-By': 'elastic.io']
    }
}
