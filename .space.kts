job("Build, run tests, publish") {
    startOn {
        gitPush{
            branchFilter = "refs/heads/main"
        }
    }

    container(displayName = "Run publish script", image = "reflectednetwork.registry.jetbrains.space/p/internalapi/containers/spigot:0.0.14", ) {
        env["REPOSITORY_URL"] = "https://maven.pkg.jetbrains.space/reflectednetwork/p/internalapi/maven"

        shellScript {
            content = """
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
