class AndroidBuilder implements Serializable {
    private def script
    private def config
    private def defaultConfig = [
        platform: 'Google',
        gradleProject: ':app',
        buildTypes: ['bundle', 'apk'],
        debugBuild: false,
        enableTests: false,
        gradleOpts: '',
        timeoutMinutes: 30,
        retryCount: 3,
        nodeVersion: 'Node-18.7',
        cleanWorkspace: true
    ]
    
    // Stati del build
    private def buildResults = [
        bundle: false,
        apk: false
    ]

    // Constructor
    AndroidBuilder(script, Map config = [:]) {
        this.script = script
        this.config = defaultConfig + config
    }

    // Metodo principale per eseguire il build
    def execute() {
        try {
            setupEnvironment()
            installDependencies()
            if (config.buildTypes.contains('bundle')) {
                buildBundle()
            }
            if (config.buildTypes.contains('apk')) {
                buildApk()
            }
            return buildResults
        } catch (Exception e) {
            script.error("Build failed: ${e.message}")
            return false
        }
    }

    // Setup dell'ambiente
    private def setupEnvironment() {
        script.stage('Setup Environment') {
            if (config.cleanWorkspace) {
                script.cleanWs()
            }
            
            // Setup delle variabili d'ambiente
            script.env.ANDROID_SDK_ROOT = "${script.env.HOME}/Library/Android/sdk"
            script.env.NPM_CONFIG_CACHE = "${script.WORKSPACE}/.npm"
            
            // Log della configurazione
            script.echo "Building for platform: ${config.platform}"
            script.echo "Using gradle project: ${config.gradleProject}"
        }
    }

    // Installazione delle dipendenze
    private def installDependencies() {
        script.stage('Install Dependencies') {
            script.nodejs(config.nodeVersion) {
                script.timeout(time: config.timeoutMinutes, unit: 'MINUTES') {
                    script.retry(config.retryCount) {
                        script.sh """
                            yarn
                            yarn prepare:build ${config.platform.toLowerCase()}
                        """
                    }
                }
            }
        }
    }

    // Build del bundle
    private def buildBundle() {
        script.stage('Build Bundle') {
            try {
                script.nodejs(config.nodeVersion) {
                    script.timeout(time: config.timeoutMinutes, unit: 'MINUTES') {
                        script.retry(config.retryCount) {
                            def buildType = config.debugBuild ? 'Debug' : 'Release'
                            def gradleCmd = """
                                cd android/
                                ./gradlew ${config.gradleProject}:bundle${buildType} \
                                    ${config.gradleOpts} \
                                    -PversionCode=${script.env.BUILD_NUMBER} \
                                    --parallel \
                                    --stacktrace
                            """
                            
                            script.sh gradleCmd
                            buildResults.bundle = true
                            script.echo "Bundle build successful"
                        }
                    }
                }
            } catch (Exception e) {
                script.echo "Bundle build failed: ${e.message}"
                buildResults.bundle = false
                if (!config.continueOnError) {
                    throw e
                }
            }
        }
    }

    // Build dell'APK
    private def buildApk() {
        script.stage('Build APK') {
            try {
                script.nodejs(config.nodeVersion) {
                    script.timeout(time: config.timeoutMinutes, unit: 'MINUTES') {
                        script.retry(config.retryCount) {
                            def buildType = config.debugBuild ? 'Debug' : 'Release'
                            def gradleCmd = """
                                cd android/
                                ./gradlew ${config.gradleProject}:assemble${buildType} \
                                    ${config.gradleOpts} \
                                    -PversionCode=${script.env.BUILD_NUMBER} \
                                    --parallel \
                                    --stacktrace
                            """
                            
                            script.sh gradleCmd
                            buildResults.apk = true
                            script.echo "APK build successful"
                        }
                    }
                }
            } catch (Exception e) {
                script.echo "APK build failed: ${e.message}"
                buildResults.apk = false
                if (!config.continueOnError) {
                    throw e
                }
            }
        }
    }


    // Metodi di utility per ottenere i path dei file generati
    def getBundlePath() {
        return "android/${config.gradleProject.replace(':', '')}/build/outputs/bundle/release/app-release.aab"
    }

    def getApkPath() {
        return "android/${config.gradleProject.replace(':', '')}/build/outputs/apk/release/app-release.apk"
    }
}

// Metodo globale per utilizzare il builder nelle pipeline
def call(Map config = [:]) {
    def builder = new AndroidBuilder(this, config)
    return builder.execute()
}