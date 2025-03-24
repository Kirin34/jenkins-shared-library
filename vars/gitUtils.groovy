def checkoutRepo(Map config) {
    def gitTool = tool name: 'slave-git', type: 'git'
    withEnv(["PATH+GIT=${gitTool}/bin"]) {
        checkout([$class: 'GitSCM',
            branches: [[name: config.branch ?: '*/main']],
            userRemoteConfigs: [[
                url: config.repoUrl,
                credentialsId: config.gitCredentials
            ]]
        ])
    }
}

def configureGit(Map config) {
    sh """
        git config user.email "${config.email ?: 'automation.cicd@kineton.it'}"
        git config user.name "${config.name ?: 'Jenkins'}"
    """
}

def add(Map config) {
    // Validazione input
    if (!config.files) {
        error("Nessun file specificato per l'aggiunta al commit")
    }
    def filesString = config.files.join(' ')
    sh """
        git add ${filesString}
    """
}


def commit(Map config) {
    sh """
        git commit -m "${config.message}"
    """
}

def push(Map config) {
    
    withCredentials([usernamePassword(credentialsId: config.gitCredentials, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
        // Estrai l'URL del repository
        def repoUrl = sh(script: 'git config --get remote.origin.url', returnStdout: true).trim()
        
        // Costruisci l'URL con username e password incorporati
        def urlWithAuth = repoUrl.replaceFirst('https://', "https://${GIT_USERNAME}:${GIT_PASSWORD}@")
        
        sh """
            git push ${urlWithAuth} HEAD:${config.branch}
        """
    }
}

def commitAndPush(Map config) {
    commit([
        files: config.files,
        message: config.message,
        email: config.email,
        name: config.name
    ])
    
    push([
        branch: config.branch,
        gitCredentials: config.gitCredentials
    ])
}

def getLastCommitMessage() {
    return sh(
        script: 'git log -1 --pretty=%B',
        returnStdout: true
    ).trim()
}