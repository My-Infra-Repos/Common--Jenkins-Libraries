/**
 * Create the myorgfile as needed.
 *
 * Optional params:
 * @param config Map Options for the myorgfile.
 * @param forceOverwrite Boolean Forces rewriting the file even if it exists.
 *
 * @return Map myorgfile values
 */
def call(Map<String, Object> params) {
    defaults = [
        options: [:],
        forceOverwrite: false
    ]
    def config = defaults + params

    def fileName = 'myorgfile.yml'
    def myorgfile = [:]

    if (!fileExists(fileName) || config.forceOverwrite) {
        echoLog "Generating ${fileName}"
        myorgfile = generatemyorgfile config.options
    } else {
        echoLog "Found existing ${fileName}"
        myorgfile = readYaml file: fileName
    }

    return myorgfile
}
