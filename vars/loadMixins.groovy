/**
 * Load the Jenkins mixins.
 *
 * The only available argument is a bool that determines whether or not to show
 * verbose output. This should generally be left to use the default 'false' except
 * for the first invocation in your script in case there are any warnings to
 * display.
 */
def call(verbose = false) {
    if (verbose) {
        return "set +x && . /etc/profile.d/jenkins.sh && set -x\n"
    } else {
        return ". /etc/profile.d/jenkins.sh > /dev/null 2>&1\n"
    }
}
