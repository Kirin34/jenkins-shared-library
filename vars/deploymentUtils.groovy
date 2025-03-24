def updateImageTag(Map config) {
    gitUtils.checkoutRepo([
        repoUrl: config.manifestRepo,
        branch: config.manifestBranch ?: 'main',
        gitCredentials: config.gitCredentials
    ])
    
    sh """
        sed -i '/newName: .*${config.imageName}/{n;s/newTag: .*/newTag: ${config.version}/}' ${config.kustomizationPath}/kustomization.yaml
    """
    
    def hasChanges = sh(
        script: "git diff --quiet ${config.kustomizationPath}/kustomization.yaml || echo 'changed'",
        returnStdout: true
    ).trim()
    
     if (hasChanges == 'changed') {
        gitUtils.configureGit([
            email: 'automation.cicd@kineton.it',
            name: 'Jenkins'
        ])
        
        // Usa il nuovo metodo add
        gitUtils.add([
            files: ["${config.kustomizationPath}/kustomization.yaml"]
        ])
        
        gitUtils.commit([
            message: "chore: update ${config.imageName} image to ${config.version}"
        ])
        
        gitUtils.push([
            branch: config.manifestBranch,
            gitCredentials: config.gitCredentials
        ])
    } else {
        echo "Nessuna modifica necessaria al file kustomization.yaml"
    }
}