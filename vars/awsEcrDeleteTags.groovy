import com.myorg.common.utils.Util

/**
 * Delete tags in ECR.
 *
 * Required params:
 * @param repoName String The full ECR repo name. Do not include the ECR URL.
 * @param tagRegex Regex The pattern to match tag names against.
 * @param awsAccountId The AWS account associated with the registry.
 *
 * Optional params:
 * @param awsProfileName String The name of the AWS profile to use in the AWS credentials file. Default is 'default'.
 * @param awsRegion String The name of the aws region to use.
 *
 * @return List of deleted tags.
 */
def call(Map<String, Object> params) {
    defaults = [
        awsProfileName: "default",
        awsRegion: null,
        awsAccountId: null,
        repoName: null,
        tagRegex: null
    ]
    def config = defaults + params

    if (!config.repoName) {
        echoErr "repoName is required!"
        return null
    }

    if (!config.tagRegex) {
        echoErr "tagRegex is required!"
        return null
    }

    def matchedTags = getMatchingTags(config)

    if (matchedTags) {
        echoLog "Will delete image tags: ${matchedTags}"
        return deleteTags(config, matchedTags)
    } else {
        echoLog "No image tags matching regex /${config.tagRegex}/"
        return null
    }
}

def getMatchingTags(config) {
    def jsonOutput = sh(script: loadMixins() + """
        aws ${Util.commonAwsArgs(config)} ecr describe-images \
            --repository-name ${config.repoName} \
            --output json
    """, returnStdout: true)

    def obj = readJSON(text: jsonOutput)
    def imageDetails = obj['imageDetails']

    def matches = []
    imageDetails.each { detail ->
        detail['imageTags'].each { tag ->
            if (tag =~ config.tagRegex) {
                matches << tag
            }
        }
    }

    return matches
}

def deleteTags(config, matchedTags) {
    def registryArgs = config.awsAccountId ? "--registry-id ${config.awsAccountId}" : ''
    def tagArgs = matchedTags.collect({ "imageTag=${it}" }).join(' ')

    sh loadMixins() + """
        aws ${Util.commonAwsArgs(config)} ecr batch-delete-image \
            ${registryArgs} \
            --repository-name ${config.repoName} \
            --image-ids ${tagArgs}
    """
    return matchedTags
}
