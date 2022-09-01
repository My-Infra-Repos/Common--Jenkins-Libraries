import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Remove label to pull request. Assumes that it is run inside of a pull request build.
 *
 * Required params:
 * @param credentialsId String The name or id of a Jenkins token credential used for GitHub authentication
 * @param labelName     String The name of the label to tag the github issue or pull request.
 * @param issueID Number The id of the pull request.
 *
 * Optional params:
 * @param gitHubApiUrl String The GitHub API URL. Default is Constants.GITHUB_API_URL
 *
 * Returns issue label ID.
 */
def call(Map<String, Object> params) {
    defaults = [
        credentialsId: null,
        labelName: null,
        issueID: null,
        gitHubApiUrl: Constants.GITHUB_API_URL
    ]

    def config = defaults + params

    withCredentials([
        string(credentialsId: config.credentialsId, variable: 'token'),
    ]) {
        try {
            prURL = "${confg.gitHubApiUrl}/repos/${Util.getProjectName(this)}/issues/${config.issueID}/labels/${config.labelName}".trim()
            def responseCode = sh(script: """
                curl -sS \
                     -X DELETE \
                     -H 'Authorization: token ${token}' \
                     -w %{http_code} \
                     '${prURL}'
            """, returnStdout: true).trim()

            // Return true if 204: https://docs.github.com/en/rest/reference/repos#delete-a-commit-comment
            return responseCode == '204' ? true : false
        } catch (err) {
            echoErr 'Could not delete label'
            return null
        }
    }
}
