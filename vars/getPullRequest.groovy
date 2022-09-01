import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Lookup a Pull Request from a gitrepo using the GitHub API.
 *
 * Required params:
 * @param credentialsId String The name a Jenkins username/password credential where the password is a GitHub token. Default is Constants.GITHUB_USER_AND_TOKEN_CREDENTIAL.
 * @param id String The ID of the Pull Request.
 *
 * Optional params:
 * @param projectSlug String The org and repo name slug as found in GitHub, including the slash like 'myorg/myrepo'. Default uses the current repository for the build.
 * @param gitHubApiUrl String The GitHub API URL. Default is Constants.GITHUB_API_URL
 *
 * Returns a map of release tag information.
 */
def call(Map<String, Object> params) {
    thisProjectSlug = Util.getProjectName(this)

    defaults = [
        credentialsId : Constants.GITHUB_USER_AND_TOKEN_CREDENTIAL,
        id: null,
        projectSlug: thisProjectSlug,
        githubApiUrl: Constants.GITHUB_API_URL
    ]
    def config = defaults + params

    if (!config.credentialsId) {
        echoErr "credentialsId is required!"
        return null
    }

    if (!config.id) {
        echoErr "id is required!"
        return null
    }

    def prInfo = lookupPullRequest(config)
    return prInfo
}

def lookupPullRequest(config) {
    withCredentials([
        usernamePassword(credentialsId: config.credentialsId, usernameVariable: 'GIT_USER', passwordVariable: 'TOKEN')
    ]) {
        try {
            def pullRequestUrl = "${config.githubApiUrl}/repos/${config.projectSlug}/pulls/${config.id}"
            def responseStr = sh(script: """
                curl -sS \
                     -H 'Content-Type: application/json' \
                     -H 'Authorization: token ${TOKEN}' \
                     '${pullRequestUrl}'
            """, returnStdout: true).trim()

            def responseObj = readJSON(text: responseStr)

            if (responseObj.body) {
                return responseObj
            } else {
                echoErr responseObj
                return null
            }
        } catch (err) {
            echoErr "Could not get Pull Request #${config.id}"
            return null
        }
    }
}
