fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android test_slack
```
fastlane android test_slack
```
Test Slack integration
### android test_slack_success
```
fastlane android test_slack_success
```
Test Slack test prompt integration
### android test
```
fastlane android test
```
Run unit tests
### android deploy_firebase_app_distribution
```
fastlane android deploy_firebase_app_distribution
```
Submit a new alpha build to Firebase App Distribution
### android deploy_google_play_internal
```
fastlane android deploy_google_play_internal
```
Deploy a new version to the Google Play internal channel
### android build_release_local
```
fastlane android build_release_local
```
Build a new release version locally

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
