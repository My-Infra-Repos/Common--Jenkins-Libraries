/**
 * Loads any git submodules.
 *
 * No params.
 *
 * @return Boolean true.
 */

import com.myorg.common.utils.Util

def call(Map<String, Object> params) {
    defaults = [:]
    def config = defaults + params

    sh loadMixins() + "git submodule update --init"
    return true
}
