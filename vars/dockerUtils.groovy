def buildImage(Map config) {
    def DOCKER_REGISTRY = secretUtils.getDockerRegistry(environment: env.BRANCH)
    sh "docker build -t ${DOCKER_REGISTRY}/${env.IMAGE_NAME}:${config.tag} -f ${env.DOCKERFILE} ${env.BUILD_CONTEXT}"
}

def pushImage(Map config) {
    def DOCKER_REGISTRY = secretUtils.getDockerRegistry(environment: env.BRANCH)
    sh """
        aws ecr get-login-password --region ${config.awsRegion} | \
        docker login --username AWS --password-stdin ${DOCKER_REGISTRY}
        docker push ${DOCKER_REGISTRY}/${env.IMAGE_NAME}:${config.tag}
    """
}