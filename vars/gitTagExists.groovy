/**
 * Checks if a ref exists in the git repo.
 *
 * Required params:
 * @param remoteName String The name of the remote. Defaults to 'origin'.
 * @param tagName String The name of the tag to delete.
 *
 * @return Boolean Whether the tag exists or not.
 */

import com.myorg.common.utils.Util

def call(Map<String, Object> params) {
    defaults = [
        remoteName: 'origin',
        tagName: null,
    ]
    def config = defaults + params

    if (!config.tagName) {
        echoErr "tagName is required!"
        return null
    }

    if (!config.remoteName) {
        echoErr "remoteName is required!"
        return null
    }

    def exists = tagExists(config)
    return exists ? true : false
}

def tagExists(config) {
    def tags = Util.listGitTags(this, config.remoteName)
    return tags.find { it == config.tagName }
}
