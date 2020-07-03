import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.embeddedKotlinVersion

object Versions {

  const val camundaSpringBoot = "3.4.4"

  const val camel = "3.4.0"
  const val camundaCamel = "0.8.0"

  object Build {
    val kotlin = embeddedKotlinVersion
    val java = JavaVersion.VERSION_1_8.toString()
  }
}
