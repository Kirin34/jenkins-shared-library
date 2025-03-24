
def call(Map params) {
    // Mapping dei colori
    def COLOR_MAP = [
        'SUCCESS' : 'good',
        'FAILURE' : 'danger',
        'ABORTED' : '#ffa500',
        'STARTED' : '#ffdf20'
    ]
    slackSend botUser: true, 
            channel: 'demo-radioplayer', 
            color: COLOR_MAP[params.status], 
            message: "*${params.status}*: ${env.JOB_NAME}(${env.BUILD_NUMBER})\nhttps://jenkins-radioplayer.kineton.cloud/job/${env.JOB_NAME}/${env.BUILD_NUMBER}/", 
            tokenCredentialId: 'test-slack-token'
}

def notifyUpload(Map config) {
    def blocks = [
        [
            type: "section",
            text: [
                type: "mrkdwn",
                text: "${config.fileName} has been uploaded on <https://drive.google.com/drive/folders/1flWHCj3xaufihd6XfhFwjmM1HCrAL4Fv|Radioplayer Drive>"
            ]
        ]
    ]
               
    slackSend(
        botUser: true, 
        channel: 'demo-radioplayer',
        blocks: blocks,
        tokenCredentialId: 'test-slack-token'
    )
}