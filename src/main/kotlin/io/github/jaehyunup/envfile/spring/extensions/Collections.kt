package io.github.jaehyunup.envfile.extensions

fun Map<String, String>.onlyMissingEnv() = filterKeys { System.getenv(it) == null }