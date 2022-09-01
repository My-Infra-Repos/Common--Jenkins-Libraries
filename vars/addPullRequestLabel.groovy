import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Add label to pull request. Assumes that it is run inside of a pull request build.
 *
 * Required params:
 * @param credentialsId String The name or id of a Jenkins token credential used for GitHub authentication
 * @param labelName     String The name of the label to tag the github issue or pull request.
 * @param issueID       Number The id of the pull request.
 *
 * Optional params:
 * @param gitHubApiUrl String The url for the github issue or pull request.
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
            prURL = "${confg.gitHubApiUrl}/repos/${Util.getProjectName(this)}/issues/${config.issueID}/labels".trim()
            def responseStr = sh(script: """
                curl -sS \
                     -X POST \
                     -H 'Content-Type: application/json' \
                     -H 'Authorization: token ${token}' \
                     -d '{"labels":["${config.labelName}"]}' \
                     '${prURL}'
            """, returnStdout: true).trim()

            def responseObj = readJSON(text: responseStr)
            responseObjStr = ""
            responseObjStrSep = ""
            responseObj.each {
                responseObjStr = responseObjStrSep + responseObjStr + it.id;
                responseObjStrSep = ",";
            }

            // Parsing errors still return a 201 / 0 exit code. A label id means it posted successfully.
            if (responseObjStr != "") {
                return responseObj.id
            } else {
                echoErr responseObj
                return null
            }
        } catch (err) {
            echoErr 'Could not delete label'
            return null
        }
    }
}
