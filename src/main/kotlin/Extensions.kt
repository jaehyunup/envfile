package io.github.jaehyunup.envfile

fun Map<String, String>.onlyMissingEnv() = filterKeys { System.getenv(it) == null }