import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Push a existing docker image up to a registry. Assumes that the image already exists locally.
 *
 * Required params:
 * @param image String The name of the image including the tag, e.g. 'cloud-idp/some-image:latest'.
 * @param images List A list of images including the tag, e.g. ['cloud-idp/some-image:latest'].
 *
 * @return true
 */
def call(Map<String, Object> params) {
    defaults = [
        image: null,
        images: []
    ]
    def config = defaults + params

    if (!config.image && !config.images) {
        echoErr "image or images is required!"
        return null
    }

    def dockerHostArgs = Util.getDockerHostArgs(this)

    def cmd
    if (config.image) {
        cmd = "docker ${dockerHostArgs} push ${config.image}"
    }  else {
        def cmds = config.images.collect { img -> "docker ${dockerHostArgs} push ${img}"}
        cmd = cmds.join('\n')
    }

    sh loadMixins() + cmd
    return true
}
