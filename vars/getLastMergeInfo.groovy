import com.myorg.common.utils.Util

/**
 * Helper function for returning a map of last merge commit information.
 *
 * No params.

 * @return Map:
 * [
 *   mergeBase: 'f8ac844aa4ad5c6dfd32a1fbac7b9cc0b03782a2',
 *   mergeHead: '14d22351a5c13fbd3b274c37237a88c3fbe89f11',
 *   mergeBranch: 'my-feature'
 *   mergeCommitHash: '3c6866cabafcd5e02ac78c770e8d30e58bfc609a',
 *   pullRequestNumber: '122',
 *   mergeType: 'PR',
 *   isPullRequestMerge: true,
 *   isBranchMerge: false,
 *   isLocalMerge: false,
 *   headIsMerge: true,
 *   logMessage: 'Merge pull request #122...'
 * ]
 */
def call() {
    def (mergeHash, leftHash, rightHash) = getRecentMergeHashes()

    def currentBranchName = Util.getCurrentBranchName(this)
    def headSha = Util.getHeadSha(this)

    def rightBranchName, mergeType, prNumber, commitMsg
    if (rightHash) {
        (rightBranchName, commitMsg, mergeType, prNumber) = getMergeCommitInfo(mergeHash)
    }

    def info = [
        mergeBase: leftHash,
        mergeHead: rightHash,
        mergeBranch: rightBranchName,
        mergeCommitHash: mergeHash,
        pullRequestNumber: prNumber,
        mergeType: mergeType,
        isPullRequestMerge: (mergeType == 'PR'),
        isBranchMerge: (mergeType == 'BRANCH'),
        isLocalMerge: (mergeType == 'LOCAL'),
        headIsMerge: (headSha == mergeHash),
        logMessage: commitMsg
    ]
    return info
}

def getRecentMergeHashes() {
    // Fetch the commit hashes from the last merge commit
    // Ex. COMMIT_HASH LEFT_HASH RIGHT_HASH
    def hashesStr = sh(script: "git log --merges -1 --pretty='%H %P'", returnStdout: true).trim()

    // mergeHash is the resulting commit
    // leftHash is the merge base (ex. 'master' before the commit)
    // rightHash is the HEAD commit of the branch being merged in
    // Null is added to avoid exceptions if there is no right side (no merges in history)
    def (mergeHash, leftHash, rightHash) = hashesStr.split() + null
    return [mergeHash, leftHash, rightHash]
}

def getMergeCommitInfo(commitSha) {
    def mergeType, prNumber, branchName
    def message = sh(script: "git show ${commitSha} --pretty=%s", returnStdout: true).trim()

    // This only works for non-fast-forward merges (which don't appear as merges anyway)
    if (message.startsWith('Merge pull request')) {
        mergeType = 'PR'
        prNumber = getPullRequestNumberFromMsg(message)
        branchName = getPullRequestBranchFromCommitMsg(message)
    } else if (message.startsWith('Merge branch')) {
        mergeType = 'BRANCH'
        branchName = getMergeBranchFromCommitMsg(message)
    } else if (message.startsWith('Merge commit')) {
        // The merge was performed locally in the Jenkins build
        mergeType = 'LOCAL'
        branchName = env.CHANGE_TARGET || env.BRANCH_NAME
    }

    return [branchName, message, mergeType, prNumber]
}

def getPullRequestNumberFromMsg(msg) {
    // Merge pull request #1267 from cloud-idp/some-branch-name
    return msg.split()[3] - "#"
}

def getPullRequestBranchFromCommitMsg(msg) {
    // Merge pull request #1267 from cloud-idp/some-branch-name
    def upstreamRef = msg.split()[-1]
    // Drop the first part from the ref (the org name)
    def refName = upstreamRef.split('/')[1..-1].join('/')
    return refName
}

def getMergeBranchFromCommitMsg(msg) {
    // Ex:
    //     Merge branch 'my-branch'
    //     * OR *
    //     Merge branch 'my-branch' into some-feature-branch
    def str = msg.split()[2]
    return str.replace("'", "")
}
