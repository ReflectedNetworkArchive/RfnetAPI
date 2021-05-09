job("Build and run tests") {
    container(displayName = "Run mvn package", image = "maven:latest") {
        shellScript {
            content = """
	            mvn clean pacakge
            """
        }
    }
}