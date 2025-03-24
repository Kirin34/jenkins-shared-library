def getVersionFromPackage() {
    def packageJson = readJSON file: "${env.BUILD_CONTEXT}/package.json"
    return packageJson.version
}

def getLatestVersionFromECR(Map config) {
    def defaultVersion = "1.0.0"
    def registryId = ""
    def awsRegion = config.awsRegion ?: 'eu-west-1'
    def DOCKER_REGISTRY = secretUtils.getDockerRegistry(environment: env.BRANCH)
    try {
        // Estrai l'ID del registry in modo più sicuro
        registryId = DOCKER_REGISTRY.split('\\.')[0]
        echo "Registry ID: ${registryId}"
        
        // Esegui il login separatamente
        sh """
            aws ecr get-login-password --region ${awsRegion} | \\
            docker login --username AWS --password-stdin ${DOCKER_REGISTRY}
        """
         sh "aws --version"
         sh "aws sts get-caller-identity"
        // Verifica che il repository esista
       def repoExists = sh(
        script: """
            aws ecr describe-repositories \\
                --region ${awsRegion} \\
                --registry-id ${registryId} \\
                --repository-names ${env.IMAGE_NAME} > /dev/null 2>&1
            """,
        returnStatus: true
        ) == 0  // Restituisce true se il comando ha successo, false altrimenti
        
        // Verifica se ci sono immagini nel repository
        def imagesCount = sh(
            script: """
                aws ecr list-images \\
                    --region ${awsRegion} \\
                    --registry-id ${registryId} \\
                    --repository-name ${env.IMAGE_NAME} \\
                    --query 'length(imageIds)' \\
                    --output text || echo "0"
            """,
            returnStdout: true
        ).trim()
        
        if (imagesCount == "0") {
            echo "Repository ${env.IMAGE_NAME} è vuoto. Utilizzo versione predefinita: ${defaultVersion}"
            return defaultVersion
        }
        
        // Ottieni il tag più recente
        def tag = sh(
            script: """
                aws ecr describe-images \\
                    --region ${awsRegion} \\
                    --registry-id ${registryId} \\
                    --repository-name ${env.IMAGE_NAME} \\
                    --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]' \\
                    --output text 2>/dev/null || echo ""
            """,
            returnStdout: true
        ).trim()
        
        echo "Tag ottenuto dall'ECR: '${tag}'"
        
        if (tag == "null" || tag == "None" || tag == "" || tag == "latest") {
            echo "Tag non valido. Utilizzo versione predefinita: ${defaultVersion}"
            return defaultVersion
        } else {
            echo "Utilizzo tag esistente: ${tag}"
            return tag
        }
    } catch (Exception e) {
        echo "Errore nel recupero tag da ECR: ${e.message}"
        echo "Stack trace: ${e.stackTrace.join('\n')}"
        echo "Utilizzo versione predefinita: ${defaultVersion}"
        return defaultVersion
    }
}

def bumpVersionNode(Map config) {
    def currentVersion = getVersionFromPackage()
    def (major, minor, patch) = currentVersion.tokenize('.')
    def newVersion = currentVersion
    
    if (config.commitMessage.contains('#major')) {
        newVersion = "${(major.toInteger() + 1)}.0.0"
    } else if (config.commitMessage.contains('#minor')) {
        newVersion = "${major}.${(minor.toInteger() + 1)}.0"
    } else if (config.commitMessage.contains('#patch')) {
        newVersion = "${major}.${minor}.${(patch.toInteger() + 1)}"
    }
    
    if (newVersion != currentVersion) {
        sh """
            cd ${env.BUILD_CONTEXT}
            npm version ${newVersion} --no-git-tag-version
        """
    }
    
    return newVersion
}


def bumpVersion(Map config) {
    def currentVersion = getLatestVersionFromECR([
        awsRegion: config.awsRegion ?: 'eu-west-1'
    ])
    echo "Versione recuperata da ECR (prima di toString): ${currentVersion}"
    def (major, minor, patch) = currentVersion.tokenize('.')
    echo "${major} ${minor} ${patch}"
    def newVersion = currentVersion
    if (config.commitMessage.contains('[major]')) {
        newVersion = "${(major.toInteger() + 1)}.0.0"
        echo " major ${newVersion}"
    } else if (config.commitMessage.contains('[minor]')) {
        newVersion = "${major}.${(minor.toInteger() + 1)}.0"
        echo "minor ${newVersion}"
    } else if (config.commitMessage.contains('[patch]')) {
        newVersion = "${major}.${minor}.${(patch.toInteger() + 1)}"
        echo " patch ${newVersion}"

    }
    echo "${newVersion}"
    return newVersion
}
