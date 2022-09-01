#!/usr/bin/env groovy
package com.myorg.common.utils

class Constants {
    static final String ARTIFACTORY_DOCKER_HUB = 'docker.repo1.your_org.com'
    static final String LDAP_SERVER = 'ad-ldap-prod.your_org.com'

    // AWS
    static final String ECR_ACCOUNT_ID = 'TODO_REPLACEME'
    static final String ECR_REGION = 'us-east-1'

    // ServiceNow
    static final String SERVICE_NOW_API_URL = 'https://myorgworker.service-now.com'
    static final String SERVICE_NOW_CHANGE_API_URL = '/api/now/table/change_request?'
    static final String SERVICE_NOW_INCIDENT_API_URL = '/api/now/table/incident?'
    static final String SERVICENOW_CREDENTIAL_ID = 'TODO_REPLACEME'
    static final String INCIDENT_PREFIX = 'INC'
    static final String CHANGE_PREFIX = 'CHG'

    // Git & GitHub
    static final String MAINLINE_BRANCH = 'master'
    static final String RELEASE_BRANCH = 'deploy'
    static final String GIT_USER = 'TODO_REPLACEME'
    static final String GIT_EMAIL = 'jenkins-ci.myorg.com'
    static final String GITHUB_API_URL = 'https://github.com/api/v3'
    static final String GITHUB_USER_CREDENTIAL = 'TODO_REPLACE_ME'
    static final String GITHUB_USER_AND_TOKEN_CREDENTIAL = 'TODO_REPLACEME'

    // Semver Bump Levels
    static final String PATCH_BUMP = 'patch'
    static final String MINOR_BUMP = 'minor'
    static final String MAJOR_BUMP = 'major'

    // Credential Types
    static final String SECRET_TEXT_CREDENTIAL_TYPE = 'SECRET_TEXT'
    static final String USER_PASSWORD_CREDENTIAL_TYPE = 'USERNAME_PASSWORD'
    static final String SECRET_FILE_CREDENTIAL_TYPE = 'SECRET_FILE'
    static final String UNKNOWN_CREDENTIAL_TYPE = 'UNKNOWN'

}
