import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Tag an existing docker image. Assumes that the image already exists locally.
 *
 * @param fromImage String The full name of the local image including any tag, e.g. 'cloud-idp/some-image:v1.2.3'.
 * @param toImage String The new image name or URL including tag.
 *
 * @return the tagged image URL
 */
def call(Map<String, Object> params) {
    defaults = [
        fromImage : null,
        toImage : null
    ]
    def config = defaults + params

    if (!config.fromImage) {
        echoErr "fromImage is required!"
        return null
    }

    if (!config.toImage) {
        echoErr "toImage is required!"
        return null
    }

    def dockerHostArgs = Util.getDockerHostArgs(this)

    sh loadMixins() + "docker ${dockerHostArgs} tag ${config.fromImage} ${config.toImage}"
    return newTaggedImageUrl
}
