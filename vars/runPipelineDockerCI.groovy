/**
 * Run one of the pipelines. It is expected that the repository has a 'ci.yaml' file with configuration.
 */

import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

def call() {
    // Load JPaC functions
    library("com.myorg.jenkins.pipeline.library")

    pipeline {
        agent { label "docker-aws-slave" }

        options {
            ansiColor('xterm')
        }

        environment {
            GIT_USER_CREDENTIALS_ID = 'de71eba8-ea16-465d-9910-4940b961e0ef'
            GIT_TOKEN_CREDENTIALS_ID = 'gh-token-admin'

            AWS_ACCOUNT_ID = Constants.ECR_ACCOUNT_ID
            AWS_PROFILE_NAME = env.JOB_NAME

            IMAGE_PUSH_PATTERNS = "(${Constants.RELEASE_BRANCH}|${Constants.MAINLINE_BRANCH}|PR-.+)"
        }

        stages {
            stage ('Setup') {
                steps {
                    script {
                        skipRemainingStages = false

                        // Configuration
                        pipelineDefaults = [
                            includeTwistlock: true,
                            includeArtifactory: false,
                            includeGitTags: true,
                            gitUserCredentialsId: env.GIT_USER_CREDENTIALS_ID,
                            gitTokenCredentialsId: env.GIT_TOKEN_CREDENTIALS_ID,
                            artifactoryCredentialsId: env.GIT_USER_CREDENTIALS_ID,
                            dockerCredentialsId: env.GIT_USER_CREDENTIALS_ID
                        ]

                        // Config precedence order is: pipelineDefaults > ciConfigDefaults > ci.yaml
                        ciConfig = generatePipelineConfig(pipelineDefaults: pipelineDefaults, pipelineType: 'DOCKER')

                        echoLog "CONFIG: ${ciConfig}"
                        echoLog "myorgfile: ${ciConfig.myorgfile}"
                    }
                }
            }

            stage ("Load Submodules") {
                when {
                    not { expression { skipRemainingStages == true } }
                    expression { ciConfig.git.submodules == true }
                }
                steps {
                    script {
                        gitInitSubmodules [:]
                        glDockerImageBuild ciConfig.dockerBuild
                    }
                }
            }

            stage ("Docker Build") {
                when {
                    not { expression { skipRemainingStages == true } }
                    expression { ciConfig.dockerBuild }
                }
                steps {
                    script {
                        glDockerImageBuild ciConfig.dockerBuild
                    }
                }
            }

            stage ("Twistlock Scan") {
                when {
                    not { expression { skipRemainingStages == true } }
                    expression { ciConfig.twistlock }
                }
                steps {
                    script {
                        def scanResults = glTwistlockScan ciConfig.twistlock

                        if (ciConfig.twistlockArchive) {
                            def resultsFile = ciConfig.twistlockArchive.filename
                            writeFile file: resultsFile, text: scanResults
                            archiveArtifacts artifacts: resultsFile, allowEmptyArchive: true
                        }
                    }
                }
            }

            stage ("Create Git Tags") {
                when {
                    not { expression { skipRemainingStages == true } }
                    branch pattern: "${env.IMAGE_PUSH_PATTERNS}", comparator: "REGEXP"
                    expression { ciConfig.gitTag }
                }
                steps {
                    script {
                        gitTagAndPush ciConfig.gitTag
                    }
                }
            }

            stage ('Artifactory Upload') {
                when {
                    not { expression { skipRemainingStages == true } }
                    expression { ciConfig.artifactory }
                    branch 'master'
                }
                steps {
                    glMavenArtifactoryDeploy ciConfig.artifactory
                }
            }

            stage ('AWS Authentication') {
                when {
                    not { expression { skipRemainingStages == true } }
                    branch pattern: "${env.IMAGE_PUSH_PATTERNS}", comparator: "REGEXP"
                }
                steps {
                    awsAuth profileName: env.AWS_PROFILE_NAME,
                        credentialsId : Constants.ECR_CREDENTIALS_ID,
                        awsAccountId : env.AWS_ACCOUNT_ID
                }
            }

            stage ("Image Push") {
                when {
                    not { expression { skipRemainingStages == true } }
                    branch pattern: "${env.IMAGE_PUSH_PATTERNS}", comparator: "REGEXP"
                    expression { ciConfig.dockerPushCfgs }
                }
                steps {
                    script {
                        // We may have multiple Hubs (for multiple regions etc)
                        ciConfig.dockerPushCfgs.each { dockercfg ->
                            // Create the tags
                            dockercfg.dockerTags.each { dockerTagCfg ->
                                glDockerImageTag dockerTagCfg
                            }

                            try {
                                // Note: JPaC doesn't work with AWS ECR Registries
                                dockerLogin dockercfg.dockerLoginValues
                                dockerPush dockercfg.dockerPush
                                submitDevopsEvent(myorgfile: ciConfig.myorgfile, type: "pipeline.build", eventTool: "Docker", status: "SUCCESS")
                            } catch(err) {
                                submitDevopsEvent(myorgfile: ciConfig.myorgfile, type: "pipeline.build", eventTool: "Docker", status: "FAILURE")
                            }
                        }
                    }
                }
            }
        }
    }
}
