#!/usr/bin/env groovy
package com.myorg.common.utils

import com.myorg.common.utils.Constants

class Util implements Serializable {
    // From JPaC: https://github.com/jenkins-pipelines/global-pipeline-library/blob/master/src/com/myorg/jenkins/pipeline/library/scm/GitURLParser.groovy
    enum GitURLParser {
        // using specified gitUrlRegex, match it against git remote URL
        // regexMatcher will be a 2 dimensional arrays of java.util.regex.Matcher, if there's a regex match
        // protocol, hostname, orgname reponame will be in the respective position, which is defined in the enum constant value above
        // i.e given git URL 'https://github.com/jenkins-pipelines/global-pipeline-library.git'
        // regexMatcher[0] = [https://github.com/jenkins-pipelines/global-pipeline-library.git, https, ://, github.myorg.com, jenkins-pipelines, global-pipeline-library.git]
        // from the pre-defined enum constant, the result value can be extrapolated
        PROTOCOL {
            @NonCPS
            @Override
            String parse(gitUrl){
                def regexMatcher = (gitUrl =~ gitUrlRegex)
                def protocol = (regexMatcher ? regexMatcher[0][1] : null)
                return (protocol == 'git' ? 'ssh' : protocol)
            }
        },
        HOST_NAME {
            @NonCPS
            @Override
            String parse(gitUrl){
                def regexMatcher = (gitUrl =~ gitUrlRegex)
                return (regexMatcher ? regexMatcher[0][3] : null)
            }
        },
        ORG_NAME {
            @NonCPS
            @Override
            String parse(gitUrl){
                def regexMatcher = (gitUrl =~ gitUrlRegex)
                return (regexMatcher ? regexMatcher[0][4] : null)
            }
        },
        REPO_NAME {
            @NonCPS
            @Override
            String parse(gitUrl){
                def regexMatcher = (gitUrl =~ gitUrlRegex)
                def repo = (regexMatcher ? regexMatcher[0][5] : null)
                return repo?.replace('.git', '')
            }
        }

        // this regex will match git url that complies with either https (https://github.com/orgname/reponame.git) or ssh (git@github.myorg.com:orgname/reponame.git) protocol
        final String gitUrlRegex = "^(http[s]?|git)(:\\/\\/|@)([^\\/:]+)[\\/:]([^\\/:]+)\\/(.+)[.git]?\$"
        abstract String parse(gitUrl)
    }

    static def isDockerSocketMounted(jenkinsCtx) {
        def result = jenkinsCtx.sh(script: "[ -e /var/run/docker.sock ] && echo true || echo false", returnStdout: true).trim()
        return result.toBoolean()
    }

    static def getDockerHostArgs(jenkinsCtx) {
        // use DOCKER_HOST jenkins.myorg.com:30303 if docker socket is not mounted
        if (!isDockerSocketMounted(jenkinsCtx)) {
            return "-H jenkins.myorg.com:30303"
        } else {
            return ""
        }
    }

    static def getCurrentBranchName(jenkinsCtx) {
        if (jenkinsCtx.env.CHANGE_BRANCH) {
            return jenkinsCtx.env.CHANGE_BRANCH
        } else {
            return jenkinsCtx.env.BRANCH_NAME
        }
    }

    static def isMainlineBuild(jenkinsCtx) {
        def branchName = getCurrentBranchName(jenkinsCtx)
        return (branchName == Constants.MAINLINE_BRANCH)
    }

    static def getHeadSha(jenkinsCtx) {
        return jenkinsCtx.sh(script: "git rev-parse HEAD", returnStdout: true).trim()
    }

    static def isPullRequest(jenkinsCtx) {
        return jenkinsCtx.env.CHANGE_ID ? true : false
    }

    static def listGitBranches(jenkinsCtx, remote='origin') {
        def result = jenkinsCtx.sh(script: "git ls-remote --heads --refs --quiet ${remote}", returnStdout: true).trim()
        def fullRefs = result.split('\n').collect { it ? it.split('\t')[-1] : null }
        fullRefs = fullRefs - null
        return fullRefs.collect { it - 'refs/heads/' }
    }

