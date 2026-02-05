package io.github.jaehyunup.envfile.extensions

fun Map<String, String>.onlyMissingOnSystemEnv() = filterKeys { System.getenv(it) == null }