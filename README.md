# common-jenkins-library
Shared functions for use in Jenkinsfiles

This library is intended to for aggregating common functions used across many Jenkinsfiles.

[JPaC] also has many common functions and these should generally be used before we consider writing our own. Why reinvent the wheel? These are used by many teams and likely better tested.

The functions we add here should only be for things that are unique to us or not otherwise found in JPaC.

## How to use this library
You need to import the library in your Jenkinsfile. Once this is done, you can simply call the functions like any other:

```
@Library("CommonJenkinsLibrary") _

node('docker-aws-slave') {
    stage('AWS Auth') {
        awsAuth credentialsId: awsCredentialsId, awsAccountId: awsAccountId
    }

    stage('Configure Kubeconfig') {
        configureEksKubeconfig clusterName: "mycluster"
    }

    stage('Docker Auth') {
        dockerLogin registryType: "ecr", awsAccountId: awsAccountId
    }

    stage('Configure Git') {
        dir("somerepo") {
            configureGitForPush credentialsId: gitTokenCredentialId, branch: "mybranch"
        }
    }

    stage('Bump version') {
        if (someCondition) {
            bumpVersion credentialsId: gitTokenCredentialId, tagPrefix: "RELEASE-"
        }
    }

    ...
```

## Function documentation
Each function is documented via groovydoc comments on its `call` definition. This includes a
description of what the function does, the parameters it accepts, with defaults and what can be
expected as a return value.

## Creating new functions
Functions go in the `vars/` directory. The name of the file (before `.groovy`) is the function name you will call externally.

The function needs to have `call` defined and should generally accept a `Map<String, Object>` as parameters. You can then use
this map to easily set defaults to use with the rest of your code. For example:
``` vars/myNewFunction.groovy
def call(Map<String, Object> params) {
    defaults = [
        profileName : "default",
        credentialsId : null,
        awsAccountId : null,
        awsRoleArn: null
    ]
    def config = defaults + params
    ...
}
```

You must document your function with a groovydoc comment above the `call` function. See the [docs on groovydoc here](https://groovy-lang.org/syntax.html#_groovydoc_comment)

# Shared pipelines
Reusable pipelines-as-functions following the naming convention `runPipeline<NAME>.groovy`.

These can be called via your application's `Jenkinsfile`:
```groovy
@Library('CommonJenkinsLibrary') _
runPipelineMavenCI()
```

It is expected that you have a `ci.yaml` file in the root of your repository for pipeline configuration. If your repo does not already have an `.file.yml` one will be generated during the pipeline runtime, although this is not added back to the repo.

You can see [examples of the ci.yaml file here](Example_ci_yaml.md).
