def getDockerRegistry(Map config = [:]) {
    def environment = config.environment ?: 'dev'
    def registryId = "legaserieb-${environment}-ecr-repository"
    
    withCredentials([string(credentialsId: registryId, variable: 'REGISTRY')]) {
        return REGISTRY
    }
}
