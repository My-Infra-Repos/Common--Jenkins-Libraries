/**
 * Delete a tag in the git repo.
 *
 * Required params:
 * @param remoteName String The name of the remote. Defaults to 'origin'.
 * @param tagName String The name of the tag to delete.
 *
 * @return String The name of the deleted tag.
 */
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

    return deleteTag(config)
}

def deleteTag(config) {
    sh "git push ${config.remoteName} :${config.tagName}"
    return config.tagName
}
