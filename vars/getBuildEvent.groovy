import com.myorg.common.utils.Util

/**
 * Helper function for returning information about the build event, including the user who triggered it.
 *
 * Required params:
 * @param credentialsId String The name of Jenkins credential to use for ldap bind. Required for looking up a user by email.
 *
 * @return Map of build attributes:
 * [username: '', email: '', eventType: 'BRANCH']
 */
def call(Map<String, Object> params) {
    defaults = [
        credentialsId: null
    ]
    def config = defaults + params

    def buildInfo = getBuildInfo(config)
    echoLog buildInfo
    return buildInfo
}

def getBuildInfo(Map<String, Object> config) {
    // Examples:
    // USER: [{"_class":"hudson.model.Cause$UserIdCause","shortDescription":"Started by user Winger, Bryon S","userId":"bwinge1","userName":"Winger, Bryon S"}]
    // REPLAY: [{"_class":"hudson.model.Cause$UserIdCause","shortDescription":"Started by user Winger, Bryon S","userId":"bwinge1","userName":"Winger, Bryon S"},{"_class":"org.jenkinsci.plugins.workflow.cps.replay.ReplayCause","shortDescription":"Replayed #94"}]

    def currentCauses = [
        'BRANCH': currentBuild.getBuildCauses('jenkins.branch.BranchEventCause'),
        'TIMER': currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause'),
        'REPLAY': currentBuild.getBuildCauses('org.jenkinsci.plugins.workflow.cps.replay.ReplayCause'),
        'USER': currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
    ]

    def primaryCause, secondaryCause

    if (currentCauses['BRANCH']) {
        // started by git push
        primaryCause = currentCauses['BRANCH'][0]

        def userEmail
        if (env.CHANGE_AUTHOR_EMAIL) {
            userEmail = env.CHANGE_AUTHOR_EMAIL
        } else {
            userEmail = Util.getAuthorEmail(this)
        }

        if (userEmail) {
            def credentials = config.credentialsId
            if (!credentials) {
                echoWarn "credentialsId is required to lookup users by email!"
                return [username: 'UNKNOWN', email: userEmail, eventType: 'BRANCH']
            } else {
                def user = getActiveDirectoryDetails credentialsId: credentials, email: userEmail
                return [username: user.username, email: user.email, eventType: 'BRANCH']
            }
        }
        return [username: 'UNKNOWN', email: 'UNKNOWN', eventType: 'BRANCH']
    } else if (currentCauses['TIMER']) {
        // started by timer
        primaryCause = currentCauses['TIMER'][0]
        return [username: 'TIMER', eventType: 'TIMER']
    } else if (currentCauses['REPLAY']) {
        // started by Jenkins Replay
        primaryCause = currentCauses['REPLAY'][0]
        secondaryCause = currentCauses['USER'][0]
        return [username: secondaryCause['userId'], eventType: 'REPLAY']
    } else if (currentCauses['USER']) {
        // started by user
        primaryCause = currentCauses['USER'][0]
        return [username: primaryCause['userId'], eventType: 'USER']
    } else {
        // Some other unexpected cause
        def klass = currentBuild.buildCauses()[0]['_class']
        primaryCause = currentBuild.getBuildCauses(klass)

        echoWarn "unexpected build cause: ${primaryCause}"
        return [username: 'UNKNOWN', eventType: 'OTHER']
    }
}
