/**
 * Setup configuration for common pipelines. For some types this includes generating the myorgfile.
 *
 * Optional params:
 * @param pipelineDefaults Map Config overrides for the pipeline.
 * @param fileName String The name of the config file.
 *
 * @return Map fullCfg
 */
def call(Map<String, Object> params) {
    defaults = [
        pipelineDefaults: [:],    // The defaults set in the pipeline script
        pipelineType: 'DOCKER',   // This is only used for determining which defaults to use
        fileName: 'ci.yaml',      // The file in the application repo with overrides
    ]
    def config = defaults + params

    if (!config.fileName) {
        echoErr "fileName is required!"
        return null
    }

    def ciYaml = [:]
    def fullCfg = [:]

    // Read the ci.yaml configuration for app-level overrides.
    if (fileExists(config.fileName)) {
        ciYaml = readYaml file: config.fileName
    } else {
        echoLog "No ${config.fileName} file found. Using default values."
    }

    // We can define a different set of defaults for other pipeline types (Lambda, UI, etc).
    // These also generate the myorgfile as needed.
    if (config.pipelineType == 'DOCKER') {
        fullCfg = mergeDockerCIConfig(ciYaml, config.pipelineDefaults)
    // } else if (config.pipelineType == 'GOLANG') {
    //     fullCfg = mergeGolangCIConfig(ciYaml, config.pipelineDefaults)
    // } else if (config.pipelineType == 'PYTHON') {
    //     fullCfg = mergePythonCIConfig(ciYaml, config.pipelineDefaults)
    }

    return fullCfg
}
