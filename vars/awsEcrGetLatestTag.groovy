import com.myorg.common.utils.Util

/**
 * Return the latest ECR tag for an image matching some prefix.
 *
 * Required params:
 * @param repoName String The full ECR repo name. Example: 'cc-eac/my-application'.

 * Optional params:
 * @param awsProfileName String The name of the AWS profile to use in the AWS credentials file. Default is 'default'.
 * @param awsRegion String The name of the aws region to use.
 * @param awsAccountId String The AWS account id, e.g. '1122334455667788'.
 * @param tagPrefix The string prefix at the beginning of the image tag to match against. Example: 'deploy-'. Default is 'v'.
 *
 * @return List of matching file paths
 */
def call(Map<String, Object> params) {
    defaults = [
        awsProfileName: "default",
        awsRegion: null,
        awsAccountId: null,
        repoName: "",
        tagPrefix: "v"
    ]
    def config = defaults + params

    def allImages = getEcrImages(config)
    def matchingImages = filterImagesByTagPrefix(config, allImages)

    if (matchingImages) {
        return findLatestImageTag(matchingImages)
    } else {
        echoWarn "No image tag for ${config.repoName} matching '${config.tagPrefix}*'"
        return null
    }
}

def getEcrImages(config) {
    def registryArgs = config.awsAccountId ? "--registry-id ${config.awsAccountId}" : ''

    def response = sh(script: """
            aws ${Util.commonAwsArgs(config)} ecr describe-images \
                ${registryArgs} \
                --repository-name ${config.repoName} \
                --filter tagStatus=TAGGED
        """,
        returnStdout: true
    )

    def jsonResponse = readJSON(text: response)
    return jsonResponse.imageDetails
}

def filterImagesByTagPrefix(config, images) {
    return images.findAll { img ->
      img.imageTags.find { it.startsWith(config.tagPrefix) }
    }
}

@NonCPS
def findLatestImageTag(images) {
    def latestImage = images.sort({ it.imagePushedAt })[-1]
    return latestImage.imageTags.find { it.startsWith(tagPrefix) }
}
