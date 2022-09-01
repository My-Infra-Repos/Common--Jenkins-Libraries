import com.myorg.common.utils.Util

/**
 * Fetch the value of a secret from AWS Secrets Manager
 *
 * Required params:
 * @param secretName String The name of the secret.
 *
 * Optional params:
 * @param awsProfileName String The name of the aws profile to use. Default is 'default'.
 * @param awsRegion String The region where the secret exists.
 *
 * @return a JSON object
 */
def call(Map<String, Object> params) {
    defaults = [
        secretName : null,
        awsProfileName: 'default',
        awsRegion: null
    ]
    def config = defaults + params

    if (!config.secretName) {
        echoErr "secretName is required!"
        return null
    }

    def jsonStr

    try {
        jsonStr = sh(script: loadMixins() + """
            aws ${Util.commonAwsArgs(config)} secretsmanager get-secret-value \
                --secret-id ${config.secretName} \
                --output json
        """, returnStdout: true)
    } catch(_) {
        echoErr "${config.secretName} not found!"
        return null
    }

    def jsonObj = readJSON(text: jsonStr.trim())
    def valueStr = jsonObj.SecretString ?: jsonObj.SecretBinary

    if (!valueStr) {
        echoErr "${config.secretName} has no SecretString or SecretBinary!"
        return null
    }

    // Return SecretString or SecretBinary as object
    def valueObj = readJSON(text: valueStr.trim())

    return valueObj
}
