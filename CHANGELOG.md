## 4.0.3 (July 31, 2024)
  * Bumped all the dependencies to its most recent minor versions

## 4.0.2 (June 26, 2024) 
  * Fixed a [bug](https://github.com/elasticio/sailor-jvm/issues/94) when snapshots are not read correctly when there is more than one execution within a single container

## 4.0.1 (July 14, 2023) 
  * Replaced jakarta.json with org.eclipse.parsson

## 4.0.0 (February 14 ♥, 2022) 
  * [Migrate from javax.json to jakarta.json](https://github.com/elasticio/elasticio/issues/6744)
    * Backward incompatible changes. Replaced `javax.json` with `org.glassfish:jakarta.json:2.0.1`
## 3.5.0 (September 30, 2022)

* [JAVA sailor doesn't support java version 17](https://github.com/elasticio/elasticio/issues/6364) 
  * upgrade gradle wrapper version to `7.5.1`
  * new publish mechanism
  * update libraries to support Java 17
## 3.4.0 (September 20, 2022)

* [Add AMQP delivery mode configuration](https://github.com/elasticio/elasticio/issues/6247) by `AMQP_PERSISTENT_MESSAGES` env var

## 3.3.9 (April 8, 2022)

* Get rid of high vulnerabilities in dependencies
* Add dependency check job to Circle CI

## 3.3.8 (February 11, 2022)

* Get rid of high and critical vulnerabilities in dependencies

## 3.3.7 (December 10, 2021)

* [Make sure message contains all webhooks related info #68](https://github.com/elasticio/sailor-jvm/issues/68)

## 3.3.6 (June 3, 2021)

* [Make sailor-jvm process messages in parallel #5433](https://github.com/elasticio/elasticio/issues/5433)

## 3.3.5 (March 5, 2021)

* [AMQP_PUBLISH_CONFIRM_ENABLED was renamed to the ELASTICIO_AMQP_PUBLISH_CONFIRM_ENABLED](https://github.com/elasticio/elasticio/issues/5191)

## 3.3.4 (March 3, 2021)

* [New environment variable AMQP_PUBLISH_CONFIRM_ENABLED is introduced](https://github.com/elasticio/elasticio/issues/5191)

## 3.3.3 (February 22, 2021)

* Fixed issue [Publish exceptions under load #63](https://github.com/elasticio/sailor-jvm/issues/63)

## 3.3.2 (February 2, 2021)

* Fixed a bug when a credentials verification reason was not displayed on the UI 

## 3.3.1 (November 3, 2020)

* Remove sensitive logs and data

## 3.3.0 (July 23, 2020)

* Error Handling and Protocol Version 2 encoding

## 3.2.0 (July 13, 2020)

* Support Lightweight messages in Sailor and Enabling graceful restart of tasks pods

## 3.1.0 (May 20, 2020)

* Dynamic Flow Control
* Fix bug when Lookout throws an exception if an incoming message from the error queue doesn't have an errorInput property

## 3.0.0 (April 23, 2020)

* Introduced shutdown-hooks functionality

## 2.1.2 (February 3, 2020)

* Add a language specific feature flag
