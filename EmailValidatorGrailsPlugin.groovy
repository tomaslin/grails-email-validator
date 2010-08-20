class EmailValidatorGrailsPlugin {

    def version = "0.1"
    def grailsVersion = "1.1.1 > *"
    def dependsOn = [:]

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "DMC Digital"
    def authorEmail = "tomaslin@gmail.com"
    def title = "Email validator"
    def description = '''Checks the validity of email addresses entered.

Rather than just syntax, it can also be configured to 
    '''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/email-validator"

}
