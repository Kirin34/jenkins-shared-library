// vars/androidSigner.groovy
def call(Map config = [:]) {
    def keystorePath = "${env.HOME}/Documents/*/keys-credentials/RadioplayerKeyStore.jks"
    
    if (config.type == 'bundle') {
        sh """
            zip -d ${config.inputFile} META-INF/\\*
            jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
                -keystore ${keystorePath} \
                -signedjar ${config.outputFile} \
                ${config.inputFile} radioplayer \
                -storepass Radioplayer01!
            keytool -printcert -jarfile ${config.outputFile}
        """
    } else if (config.type == 'apk') {
        sh """
            ${env.HOME}/Library/Android/sdk/build-tools/34.0.0/zipalign -p -f -v 4 ${config.inputFile} app-release.apk
            ${env.HOME}/library/Android/sdk/build-tools/30.0.2/apksigner sign --ks ${keystorePath} \
                --ks-key-alias radioplayer \
                --ks-pass pass:Radioplayer01! \
                --key-pass pass:Radioplayer01! \
                ${config.outputFile}
            ${env.HOME}/library/Android/sdk/build-tools/30.0.2/apksigner verify --print-certs app-release.apk
        """
    }
}