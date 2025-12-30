package com.mcafee.orbit.Utils

import spock.lang.Specification

class PathUtilsTest extends Specification {
    def "can expand file lists"(String input, List expected) {
        when:
        List actual = PathUtils.expandFileList(input)

        then:
        actual == expected

        where:
        input | expected
        '**' | ['.']
        '*,**,*,**' | ['.']
        'foo/bar/baz/*.txt' | ['foo/bar/baz']
        'foo,bar/baz/*.txt' | ['foo','bar/baz']
        'foo/*/bar/*/baz' | ['foo']
        'foo/bar' | ['foo/bar']
        'foo/bar/baz/**/something/else' | ['foo/bar/baz']
        'foo\\bar\\baz\\*.pdf,foo/bar/baz/**/.*,foo/bar/baz' | ['foo/bar/baz']
    }
    def "throws on file lists"(String input) {
        when:
        PathUtils.expandFileList(input)

        then:
        thrown(RuntimeException)

        where:
        input | _
        null | _
        '' | _
        '  ' | _
    }
}
