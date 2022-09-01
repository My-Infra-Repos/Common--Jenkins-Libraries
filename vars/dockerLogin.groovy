import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Log into a Docker registry.
 *
 * Required params:
 * @param awsAccountId String The AWS account id, e.g. '1122334455667788'. Required when registryType is 'ecr'.
 * @param awsRegion String The region to use for AWS ECR. Required when registryType is 'ecr'. Defaults to 'us-east-1'.
 * @param credentialsId String The name or id of a Jenkins username/password credential. Required when registryType is 'docker'.
 *
 * Optional params:
 * @param registryType String The type of Docker Registry. One of 'docker' or 'ecr' (default).
 * @param awsProfileName String The name of the aws profile to use. Default is 'default'.
 * @param dockerHub String The base URL for the (non-ECR) Docker Hub. This is ignored if registryType is 'ecr'. Defaults to Constants.ARTIFACTORY_DOCKER_HUB.
 *
 * @return true or false depending on if the login was successful.
 */
def call(Map<String, Object> params) {
    defaults = [
        registryType: "ecr",
        credentialsId: null,
        awsRegion: "us-east-1",
        awsAccountId: null,
        awsProfileName: 'default',
        dockerHub: Constants.ARTIFACTORY_DOCKER_HUB
    ]
    def config = defaults + params

    def supportedRegistryTypes = ["docker", "ecr"]
    def dockerHostArgs = Util.getDockerHostArgs(this)
    def output

    if (config.registryType.toLowerCase() == "docker") {
        withCredentials([
            usernamePassword(credentialsId: "${config.credentialsId}", usernameVariable:'USER', passwordVariable: 'PASSWORD')
        ]) {
            output = sh(script: loadMixins() + "docker ${dockerHostArgs} login -u ${USER} -p ${PASSWORD} ${config.dockerHub}", returnStdout: true)
        }
    } else if (config.registryType.toLowerCase() == "ecr") {
        def ecrHub = "${config.awsAccountId}.dkr.ecr.${config.awsRegion}.amazonaws.com"

        if (!env.AWSCLI_VERSION) {
            echoErr "Cannot determine awscli version, AWSCLI_VERSION is unset!"
            return null
        }

        def semver = Util.parseSemver(env.AWSCLI_VERSION)
        def major = semver.major as Integer
        if (major >= 2) {
            // AWS CLI v2.x
            output = sh(script: loadMixins() + """
                aws ${Util.commonAwsArgs(config)} ecr get-login-password |\
                    docker login --username AWS --password-stdin ${ecrHub}
            """, returnStdout: true)
        } else {
            // AWS CLI v1.x
            output = sh(script: loadMixins() + """
                KEY=\$(aws ${Util.commonAwsArgs(config)} ecr get-login | awk '{ print \$6 }')
                docker ${dockerHostArgs} login -u AWS -p \$KEY https://${ecrHub}
            """, returnStdout: true)
        }
    } else {
        println("${config.registryType} is not a supported registry type! Must be one of: ${supportedRegistryTypes}")
        return false
    }
    return output.contains("Login Succeeded")
}
