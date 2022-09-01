import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Build and tag a docker image.
 *
 * Required params:
 * @param imageName String The name of the image. This includes the organization (parent folder) name, e.g. 'cloud-idp/some-image'.
 *
 * Optional params:
 * @param tagName String The name to use for the tag. Default is 'latest'.
 * @param dockerHub String The base URL for the Docker Hub. Defaults to Constants.ARTIFACTORY_DOCKER_HUB.
 * @param dockerFile String The path to the Dockerfile. Defaults to './Dockerfile'.
 * @param buildArgs Map A map of additional build args to pass to the build command. Default is an empty map.
 * @param labels Map A map of labels to pass to the build command. Default is an empty map.
 *
 * @return the tagged image URL
 */
def call(Map<String, Object> params) {
    defaults = [
        imageName: null,
        tagName: "latest",
        dockerHub: Constants.ARTIFACTORY_DOCKER_HUB,
        dockerfile: "./Dockerfile",
        buildArgs: [:],
        labels: [:]
    ]

    def config = defaults + params

    def dockerHostArgs = Util.getDockerHostArgs(this)
    def taggedImageUrl = "${config.dockerHub}/${config.imageName}:${config.tagName}"

    def additionalArgs = ""
    labels.each { key, val ->
        additionalArgs += " --label '${key}=${val}'"
    }
    buildArgs.each { key, val ->
        additionalArgs += " --build-arg '${key}=${val}'"
    }

    sh loadMixins() + "docker ${dockerHostArgs} build -f ${config.dockerfile} ${additionalArgs} --force-rm --pull -t ${taggedImageUrl} ."
    return taggedImageUrl
}
