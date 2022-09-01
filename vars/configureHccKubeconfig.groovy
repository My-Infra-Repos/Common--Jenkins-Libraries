import com.myorg.common.utils.Util

/**
 * Genererate a valid Kubeconfig for HCC Kubernetes clusters.
 *
 * Required params:
 * @param namespace String The namespace to use for the current context.
 * @param user String The user name to use for the current context.
 * @param tokenCredentialsId String The name or ID of a Jenkins SecretText credential. The value should be an unencoded k8s ServiceAccount token.

 * Optional params:
 * @param cluster String The short name name of the cluster. Default is 'ctcprdusr001'.
 * @param certificateUrl String Optional. The URL for the cluster certificate. Default is based on the chosen cluster.
 * @param clusterUrl String Optional. The URL of the cluster. Default is based on the chosen cluster.
 * @param clusterIp String Optional. The IP of the cluster. Will be fetched if not passed in.
 * @param kubeconfig String The path to use for the kubeconfig file. Default is "${HOME}/.kube/config".
 *
 * @returns true
 */
def call(Map<String, Object> params) {
    defaults = [
        cluster: 'ctcprdusr001',
        certificateUrl: null,    // Optional
        clusterUrl: null,        // Optional
        clusterIp: null,         // Optional
        namespace: '',
        user: '',
        tokenCredentialsId: '',  // This is assumed to be decoded
        kubeconfig: "${HOME}/.kube/config",
    ]

    def config = defaults + params
    config = addServerInfo(config)

    return generateFiles(config)
}

def addServerInfo(config) {
    def certificatesUrlBase = 'https://github.com/raw/HCC-Container-Platform/hcc-k8s-microsite/master/docs/documentation/certs'
    def certificateUrl = config.certificateUrl  // May be null
    def clusterUrl = config.clusterUrl          // May be null

    // Use defaults for that datacenter if not otherwise passed in
    if (config.cluster == 'ctcprdusr001') {
        certificateUrl = certificateUrl ?: "${certificatesUrlBase}/hcck8s_ctc-ca.crt"
        clusterUrl = clusterUrl ?: 'hcc-ctc-prd-usr001-0.myorg.com'
    } else if (config.cluster == 'elrprdusr001') {
        certificateUrl = certificateUrl ?: "${certificatesUrlBase}/hcck8s_elr-ca.crt"
        clusterUrl = clusterUrl ?: 'hcc-elr-prd-usr001-0.myorg.com'
    }

    def clusterIp = config.clusterIp ?: getClusterIp(clusterUrl)

    clusterCfg = [
        certificateUrl: certificateUrl,
        clusterUrl: clusterUrl,
        clusterIp: clusterIp
    ]
    return config + clusterCfg
}

def getClusterIp(clusterUrl) {
    def ip = sh(script: loadMixins() + "nslookup ${clusterUrl} | grep Address: | tail -n1 | tr -d 'Address: '", returnStdout: true)
    return ip.trim()
}

def downloadCertificateFile(config) {
    // Fetch the server certificate
    def kubeHome = Util.baseDirFromStr(config.kubeconfig)
    def certAuthorityFile = "${kubeHome}/${config.cluster}.crt"
    sh """
        mkdir -p ${kubeHome}
        curl -Ss ${config.certificateUrl} -o ${certAuthorityFile}
    """
    return certAuthorityFile
}

def generateFiles(config) {
    // Fetch the server certificate
    def certAuthorityFile = downloadCertificateFile(config)

    // Generate the kubeconfig
    withCredentials([
        string(credentialsId: config.tokenCredentialsId, variable: 'TOKEN')
    ]) {
        data = [
            'apiVersion': 'v1',
            'clusters': [
                [
                    'name': config.cluster,
                    'cluster': [
                        'certificate-authority': certAuthorityFile,
                        'server': "https://${config.clusterIp}:443"
                    ]
                ]
            ],
            'contexts': [
                [
                    'name': config.namespace,
                    'context': [
                        'cluster': config.cluster,
                        'namespace': config.namespace,
                        'user': config.user
                    ]
                ]
            ],
            'current-context': config.namespace,
            'kind': 'Config',
            'preferences': [:],
            'users': [
                [
                    'name': config.user,
                    'user': [
                        'token': env.TOKEN
                    ]
                ]
            ]
        ]

        writeYaml file: config.kubeconfig, data: data, overwrite: true
    }

    return config.kubeconfig
}
