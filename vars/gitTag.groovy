/**
 * Create a new tag in the git repo.
 *
 * Required params:
 * @param tagName String The name of the tag to push.
 *
 * Optional params:
 * @param remoteName String The name of the remote. Defaults to 'origin'.
 * @param overwrite Boolean Overwrite the tag if it exists.
 *
 * @return String The name of the tag.
 */
def call(Map<String, Object> params) {
    defaults = [
        tagName: null,
        remoteName: 'origin',
        overwrite: false
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

    def tagExists = gitTagExists(config)
    if (tagExists) {
        if (config.overwrite) {
            echoLog "Will overwrite git tag ${config.tagName}"
            gitDeleteTag(config)
        } else {
            echoErr "Git tag ${config.tagName} already exists!"
            return false
        }
    }

    return createAndPushTag(config)
}

def createAndPushTag(config) {
    def message = "Automated tag for ${config.tagName}"

    sh """
        git tag -a ${config.tagName} -m "${message}"
        git push ${config.remoteName} ${config.tagName}
    """

    return config.tagName
}
