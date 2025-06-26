# sailor-jvm
The official elastic.io library for bootstrapping and executing for connectors built on JVM.

### Building:
To build the project run in terminal

    ./gradlew build



### Environment variables


 - `ELASTICIO_AMQP_PUBLISH_CONFIRM_ENABLED` - Enable publish confirm functionality. Default value: `true`
 - `ELASTICIO_CONSUMER_THREAD_POOL_SIZE` - if not specified (by default) it equals Prefetch Count value. Indicates the size of the thread pool for AMQP consumers.

## Development lifecycle
Prerequisites:
Imagine that current sailor version is `3.5.1` and you gonna release new major version `4.0.0`
1. Create branch to implement feature.
2. During implementation specify `-SNAPSHOT` suffix for the version in the `build.gradle`. The version should have next value - `4.0.0-SHAPSHOT`.
3. If you want to test new sailor version, just push changes to your feature branch. If you push any changes to the Sailor GitHub repository with X.X.X-SNAPSHOT, circle.ci will automatically upload the SNAPSHOT version to Sonatype repository.
After CI job will be done you can use `4.0.0-SHAPSHOT` version in the components.
4. After code changes will be reviewed and tested by qa, remove `-SNAPHOT` suffix, and merge Pull Request to **master** branch. The version should have next value - `4.0.0`.
5. To publish stable release version create GitHub release with tag **4.0.0**. This will trigger CI pipeline to publish release version to the Production maven repository.

6. Go to [Repository Manager](https://oss.sonatype.org/) and log in with your credentials (the same Sonatype credentials)

7. Under `Staging Repositories` you will find a freshly created repo for elastic.io

<img width="717" alt="Nexus Repository Manager 2021-01-28 13-48-33" src="https://user-images.githubusercontent.com/464220/106140893-a906ee80-616f-11eb-920f-ff142b728d66.png">

8. Close the repository

Select the repository and click on the `Close` button. A popup confirmation will appear and ask you for a description. Type something like "Releasing version 4.0.0" and hit the button.

<img width="403" alt="Nexus Repository Manager 2021-01-28 13-52-17" src="https://user-images.githubusercontent.com/464220/106141193-1024a300-6170-11eb-9aab-5c2bd5169596.png">

9. Wait until the repo is closed

You can check the process in the `Activity` tab. Use the `Refresh` button to get the update of the latest status. Once the repository is finally closed, the `Release` button is enabled.

<img width="488" alt="Nexus Repository Manager 2021-01-28 13-56-29" src="https://user-images.githubusercontent.com/464220/106141617-a8228c80-6170-11eb-8948-ddd34c53c58b.png">

10. Release it

Once you finally decided that you want to publish the Sailor artefacts to Maven Central, hit the `Release` button, provide the description in the confirmation popup, confirm and wait for the process to complete. Once the artefacts were successfully published to Maven Central, your staging repository will be deleted.

11. Use the artefacts from Maven Central

Usually it takes 8 to 24 hour for the published artefacts to appear on Maven Central. Just check [here](https://search.maven.org/search?q=a:sailor-jvm) to find out if the Sailor version is finally publicly available.
