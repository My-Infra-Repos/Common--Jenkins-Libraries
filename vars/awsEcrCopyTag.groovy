import com.myorg.common.utils.Util

/**
 * Copy a tag in ECR.
 *
 * Required params:
 * @param repoName String The full ECR repo name. Do not include the ECR URL.
 * @param tag The name of the existing tag.
 * @param toTag The name of the new tag.
 *
 * Optional params:
 * @param awsAccountId The AWS account associated with the registry.
 * @param awsProfileName String The name of the AWS profile to use in the AWS credentials file. Default is 'default'.
 * @param awsRegion String The name of the aws region to use.
 *
 * @return Boolean True or false based on success.
 */
def call(Map<String, Object> params) {
    defaults = [
        awsProfileName: "default",
        awsRegion: null,
        awsAccountId: null,
        repoName: null,
        fromTag: null,
        toTag: null,
        overwrite: false
    ]
    def config = defaults + params

    if (!config.repoName) {
        echoErr "repoName is required!"
        return null
    }

    if (!config.fromTag) {
        echoErr "fromTag is required!"
        return null
    }

    if (!config.toTag) {
        echoErr "toTag is required!"
        return null
    }

    def manifest = getTagManifest(config)

    if (!manifest) {
        echoLog "Could not find image tag ${config.fromTag}"
        return null
    }

    if (config.overwrite) {
        overwriteCfg = config + [tagRegex: /^${config.toTag}$/]
        awsEcrDeleteTags overwriteCfg
    }

    echoLog "Copying ${config.fromTag} to ${config.toTag}"
    return putImage(config, manifest)
}

def getTagManifest(config) {
    def registryArgs = config.awsAccountId ? "--registry-id ${config.awsAccountId}" : ''

    def manifest = sh(script: loadMixins() + """
        aws ${Util.commonAwsArgs(config)} ecr batch-get-image \
            ${registryArgs} \
            --repository-name ${config.repoName} \
            --image-ids imageTag=${config.fromTag} \
            --query 'images[].imageManifest' \
            --output text
    """, returnStdout: true).trim()

    return manifest
}

def putImage(config, manifest) {
    def registryArgs = config.awsAccountId ? "--registry-id ${config.awsAccountId}" : ''

    writeFile file: "manifest", text: manifest

    sh loadMixins() + """
        aws ${Util.commonAwsArgs(config)} ecr put-image \
            ${registryArgs} \
            --repository-name ${config.repoName} \
            --image-tag ${config.toTag} \
            --image-manifest file://manifest
    """
    return true
}