    static def listGitTags(jenkinsCtx, remote='origin') {
        def result = jenkinsCtx.sh(script: "git ls-remote --tags --refs --quiet ${remote}", returnStdout: true).trim()
        def fullRefs = result.split('\n').collect { it ? it.split('\t')[-1] : null }
        fullRefs = fullRefs - null
        return fullRefs.collect { it - 'refs/tags/' }
    }

    static def getAuthorName(jenkinsCtx) {
        return jenkinsCtx.sh(script: "git show -s --format='%an' HEAD", returnStdout: true).trim()
    }

    static def getAuthorEmail(jenkinsCtx) {
        return jenkinsCtx.sh(script: "git show -s --format='%ae' HEAD", returnStdout: true).trim()
    }

    static def addOauthTokenToUrl(url, token) {
        return url.replace("https://", "https://${token}@")
    }

    static def getProjectName(jenkinsCtx) {
        // Example: https://github.com/cloud-idp/common-jenkins-library.git -> cloud-idp/common-jenkins-library
        // def url = jenkinsCtx.sh(script: "git remote get-url origin", returnStdout: true).trim()
        def url = jenkinsCtx.env.GIT_URL
        return "${GitURLParser.ORG_NAME.parse(url)}/${GitURLParser.REPO_NAME.parse(url)}".trim()
    }

    // Split up a semver string into parts. Allows for an optional prefix and suffix.
    @NonCPS
    static def parseSemver(semVerStr) {
        def sep = '[-+._\\/]' // Optional separators for prefix and suffix
        def re = ~/^(?<prefix>\w+)?${sep}?(?<major>\d+)\.(?<minor>[\d]+)\.(?<patch>[\d]+)${sep}?(?<suffix>\w+)?$/
        def prefix, major, minor, patch, suffix

        def m = semVerStr.trim() =~ re
        if (m.matches()) {
            prefix = m.group("prefix")
            major = m.group("major")
            minor = m.group("minor")
            patch = m.group("patch")
            suffix = m.group("suffix")
        } else {
            return null
        }

        return [
            prefix: prefix,
            major: major as Integer,
            minor: minor as Integer,
            patch: patch as Integer,
            suffix: suffix
        ]
    }

    @NonCPS
    static def commonAwsArgs(config) {
        def regionArgs = config.awsRegion ? "--region ${config.awsRegion}" : ''
        def profileArgs = config.awsProfileName ? "--profile ${config.awsProfileName}" : ''
        return "${profileArgs} ${regionArgs}".trim()
    }

    @NonCPS
    static def exists(path) {
        def output
        def filename = path.split('/')[-1]

        try {
            output = sh(script: "ls \"${path}\"", returnStdout: true)
            return output.trim() == filename
        } catch(_) {
            return false
        }
    }

    // Convert a string to a map to be used as config values.
    @NonCPS
    static def stringParamsToMap(str, delimiter=' ') {
        def m = [:]
        def key, val

        for (entry in str.split(delimiter)) {
            (key, val) = entry.split('=').collect({ it.trim() })

            if (val.contains(',')) {
                // Value is a list
                m[key] = val.split(',')
            } else if (['true', 'false'].contains(val)) {
                m[key] = val.toBoolean()
            } else {
                m[key] = val
            }
        }
        return m
    }

    static def baseDirFromStr(path) {
        def dirs = path.split('/')

        if (dirs.size() > 1) {
            return dirs[0..-2].join('/')
        } else {
            return '.'
        }
    }

    static def getJenkinsCredentialType(jenkinsCtx, credentialsId) {
        def credentialType = Constants.UNKNOWN_CREDENTIAL_TYPE

        try {
            jenkinsCtx.withCredentials([jenkinsCtx.string(credentialsId: credentialsId, variable: 'STRING')]) {
                credentialType = Constants.SECRET_TEXT_CREDENTIAL_TYPE
            }
        } catch(err) {}

        try {
            jenkinsCtx.withCredentials([jenkinsCtx.usernamePassword(credentialsId: credentialsId, usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
                credentialType = Constants.USER_PASSWORD_CREDENTIAL_TYPE
            }
        } catch(err) {}

        try {
            jenkinsCtx.withCredentials([jenkinsCtx.file(credentialsId: credentialsId, variable: 'FILE')]) {
                credentialType = Constants.SECRET_FILE_CREDENTIAL_TYPE
            }
        } catch(err) {}

        return credentialType
    }
}
