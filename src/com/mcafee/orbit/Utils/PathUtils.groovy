package com.mcafee.orbit.Utils

/**
 * Utility methods for manipulating paths
 */
class PathUtils {
    /**
     * Expands a comma-separated list of globs to a sensible
     * list of target locations.
     *
     * Someone thought that it would be a good idea to have
     * a comma-separated list of file paths despite the fact
     * that a comma is a perfectly valid character to have
     * as part of a path :face-palm:
     *
     * Used by the AV scanner.
     *
     * Simply calling glob() on each entry won't work as this may generate
     * a very large list of files that, when passed to the AV scanner
     * will fail as the shell command will exceed shell line length limits.
     *
     * @param locations [T:String] A comma-separated list of globs
     * @return A sensible list of target locations to scan.
     */
    static List<String> expandFileList(String locations) {
        if (StringUtils.isNullOrEmpty(locations)) {
            throw new RuntimeException(
                'Failed to parse the file list.'
            )
        }
        if (locations == '*' || locations == '**') {
            // Will recursively scan current folder
            return ['.']
        }
        return locations.split(',').collect {
            it.trim()
        }.collect {
            if (!it.contains('*')) {
                return it
            } else {
                return truncateGlob(it)
            }
        }.unique()
    }

    /**
     * Truncates a glob path string to the nearest parent
     * that does not contain a '*'.
     *
     * @param glob [T:String] the glob path string to truncate
     * @return A truncated path
     */
    private static String truncateGlob(String glob) {
        List<String> parts = glob.replace('\\', '/').split('/')
        if (parts.size() == 1) {
            // the glob is targeting something in the current folder,
            // so target the entire current folder for the scan.
            return '.'
        }
        int globIndex = parts.findIndexOf { it.contains('*') }
        return parts[0..globIndex-1].join('/')
    }
}