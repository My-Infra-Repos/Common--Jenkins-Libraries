# ci.yaml
The `ci.yaml` file is stored at the root of an application's source code repository. This file contains information on how to run the shared CI pipeline.

The pipeline itself will set most of the values for you.

## Example

```yaml
appName: my-application
ignoreTwistlockFailure: true
```

## runPipelineDockerCI
```yaml
# Stages
includeArtifactory: true
includeTwistlock: true
includeGitTags: true
includeGitSubmodules: false # Set to true if the repo contains git submodules

# Common values
appName: my-service
overwrite.file: true
gitUserCredentialsId: <pipeline specific default>  # Jenkins 'username/password' credential w/ GH username/password
gitTokenCredentialsId: <pipeline specific default> # Jenkins 'secret text' credential w/ GH OAuth token value

# Docker
dockerHub: 'xxxxxxxxxxxxx.dkr.ecr.us-east-1.amazonaws.com'
dockerOrg: 'cc-eac' # Image 'prefix', e.g. 'cc-eac/my-application'
dockerCredentialsId: null # Only used for Artifactory dockerhub
dockerBaseDir: null # Set to the directory where the 'Dockerfile' is found if not located in the root of the repository

# Twistlock
ignoreTwistlockFailure: true

# Artifactory
artifactoryCredentialsId: <pipeline specific default>
```
