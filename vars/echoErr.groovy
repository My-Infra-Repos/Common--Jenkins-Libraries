/**
 * Helper function for calling echoColor for errors.
 *
 * Required params:
 * @param msg String The message to log.
 * @return null
 */
def call(msg) {
    params = [
        level : "ERROR",
        msg: msg,
        reverse: false,
        prefix: true
    ]

    echoColor(params)
}
