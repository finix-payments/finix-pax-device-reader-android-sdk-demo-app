pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://finix.repo.sonatype.app/repository/finix-maven/")
            credentials {
                username = System.getenv("NEXUS_TOKEN_USER")
                password = System.getenv("NEXUS_TOKEN_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven {
            url = uri("https://finix.repo.sonatype.app/repository/finix-maven/")
            credentials {
                username = System.getenv("NEXUS_TOKEN_USER")
                password = System.getenv("NEXUS_TOKEN_PASSWORD")
            }
            authentication {
                create<org.gradle.authentication.http.BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "Pax Device Reader Sample Application"
include(":app")
