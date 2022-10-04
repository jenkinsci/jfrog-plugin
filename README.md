# Jenkins JFrog Plugin

## Overview
The Jenkins JFrog Plugin allows for easy integration between Jenkins and the JFrog Platform.
This integration allows your build jobs to deploy artifacts and resolve dependencies to and from Artifactory, and then have them linked to the build job that created them. It also allows you to scan your artifacts and builds with JFrog Xray and distribute your software package to remote locations using JFrog Distribution.
This is all achieved by the plugin by wrapping JFrog CLI. Any JFrog CLI command can be executed from within your Jenkins Pipeline job using the JFrog Plugin.

## Installing and configuring the plugin
1. Install the JFrog Plugin by going to **Manage Jenkins | Manage Plugins**.
![](images/readme/install-plugin.png)
2. Configure your JFrog Platform details by going to **Manage Jenkins | Configure System**.
![](images/readme/plugin-config.png)
3. Configure JFrog CLI as a tool in Jenkins as described in the [Configuring JFrog CLI as a tool](#configuring-jFrog-cli-as-a-tool) section.

## Configuring JFrog CLI as a tool
