/**
 * Genererate a valid myorgfile.
 *
 * Required params:
 * @param appName String The name of application.
 *
 * Optional params:
 * See
 *
 * @return a Map of the myorgfile values
 */
def call(Map<String, Object> params) {
    defaults = [
        appName: '',
        askId: 'poc',
        caAgileId: 'poc',
        projectPrefix: 'poc',
        componentType: 'code',
        targetQG: 'GATE_00'
    ]

    def config = defaults + params

    valuesMap = [
        apiVersion: 'v1',
        metadata: [
            askId: config.askId,
            caAgileId: config.caAgileId,
            projectKey: "${config.projectPrefix}:${config.appName}",
            projectFriendlyName: config.appName,
            componentType: config.componentType,
            targetQG: config.targetQG
        ]
    ]

    writeYaml file: 'myorgfile.yml', data: valuesMap, overwrite: true

    return valuesMap
}


