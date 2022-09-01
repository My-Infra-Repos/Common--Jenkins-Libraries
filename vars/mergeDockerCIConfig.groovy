/**
 * Helper function for returning a ci.yaml config defaults.
 */

import com.myorg.common.utils.Util
import com.myorg.common.utils.Constants

def call(Map<String, Object> ciConfig, Map<String, Object> pipelineDefaults = [:]) {
    defaults = [
        appName: null,
        overwritemyorgfile: false,
        includeArtifactory: true,
        includeTwistlock: true,
        includeGitSubmodules: false,
        includeGitTags: true,

        // Jenkins Credentials
        gitUserCredentialsId: null,
        gitTokenCredentialsId: null,
        dockerCredentialsId: null,
        artifactoryCredentialsId: null,

        // Docker
        dockerHubs: ["${Constants.ECR_ACCOUNT_ID}.dkr.ecr.${Constants.ECR_REGION}.amazonaws.com"],
        dockerOrg: 'TODO_REPLACEME',
        dockerBaseDir: '.',

        // Twistlock
        ignoreTwistlockFailure: false,
        archiveTwistlockScan: archiveTwistlockResults(),

        // Git
        gitUsername: Constants.GIT_USER,
        gitEmail: Constants.GIT_EMAIL,
        overwriteGitTag: true,

        // Semver Tags
        releaseTagPrefix: 'v', // Ex. 'v1.2.3'
    ]

    // ciConfig overrides defaults and pipeline options
    def config = defaults + pipelineDefaults + ciConfig

    def fullCfg = [
        appName: config.appName,
        git: [
            credentialsId: config.gitTokenCredentialsId,
            remoteName: 'origin',
            username: config.gitUsername,
            email: config.gitEmail,
            submodules: config.includeGitSubmodules
        ]
    ]


    def versionBumpLevel = 'patch' // default
    // Bump only when merging into the mainline
    if (Util.isMainlineBuild(this) && mergeInfo.pullRequestNumber) {
        def mergeInfo = getMergeInfo()
        versionBumpLevel = getSemverBumpLevelFromPR([pullRequestId: mergeInfo.pullRequestNumber])
    }

    // The highest formal semver tag found in the GitHub repository - e.g. 'v2.1.6'
    def lastReleaseVersion = glVersionsGetLatestSemanticVersionFromTag gitTagPrefix: config.releaseTagPrefix, gitCredentials: config.gitUserCredentialsId
    // The next semver tag that deployments are targeting, according to the bump level - e.g. 'v2.1.7' or 'v2.2.0'
    def targetReleaseVersion = glVersionsBump version: lastReleaseVersion, patchLevel: versionBumpLevel

    fullCfg += [
        release: [
            lastVersion: lastReleaseVersion,       // The current release version.
            versionBumpLevel: versionBumpLevel,    // The portion of the semver that we are incrementing
            targetVersion: targetReleaseVersion    // The new release version we are targeting (post bump)
        ]
    ]

    // Generate myorgfile
    def myorgfileOpts = [
        options: config,
        forceOverwrite: config.overwritemyorgfile
    ]

    def myorgfile = loadmyorgfile(myorgfileOpts)
    fullCfg += [myorgfile: myorgfile]

    // Add authenticated URL for local repo (needed to lookup tags, push, etc)
    gitRemoteAuth fullCfg.git

    if (config.includeGitTags) {
        fullCfg += [
            gitTag: [
                tagName: targetReleaseVersion,
                remoteName: 'origin',
                overwrite: config.overwriteGitTag
            ]
        ]
    }

    // Docker
    def targetBranch = Util.getCurrentBranchName(this)
    def githubUrl = env.CHANGE_URL ?: "${env.GIT_URL.replace('.git', '')}/tree/${targetBranch}"
    def dockerBuildOptions = ''

    def dockerLabels = [
        commitId: env.GIT_COMMIT,
        buildId: env.BUILD_NUMBER,
        branch: targetBranch,
        prNumber: "${env.CHANGE_ID}",
        prAuthorEmail: Util.getAuthorEmail(this),
        prAuthorName: Util.getAuthorName(this),
        githubUrl: githubUrl,
        gitUrl: env.GIT_URL,
        buildUrl: env.BUILD_URL,
        projectKey: myorgfile.metadata.projectKey,
        caAgileId: myorgfile.metadata.caAgileId,
        askId: myorgfile.metadata.askId
    ]

    dockerLabels.each { key, val ->
        dockerBuildOptions += " --label '${key}=${val}'"
    }

    def localImageName = "${config.appName}:${env.GIT_COMMIT}"
    def imagePushTags = getImagePushTags(targetReleaseVersion)
    def dockerPushCfgs = []

    config.dockerHubs.each { dockerHub ->
        if (dockerHub.contains('amazonaws.com')) {
            dockerAwsAccount = (dockerHub =~ /([0-9]+)\.dkr\.ecr\.[^.]+\.amazonaws\.com/)[0][1]
            dockerAwsRegion = (dockerHub =~ /[0-9]+\.dkr\.ecr\.([^.]+)\.amazonaws\.com/)[0][1]

            dockerLoginValues = [
                registryType: 'ecr',
                awsAccountId: dockerAwsAccount,
                awsRegion: dockerAwsRegion,
                awsProfileName: env.AWS_ACCOUNT_ID
            ]
        } else {
            // Artifactory dockerhub
            dockerLoginValues = [
                registryType: 'docker',
                credentialsId: config.dockerCredentialsId,
            ]
        }

        dockerRepoUrl = "${dockerHub}/${config.dockerOrg}/${config.appName}"
        destinationImages = imagePushTags.collect { tag -> "${dockerRepoUrl}:${tag}" }

        dockerTags = destinationImages.collect { destImage ->
            [sourceTag: localImageName, destTag: destImage]
        }

        dockerPush = [images: destinationImages]

        thisDockerOpts = [
            dockerLoginValues: dockerLoginValues,
            dockerTags: dockerTags,
            dockerPush: dockerPush
        ]

        dockerPushCfgs << thisDockerOpts
    }

    fullCfg += [
        dockerBuild: [
            image: localImageName,
            baseDir: config.dockerBaseDir,
            extraBuildOptions: dockerBuildOptions
        ],
        dockerPushCfgs: dockerPushCfgs
    ]

    // Twistlock
    if (config.includeTwistlock) {
        fullCfg += [
            twistlock: [
                dockerRepository: localImageName,
                twistlockCredentials: config.gitUserCredentialsId,
                failBuild: !config.ignoreTwistlockFailure,
                useDefaultDockerHost: true,
                twistlockDashboardUrl: 'https://containersecurity.myorg.com'
            ]
        ]

        if (config.archiveTwistlockScan) {
            fullCfg += [
                twistlockArchive: [
                    filename: "${config.appName}-twistlock-scan.txt",
                ]
            ]
        }
    }

    // Artifactory
    if (config.includeArtifactory) {
        fullCfg += [
            artifactory: [
                artifactoryUserCredentialsId: config.artifactoryCredentialsId
            ]
        ]
    }

    return fullCfg
}

def getImagePushTags(gitTagName) {
    // For merges to master, create both semver and latest tags
    if (env.BRANCH_NAME == Constants.MAINLINE_BRANCH) {
        return [gitTagName, 'latest']
    } else {
        return [Util.getCurrentBranchName(this)]
    }
}

// Save the results as long as this is a PR or the mainline branch
def archiveTwistlockResults() {
    if (env.CHANGE_BRANCH || env.BRANCH_NAME == Constants.MAINLINE_BRANCH) {
        return true
    } else {
        return false
    }
}
