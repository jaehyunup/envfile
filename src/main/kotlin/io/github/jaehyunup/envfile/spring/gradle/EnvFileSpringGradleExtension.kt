package io.github.jaehyunup.envfile.spring.gradle

import io.github.jaehyunup.envfile.spring.enums.EnvFileStyle
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

abstract class EnvFileSpringGradleExtension @Inject constructor(objects: ObjectFactory) {
    val priority: Property<EnvFileStyle> =
        objects.property(EnvFileStyle::class.java).convention(EnvFileStyle.DOTENV)
}