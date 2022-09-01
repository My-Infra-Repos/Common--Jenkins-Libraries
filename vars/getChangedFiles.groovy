/**
 * Helper function for returning a list of the changed files in a commit.
 *
 * Optional params:
 * @param compareTarget String The commit hash or branch name to compare against. Default is 'HEAD^1'.
 * @param matchPatterns List A list of regex strings to use to filter the returned files.
 * @param fileStatuses String The file statuses according to git. Default is 'ACMR' for added, commit.
 *
 * @return List of matching file paths
 *
 * Example use in Jenkinsfile:
 * ```
 * stage ("Get Changed Files") {
 *     branch "master"
 *     steps {
 *         script {
 *             mergeInfo = getLastMergeInfo()
 *             compareHash = mergeInfo.mergeCommitHash
 *             changedFiles['myKey'] = getChangedFiles compareTarget: compareHash,
 *                 matchPatterns: ["some/path/.+", "another/path/.+"]
 *         }
 *     }
 * }
 *
 * stage ("Do Something") {
 *     when {
 *         branch "master"
 *         expression {
 *             // Must be an explicit true/false expression
 *             changedFiles['myKey'] && changedFiles['myKey'] != [:]
 *         }
 *     }
 *     steps {
 *         echoLog "Do Something"
 *         script {
 *             ...
 *         }
 *     }
 * }
 * ```
 */
def call(Map<String, Object> params) {
    defaults = [
        compareTarget: "HEAD^1",
        // Added (A), Copied (C), Deleted (D), Modified (M), Renamed (R)
        fileStatuses: "ACDMR",
        matchPatterns: []
    ]
    def config = defaults + params

    def matchedFiles
    def allFiles = getFiles(config)

    if (config.matchPatterns) {
        matchedFiles = filterFiles(config, allFiles)
    } else {
        matchedFiles = allFiles
    }

    return matchedFiles
}

def getFiles(Map<String, Object> config) {
    def files = sh(script: """
        git diff ${config.compareTarget} \
            --diff-filter=${config.fileStatuses} \
            --name-only
    """, returnStdout: true).trim()
    return files.split("\n")
}

def filterFiles(Map<String, Object> config, files=[]) {
    matchPatterns = config.matchPatterns
    matchedFiles = []

    matchPatterns.each { pattern ->
        matches = files.findAll { it.matches(pattern) }
        matchedFiles += matches
    }

    return matchedFiles
}
