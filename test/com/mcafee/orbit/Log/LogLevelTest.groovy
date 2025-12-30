package com.mcafee.orbit.Log

import spock.lang.Specification

class LogLevelTest extends Specification {
    def "can create valid log levels"(int level, String asStr, int asInt) {
        when:
        LogLevel logLevel = new LogLevel(level)

        then:
        logLevel.toString() == asStr
        logLevel.asInt() == asInt

        where:
        level | asStr | asInt
        0 | 'ERROR' | 0
        1 | 'ERROR' | 1
        2 | 'WARN' | 2
        3 | 'INFO' | 3
        4 | 'DEBUG' | 4
    }
    def "throws on invalid log levels"(int level) {
        when:
        new LogLevel(level)

        then:
        thrown(IllegalArgumentException)

        where:
        level | _
        -1 | _
        5 | _
        100 | _
    }
}