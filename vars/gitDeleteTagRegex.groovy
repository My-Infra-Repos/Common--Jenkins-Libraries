/**
 * Delete a tag in the git repo.
 *
 * Required params:
 * @param tagRegex Regex The pattern to match against tags.
 * @param remoteName String The name of the remote. Defaults to 'origin'.
 *
 * @return List The list of deleted tags
 */

import com.myorg.common.utils.Util

def call(Map<String, Object> params) {
    defaults = [
        tagRegex: null,
        remoteName: 'origin'
    ]
    def config = defaults + params

    if (!config.tagRegex) {
        echoErr "tagRegex is required!"
        return null
    }

    if (!config.remoteName) {
        echoErr "remoteName is required!"
        return null
    }

    def tags = Util.listGitTags(this, config.remoteName)
    def matchedTags = findMatchingTags(tags, config.tagRegex)

    if (matchedTags) {
        echoLog "Will delete git tags: ${matchedTags}"
        matchedTags.each { tag ->
            gitDeleteTag([remoteName: config.remoteName, tagName: tag])
        }
    } else {
        echoLog "No git tags matching regex /${config.tagRegex}/"
        return null
    }

    return matchedTags
}

def findMatchingTags(tags, regex) {
    return tags.findAll { it =~ regex }
}
