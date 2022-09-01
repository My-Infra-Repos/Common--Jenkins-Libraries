import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Pull a docker image from to a registry.
 *
 * Required params:
 * @param taggedImage String The name of the image including parent folder and tag, e.g. 'cloud-idp/some-image:latest'.
 *
 * Optional params:
 * @param dockerHub String The base URL for the Docker Hub. Defaults to Constants.ARTIFACTORY_DOCKER_HUB.
 *
 * @return true
 */
def call(Map<String, Object> params) {
    defaults = [
        taggedImage : null,
        dockerHub : Constants.ARTIFACTORY_DOCKER_HUB,

    ]
    def config = defaults + params

    def dockerHostArgs = Util.getDockerHostArgs(this)

    sh loadMixins() + "docker ${dockerHostArgs} pull ${config.dockerHub}/${config.taggedImage}"
    return true
}
