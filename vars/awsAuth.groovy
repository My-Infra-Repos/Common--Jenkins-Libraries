/**
 * Authenticate with AWS for running CLI commands.
 *
 * Required params:
 * @param credentialsId String The name or id of a Jenkins username/password credential.
 * @param awsAccountId String The AWS account id, e.g. '1122334455667788'.
 *
 * Optional params:
 * @param profileName String The name of the AWS profile to use in the AWS credentials file. Default is 'default'.
 * @param awsRoleArn String The ARN of the AWS role to authenticate with. The default value will be based on 'awsAccountId', e.g. 'arn:aws:iam::<awsAccountId>:role/AWS_<awsAccountId>_Owner'.
 *
 * Optional mixin params:
 *     https://github.com/jenkins/docker_build_agents/tree/master/mixins/jenkins_mixin_awscli
 * @param cliVersion String The CLI version to use in the Jenkins mixin. Defaults to '2.4.1'. This overrides 'AWSCLI_VERSION' if already set in Jenkins.
 * @param samlVersion String The SAML version to use in the Jenkins mixin. Defaults to '0.9.5'. This overrides 'AWSCLI_SAML_VERSION' if already set in Jenkins.
 *
 * @return the AWS profile name.
 */
def call(Map<String, Object> params) {
    defaults = [
        profileName : "default",
        credentialsId : null,
        awsAccountId : null,
        awsRoleArn: null,
        cliVersion: '2.4.1',
        samlVersion: '0.9.5'
    ]
    def config = defaults + params

    def roleArn = config.awsRoleArn ?: "arn:aws:iam::${config.awsAccountId}:role/AWS_${config.awsAccountId}_Owner"

    withCredentials([
        usernamePassword(credentialsId: "${config.credentialsId}", usernameVariable: 'AWS_USER', passwordVariable: 'AWS_PASSWORD'),
    ]) {
        // Since AWS authentication is usually one of the first steps in a job, we want verbose output when loading the mixins in case
        // there are any important warnings to display, like version deprecations.
        sh loadMixins(true) + """
            # Download python script and files to authenticate to AWS
            export AWS_AUTH_DIR=\${HOME}/aws-cli-saml
            mkdir -p \${AWS_AUTH_DIR}
            cd \${AWS_AUTH_DIR}

            for file in authenticate_py3.py prod.cer sandbox.cer; do \
                curl -Ss https://github.com/raw/CommercialCloud-EAC/python-scripts/master/aws_cli_saml_ping_v2/\$file > \${AWS_AUTH_DIR}/\${file}
            done

            # Configure for this AWS account
            unset AWS_PROFILE
            export AWS_SAML_ROLE=${roleArn}
            export AWSCLI_VERSION=${config.cliVersion}
            export AWSCLI_SAML_VERSION=${config.samlVersion}

            python3 authenticate_py3.py -u ${AWS_USER} -p ${AWS_PASSWORD} --profile ${config.profileName}
        """
    }

    return config.profileName
}
