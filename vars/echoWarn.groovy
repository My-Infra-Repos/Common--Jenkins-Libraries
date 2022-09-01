/**
 * Helper function for calling echoColor for warnings.
 *
 * Required params:
 * @param msg String The message to log.
 * @return null
 */
def call(msg) {
    params = [
        level : "WARN",
        msg: msg,
        reverse: false,
        prefix: true
    ]

    echoColor(params)
}
