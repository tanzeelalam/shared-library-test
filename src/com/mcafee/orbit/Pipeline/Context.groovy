package com.mcafee.orbit.Pipeline

import com.cloudbees.groovy.cps.SerializableScript

/**
 * Abstraction layer over the pipeline script.
 */
final class Context {
    /**
     * The wrapped script
     */
    SerializableScript script
    Context(SerializableScript script) {
        this.script = script
        this.Artifactory = script.Artifactory
    }

    /*
    ==================================
      Jenkins Plugins
    ==================================
     */
    /**
     * Artifactory plugin
     */
    def Artifactory

    /*
    ==================================
      Standard Jenkins Pipeline API
    ==================================
     */
    /**
     * Error step
     *
     * @param arg Error message
     */
    void error(String arg) { script.error(arg) }
    /**
     * Stash step
     *
     * @param arg Stash step arguments
     */
    void stash(def arg) { script.stash(arg) }
    /**
     * Unstash step
     *
     * @param name The name of the stash to unstash
     */
    void unstash(String name) { script.unstash(name) }
    /**
     * Dir step
     *
     * @param path The path to the directory to enter
     * @param closure The closure to execute within the directory
     */
    void dir(String path, Closure closure) { script.dir(path, closure) }
    /**
     * isUnix step
     *
     * @return true for unix, false for windows
     */
    boolean isUnix() { script.isUnix() }
    /**
     * Println step
     *
     * @param arg The message to print
     */
    void println(def arg) { script.println(arg) }
    /**
     * WriteFile step
     *
     * @param args WriteFile step arguments
     */
    void writeFile(Map args) { script.writeFile(args) }
    /**
     * LibraryResource step
     *
     * @param name The resource to fetch
     * @return The contents of the resource
     */
    String libraryResource(String name) { script.libraryResource(name) }
    /**
     * WithCredentials step
     *
     * @param creds The credentials to load
     * @param closure The closure to execute with the loaded credentials
     */
    void withCredentials(List creds, Closure closure) { script.withCredentials(creds, closure) }
    /**
     * WithEnv step
     *
     * @param env The environment variable to load
     * @param closure The closure to execute with the loaded variables
     */
    void withEnv(List env, Closure closure) { script.withEnv(env, closure) }
    /**
     * Sh step
     *
     * @param command The shell command to execute
     */
    void sh(String command) { script.sh(command) }
    /**
     * Bat step
     *
     * @param command The batch command to execute
     */
    void bat(String command) { script.bat(command) }
    /**
     * Loads a username/password combination from the credentials store.
     * Used in combination with WithCredentials step.
     *
     * @param arg credentialsId/usernameVariable/passwordVariable
     * @return The credential to be loaded into WithCredentials step
     */
    def usernamePassword(Map arg) { script.usernamePassword(arg) }

    /*
    ==================================
      Custom overloads
    ==================================
     */
    void execute(String command) {
        if (isUnix()) {
            sh(command)
        } else {
            bat(command)
        }
    }

    /*
    ==================================
      Calls to legacy code in /vars/*
    ==================================
     */
    /**
     * Returns the artifacts step from /vars
     * @return The artifacts step
     */
    def getArtifacts() { script.artifacts }
    /**
     * Returns the bom step from /vars
     * @return The bom step
     */
    def getBom() { script.bom }
    /**
     * Returns the ecm step from /vars
     * @return The ecm step
     */
    def getEcm() { script.ecm }
    /**
     * Returns the git step from /vars
     * @return The git step
     */
    def getGit() { script.git }
    /**
     * Returns the radar step from /vars
     * @return The radar step
     */
    def getRadar() { script.radar }
    /**
     * Returns the utils step from /vars
     * @return The utils step
     */
    def getUtils() { script.utils }
}