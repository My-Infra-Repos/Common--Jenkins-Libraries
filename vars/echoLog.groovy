/**
 * Helper function for calling echoColor for normal messages.
 *
 * Required params:
 * @param msg String The message to log.
 * @return null
 */
def call(msg) {
    params = [
        level : "OK",
        msg: msg,
        reverse: false,
        prefix: false
    ]

    echoColor(params)
}
