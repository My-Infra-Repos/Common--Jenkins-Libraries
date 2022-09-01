/**
 * Fetch the value of a secret that may exist in more than one region from AWS Secrets Manager
 *
 * Required params:
 * @param secretName String The name of the secret.
 *
 * Optional params:
 * @param regions List<String> The regions to try to fetch the value of the secrets from. (Default: [us-east-1])
 *
 * @return a JSON object
 */
def call(Map<String, Object> params) {
    defaults = [
        awsProfileName: 'default',
        secretName : null,
        awsRegions: ["us-east-1"]
    ]
    def config = defaults + params

    def jsonObj
    // Try to fetch secret value from each region
    for (region in config.awsRegions) {
        jsonObj = getAwsSecret awsProfileName: config.awsProfileName, secretName: config.secretName, awsRegion: region
        if (jsonObj) {
            println("Found secret ${config.secretName} in region: ${region}")
            break
        }
    }

    if (!jsonObj) {
        echoErr "${config.secretName} not found in any of the following regions: ${config.awsRegions}"
        return null
    }

    return jsonObj
}
