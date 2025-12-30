package com.mcafee.orbit.Utils

import spock.lang.Specification

class StringUtilsTest extends Specification {
    def "can parse integers"(def input, def defaultValue, def expected) {
        when:
        def actual = StringUtils.asInteger(input, defaultValue)

        then:
        actual == expected

        where:
        input | defaultValue | expected
        '0' | 0 | 0
        '123' | 0 | 123
        '-50' | 0 | -50
        '' | 0 | 0
        '1.2' | 1 | 1
        'abc' | 2 | 2
    }

    def "can parse nullable integers"(def input, def expected) {
        when:
        def actual = StringUtils.nullableInteger(input)

        then:
        actual == expected

        where:
        input | expected
        '0' | 0
        '123' | 123
        '-50' | -50
        '' | null
        '1.2' | null
        'abc' | null
    }

    def "can split command by flag"(def limit, def input, def expected) {
        when:
        def actual = StringUtils.splitCommandByFlag(
            ['test', 'command'], '-f', input, limit
        )

        then:
        actual == expected

        where:
        limit | input | expected
        20 | 'a' | [['test', 'command', '-f', 'a']]
        30 | ['a', 'b'] | [['test', 'command', '-f', 'a'], ['test', 'command', '-f', 'b']]
        40 | ['a', 'b', 'c'] | [['test', 'command', '-f', 'a', '-f', 'b'], ['test', 'command', '-f', 'c']]
        60 | ['a', 'b', 'c', 'd'] | [['test', 'command', '-f', 'a', '-f', 'b', '-f', 'c', '-f', 'd']]
    }
}
