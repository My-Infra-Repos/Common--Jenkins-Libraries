import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Delete a comment from a pull request. Assumes that it is run inside of a pull request build.
 *
 * Required params:
 * @param credentialsId String The name or id of a Jenkins token credential used for GitHub authentication.
 * @param commentId String The comment ID to delete from the pull request
 *
 * Optional params:
 * @param gitHubApiUrl String The GitHub API URL. Default is Constants.GITHUB_API_URL.
 *
 * Returns boolean indicating whether comment was successfuly deleted or not.
 */
def call(Map<String, Object> params) {
    defaults = [
        credentialsId : null,
        commentId: '',
        gitHubApiUrl: Constants.GITHUB_API_URL
    ]
    def config = defaults + params

    withCredentials([
        string(credentialsId: config.credentialsId, variable: 'token'),
    ]) {
        try {
            prCommentURL = "${config.gitHubApiUrl}/repos/${Util.getProjectName(this)}/issues/comments/${config.commentId}".trim()
            def responseCode = sh(script: """
                curl -sS \
                     -X DELETE \
                     -H 'Authorization: token ${token}' \
                     -w %{http_code} \
                     '${prCommentURL}'
            """, returnStdout: true).trim()

            // Return true if 204: https://docs.github.com/en/rest/reference/repos#delete-a-commit-comment
            return responseCode == '204' ? true : false
        } catch (err) {
            echoErr 'Could not delete comment'
            return false
        }
    }
}
