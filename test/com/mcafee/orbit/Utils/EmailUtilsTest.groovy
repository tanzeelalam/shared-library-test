package com.mcafee.orbit.Utils

import spock.lang.Specification

class EmailUtilsTest extends Specification {
    def "can reformat muiltiple emails"() {
        when:
        def result = EmailUtils.reformatEmails(input)

        then:
        result == expected

        where:
        input                                           | expected
        "_12345@McAfee.com,first_Last@mcafee.com"       | "_12345@trellix.com, first.last@trellix.com"
        "first_Last@mcafee.com,_12345@McAfee.com"       | "first.last@trellix.com, _12345@trellix.com"
        "first_Last@mcafee.com, other.name@mcafee.com"  | "first.last@trellix.com, other.name@trellix.com"
        "some_Name@mcafee.com,first.last@trellix.com"   | "some.name@trellix.com, first.last@trellix.com"
    }

    def "can reformat one email"() {
        when:
        def result = EmailUtils.reformatSingleEmail(input)

        then:
        result == expected

        where:
        input                       | expected
        "_123@mcafee.com"           | "_123@trellix.com"
        "first_last@McAfee.com"     | "first.last@trellix.com"
        "FIRST_LAST@MCAFEE.com"     | "first.last@trellix.com"
        "first.last@trellix.com"    | "first.last@trellix.com"
    }

    def "can reformat using multiple delimiters"() {
        when:
        def result = EmailUtils.reformatEmails(input)

        then:
        result == expected

        where:
        input                                                                                           | expected
        "FIRST_last@MCAFEE.com, another.name@trellix.com"                                               | "first.last@trellix.com, another.name@trellix.com"
        "first_last@mcafee.com; first_last@mcafee.com first_last@mcafee.com first_last@mcafee.com"      | "first.last@trellix.com, first.last@trellix.com, first.last@trellix.com, first.last@trellix.com"
        "first_last@mcafee.com _1234@mcafee.com ;; ,, first_last@mcafee.com"                            | "first.last@trellix.com, _1234@trellix.com, first.last@trellix.com"
        "first_last@mcafee.com first_last@mcafee.com first.last@trellix.com"                            | "first.last@trellix.com, first.last@trellix.com, first.last@trellix.com"
    }

    def "no output on empty string"() {
        when:
        def result = EmailUtils.reformatEmails(input)

        then: 
        result == expected

        where:
        input | expected
        null | null
        ""    | ""
    }

    def "dont modify input if not mcafee email"() {
        when:
        def result = EmailUtils.reformatEmails(input)

        then: 
        result == expected

        where:
        input                        | expected
        "test_another@domain.com"    | "test_another@domain.com"
    }
}