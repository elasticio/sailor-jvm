package io.elastic.sailor

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.sailor.impl.HttpUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.*

import com.github.tomakehurst.wiremock.core.WireMockConfiguration


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class HttpClientSimulationSpec extends Specification {

        @Rule
        public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private Injector injector

    def setup() {
        injector = Guice.createInjector(new HttpClientSimulationTestModule())
    }

    def cleanup() {
        final CloseableHttpClient httpClient = injector.getInstance(CloseableHttpClient.class)
        httpClient.close()
    }

    def "should handle many concurrent requests with a singleton http client"() {
        given:
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")))

        def httpClient = injector.getInstance(CloseableHttpClient.class)
        def threadCount = 100
        def threads = []
        def successCount = 0

        when:
        threadCount.times {
            def thread = new Thread({
                try {
                    def response = httpClient.execute(new HttpGet("http://localhost:${wireMockRule.port()}/test"))
                    if (response.getStatusLine().getStatusCode() == 200) {
                        synchronized (this) {
                            successCount++
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace()
                }
            })
            threads.add(thread)
            thread.start()
        }

        threads.each { it.join() }

        then:
        successCount == threadCount
    }

    def "should retry on HTTP 408"() {
        given:
        stubFor(get(urlEqualTo("/retry-test")).inScenario("Retry Scenario")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(408))
                .willSetStateTo("First Retry"))

        stubFor(get(urlEqualTo("/retry-test")).inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .willReturn(aResponse().withStatus(408))
                .willSetStateTo("Second Retry"))

        stubFor(get(urlEqualTo("/retry-test")).inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .willReturn(aResponse().withStatus(200).withBody("Success")))

        def httpClient = injector.getInstance(CloseableHttpClient.class)

        when:
        def response = httpClient.execute(new HttpGet("http://localhost:${wireMockRule.port()}/retry-test"))

        then:
        response.getStatusLine().getStatusCode() == 200

        and:
        verify(3, getRequestedFor(urlEqualTo("/retry-test")))
    }
}

