import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Add comment to pull request. Assumes that it is run inside of a pull request build.
 *
 * Required params:
 * @param credentialsId String The name or id of a Jenkins token credential used for GitHub authentication.
 * @param comment       String The comment to post to the pull request
 * @param issueID       Number The id of the pull request.
 *
 * Optional params:
 * @param gitHubApiUrl String The GitHub API URL. Defaults to Constants.GITHUB_API_URL.
 *
 * Returns issue comment ID.
 */
def call(Map<String, Object> params) {
    defaults = [
        credentialsId : null,
        comment: '',
        issueID: null,
        gitHubApiUrl: Constants.GITHUB_API_URL
    ]
    def config = defaults + params

    withCredentials([
        string(credentialsId: config.credentialsId, variable: 'token'),
    ]) {
        try {
            def jsonFile = 'comment.json'
            def jsonData = ['body': config.comment]
            writeJSON file: jsonFile, json: jsonData

            prCommentsURL = "${config.gitHubApiUrl}/repos/${Util.getProjectName(this)}/issues/${config.issueID}/comments".trim()
            def responseStr = sh(script: """
                curl -sS \
                     -X POST \
                     -H 'Content-Type: application/json' \
                     -H 'Authorization: token ${token}' \
                     -d @${jsonFile} \
                     '${prCommentsURL}'
            """, returnStdout: true).trim()

            def responseObj = readJSON(text: responseStr)

            // Parsing errors still return a 201 / 0 exit code. A comment id means it posted successfully.
            if (responseObj.id) {
                return responseObj.id
            } else {
                echoErr responseObj
                return null
            }
        } catch (err) {
            echoErr 'Could not post comment'
            return null
        }
    }
}
