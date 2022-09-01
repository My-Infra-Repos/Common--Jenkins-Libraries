import com.myorg.common.utils.Constants
import com.myorg.common.utils.Util

/**
 * Configure a git repo for automated pushes. Assumes that it is run inside of a git repository.
 *
 * Required params:
 * No required params.
 *
 * Optional params:
 * @param credentialsId String The name or id of a Jenkins Isername/Password OR SecretText credential used for GitHub authentication. Defaults to Constants.GITHUB_USER_AND_TOKEN_CREDENTIAL.
 * @param username String The username to use for commits. Default is Constants.GIT_USER. If credentialsId is a username/password credential, then that username will be used instead.
 * @param email String The username to use for commits. Default is Constants.GIT_EMAIL.
 * @param branch String The name of the branch to use for checkout. Ignored if already on a branch. Default is $BRANCH_NAME.
 * @param remote String The name of the remote to use. Default is 'origin'.
 *
 * Returns the new semver value.
 */
def call(Map<String, Object> params) {
    defaults = [
        credentialsId : Constants.GITHUB_USER_AND_TOKEN_CREDENTIAL,
        username : Constants.GIT_USER,
        email : Constants.GIT_EMAIL,
        branch : Util.getCurrentBranchName(this),
        remote : "origin",
    ]
    def config = defaults + params

    if (isDetachedHead()) {
        echoWarn "HEAD is detached, checking out ${config.branch}"
        sh loadMixins() + "git checkout ${config.branch}"
    }

    // Handle either UsernamePassword or SecretText credential types
    def credentialObject
    def credentialType = Util.getJenkinsCredentialType(this, config.credentialsId)
    if (credentialType == Constants.SECRET_TEXT_CREDENTIAL_TYPE) {
        credentialObject = string(credentialsId: config.credentialsId, variable: 'TOKEN')
    } else if (credentialType == Constants.USER_PASSWORD_CREDENTIAL_TYPE) {
        credentialObject = usernamePassword(credentialsId: config.credentialsId, usernameVariable: 'CREDENTIAL_USER', passwordVariable: 'TOKEN')
    } else {
        echoErr "SecretText or UsernamePassword credential not found with name '${config.credentialsId}'"
        return false
    }

    withCredentials([credentialObject]) {
        sh loadMixins() + """
            TOKEN_USER="\${CREDENTIAL_USER:-${config.username}}"
            AUTH_URL=\$(git remote get-url ${config.remote} | sed -e "s|https://|https://\${TOKEN_USER}:${TOKEN}@|")
            # E.g. https://USER:TOKEN@github.myorg.com/cloud-idp/everything-as-code
            git remote set-url --push ${config.remote} \$AUTH_URL
            git config user.name ${config.username}
            git config user.email ${config.email}
        """
    }
    return config.username
}

def isDetachedHead() {
    // The current branch name is returned when 'attached'. HEAD otherwise.
    output = sh(script: loadMixins() + "git rev-parse --abbrev-ref --symbolic-full-name HEAD", returnStdout: true).trim()
    return output == "HEAD"
}
