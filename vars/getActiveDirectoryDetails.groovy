import com.myorg.common.utils.Constants

/**
 * Lookup and retreive information on an Active Directory group.
 *
 * Required params:
 * @param name String The name of the LDAP resource. Either an MS ID or a group name.
 * @param type String The AD resource type. One of 'USER' or 'GROUP'. Default is 'USER'.
 *    - 'USER' and 'EMAIL' both return a User. The difference being what field is used to perform the lookup (username or email).
 * @param credentialsId String The name of Jenkins credential to use for ldap bind.
 *
 * Optional params:
 * @param email String The email address for a user. This is required if 'name' is blank and 'type' is USER.
 * @param ldapServer String The server to use for AD queries. Default is Constants.LDAP_SERVER.
 * @param debug Bool Set to true to show the commands being run for debugging.
 *
 * @return a Map of result details:
 * (user) [dn: '', username: '', accountType: '', groups: [...], lastName: '', firstName: '', email: '', location: '', country: '', state: '']
 * (group) [dn: '', group: '', members: [...]]
 *
 *
 * Example:
 * You can use this in a job to compare the user who triggered it against group membership:
 *
 *     buildInfo = getBuildEvent credentialsId: userPwdCredentialId
 *     jobUser = buildInfo.username
 *     groupName = 'some_ad_group'
 *     adGroup = getActiveDirectoryDetails credentialsId: someCredential, name: groupName, type: 'GROUP'
 *     groupMembers = adGroup.members
 *
 *     if (!groupMembers.contains(jobUser)) {
 *         // fail build
 *     }
 *
 */
def call(Map<String, Object> params) {
    defaults = [
        name: '',
        email: '',
        type: 'USER',
        ldapServer: Constants.LDAP_SERVER,
        credentialsId: null,
        debug: false
    ]
    def config = defaults + params
    def validTypes = ['USER', 'GROUP']
    def result

    if (!validTypes.contains(config.type)) {
        echoErr "type must be one of ${validTypes}!"
        return null
    }

    if (config.type == 'USER' && !(config.name || config.email)) {
        echoErr "one of name or email is required for USER!"
        return null
    }

    if (!config.credentialsId) {
        echoErr "credentialsId is required!"
        return null
    }

    if (config.debug) {
        echoWarn "[getActiveDirectoryDetails] using config: ${config}"
    }

    withCredentials([
        usernamePassword(credentialsId: config.credentialsId, usernameVariable: 'USER', passwordVariable: 'PASSWORD'),
    ]) {
        if (config.type == 'USER') {
            result = ldapUserQuery(config.ldapServer, config.name, config.email, config.debug)
        } else {
            result = ldapGroupQuery(config.ldapServer, config.name, config.debug)
        }
    }

    if (!result) {
        echoErr "No LDAP result found for ${config.type.toLowerCase()} '${config.name}'"
    }
    return result
}

def ldapUserQuery(server, username, email, debug = false) {
    def cmd = ldapBaseCmd(server)
    def opts
    def resp

    if (username) {
        opts = userRequestOpts(["sAMAccountName=${username}"])
    } else if (email) {
        opts = userRequestOpts(["mail=${email}"])
    }

    if (debug) {
        echoWarn "[getActiveDirectoryDetails] Running ldap query: '${cmd} ${opts}'"
    }

    try {
        resp = sh(script: loadMixins() + "set +x && ${cmd} ${opts}", returnStdout: true)
    } catch(err) {
        echoErr err
        return null
    }

    if (debug) {
        echoWarn "[getActiveDirectoryDetails] LDAP results: ${resp}"
    }

    return parseUserResponse(resp)
}

def ldapGroupQuery(server, group, debug = false) {
    def cmd = ldapBaseCmd(server)
    def opts = groupRequestOpts(group)
    def resp

    if (debug) {
        echoWarn "[getActiveDirectoryDetails] Running ldap query: '${cmd} ${opts}'"
    }

    try {
        resp = sh(script: loadMixins() + "set +x && ${cmd} ${opts}", returnStdout: true)
    } catch(err) {
        echoErr err
        return null
    }

    if (debug) {
        echoWarn "[getActiveDirectoryDetails] LDAP results: ${resp}"
    }

    return parseGroupResponse(resp)
}

def ldapBaseCmd(server) {
    return "ldapsearch -w $PASSWORD -h $server -s sub -D \"cn=$USER,cn=Users,dc=ms,dc=ds,dc=your_org,dc=com\""
}

def userRequestOpts(filters=[]) {
    def filterOpts = ""
    if (filters) {
        filterOpts = "(&"
        filters.each { f ->
            filterOpts += "(${f})"
        }
        filterOpts += ")"
    }

    return "-b \"cn=Users,dc=ms,dc=ds,dc=your_org,dc=com\" \"${filterOpts}\" sAMAccountName userAccountControl memberOf sn givenName mail l c st"
}


def groupRequestOpts(String group) {
    return "-b \"cn=Users,dc=ms,dc=ds,dc=your_org,dc=com\" \"(&(objectClass=group)(cn=$group))\" dn cn member"
}

def parseUserResponse(response) {
    def result = [groups:[]]

    response.split("\n").each { l ->
        if (l.startsWith("dn: ")) {
            result['dn'] = l.split(':')[1].trim()
        } else if (l.startsWith("sAMAccountName: ")) {
            result['username'] = l.split(':')[1].trim()
        } else if (l.startsWith("userAccountControl: ")) {
            def uacValue = l.split(':')[1].trim()
            def accountType = getUserAccountType(uacValue)
            result['accountType'] = accountType
        } else if (l.startsWith("memberOf: ")) {
            def groupCn = l.split(':')[1]
            // CN=AWS_xxxxxxxx_Read,CN=Users,DC=ms,DC=ds,DC=your_org,DC=com
            def group = groupCn.split(",")[0].split('=')[1]
            result['groups'] << group.trim()
        } else if (l.startsWith("sn: ")) {
            result['lastName'] = l.split(':')[1].trim()
        } else if (l.startsWith("givenName:: ")) {
            result['firstName'] = l.split(':')[1].trim()
        } else if (l.startsWith("mail: ")) {
            result['email'] = l.split(':')[1].trim()
        } else if (l.startsWith("l: ")) {
            result['location'] = l.split(':')[1].trim()
        } else if (l.startsWith("c: ")) {
            result['country'] = l.split(':')[1].trim()
        } else if (l.startsWith("st: ")) {
            result['state'] = l.split(':')[1].trim()
        }
    }

    // Verify that the response was succesful
    if (result.dn) {
        return result
    } else {
        return null
    }
}

def parseGroupResponse(response) {
    def result = [members:[]]

    response.split("\n").each { l ->
        if (l.startsWith("dn: ")) {
            result['dn'] = l.split(':')[1].trim()
        } else if (l.startsWith("cn: ")) {
            result['group'] = l.split(':')[1].trim()
        } else if (l.startsWith("member: ")) {
            def userCn = l.split(':')[1]
            // CN=somebody,CN=Users,DC=ms,DC=ds,DC=your_org,DC=com
            def username = userCn.split(",")[0].split('=')[1]
            result['members'] << username.trim()
        }
    }

    // Verify that the response was succesful
    if (result.dn) {
        return result
    } else {
        return null
    }
}

def getUserAccountType(userAccountControl) {
    if (userAccountControl == '512') {
        return 'USER_ACCOUNT'
    } else {
        return 'SERVICE_ACCOUNT'
    }
}
