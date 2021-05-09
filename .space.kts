job("Build, run tests, publish") {
    startOn {
        gitPush{
            branchFilter = "refs/heads/main"
        }
    }

    container(displayName = "Run publish script", image = "maven:3-openjdk-8-slim") {
        env["REPOSITORY_URL"] = "https://maven.pkg.jetbrains.space/reflectednetwork/p/internalapi/maven"

        shellScript {
            content = """
                echo Build Spigot dependency...
                mkdir BuildTools && cd BuildTools
                wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastStableBuild/artifact/target/BuildTools.jar
                java -jar BuildTools.jar --rev 1.8.8
                cd ../
                echo Build and run tests...
                mvn clean install
                echo Publish artifacts...
                mvn deploy -s settings.xml \
                    -DrepositoryUrl=${'$'}REPOSITORY_URL \
                    -DspaceUsername=${'$'}JB_SPACE_CLIENT_ID \
                    -DspacePassword=${'$'}JB_SPACE_CLIENT_SECRET
            """
        }
    }
}