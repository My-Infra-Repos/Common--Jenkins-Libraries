/**
 * Execute a curl command with options.
 *
 * Required params:
 * @param endpoint String The domain or URL to make the request against.
 *
 * Optional params:
 * @param data String a JSON formatted string of data to be sent.
 * @param silent Boolean Whether to show progress or not.
 * @param params Array A list of query params to set in the request.
 * @param headers Array A list of headers to set in the request.
 * @param verb String The http verb to use. Default is 'GET'.
 *
 * @return the output of the call
 */
def call(Map<String, Object> params) {
    defaults = [
        endpoint: '',
        silent: true,
        data: null,
        params: [],
        verb: 'GET',
        headers: []
    ]

    def config = defaults + params

    if (!config.endpoint) {
        echoErr "endpoint is required!"
        return null
    }

    def queryParams = config.params ? config.params.join('&') : null
    def url = queryParams ? "${config.endpoint}?${queryParams}" : config.endpoint
    def verbOpts = config.verb.toUpperCase() == 'GET' ? "" : "-X ${config.verb.toUpperCase()}"
    def headerOpts = config.headers.collect({ "-H \"${it}\"" }).join(' ')
    def dataOpts = config.data ? "-d '${config.data}'": ""
    def silentOpts = config.silent ? "-s" : ""

    def cmd = "curl ${silentOpts} ${verbOpts} ${headerOpts} \"${url}\" ${dataOpts}"
    def output = sh(script: cmd, returnStdout: true)
    return output
}
