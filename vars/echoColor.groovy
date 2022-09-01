/**
 * Print a colorized message to the log.
 *
 * Optional params:
 * @param msg String The message to log.
 * @param level String The logging level. One of 'OK', WARN' or 'ERROR'. Default is 'OK'.
 * @param reverse bool When set to 'true', colorize the background instead of the foreground. Default is 'false'.
 * @param prefix bool When set to 'false', any '[xx]' prefix will not be added to the message. Default is 'true'.
 *
 * @return null
 */
def call(Map<String, Object> params) {
    defaults = [
        level : "OK",
        msg: "",
        reverse: false,
        prefix: true
    ]
    def config = defaults + params

    def msg
    def colorCodeStr // Formatted as "FOREGROUND;BACKGROUND;EFFECT", see https://en.wikipedia.org/wiki/ANSI_escape_code

    switch(config.level) {
        case "WARN":
            msg = config.prefix ? "[WARNING] ${config.msg}" : config.msg
            colorCodeStr = config.reverse ? "97;46;1" : "34;1"
            break
        case "ERROR":
            msg = config.prefix ? "[ERROR] ${config.msg}" : config.msg
            colorCodeStr = config.reverse ? "97;41;1" : "31;1"
            break
        default:
            msg = config.msg
            colorCodeStr = config.reverse ? "97;42;1" : "32;1"
            break
    }

    def ansiStartCode = "\033[${colorCodeStr}m"
    def ansiResetCode = "\033[0m"

    ansiColor('xterm') {
        echo "${ansiStartCode}${msg}${ansiResetCode}"
    }
}
