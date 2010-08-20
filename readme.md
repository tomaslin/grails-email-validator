# Grails Email Validator Plugin

The Email Validator plugin provides extensive checking of email addresses.

## What does it do?

It helps answer the following questions:

+ Does this email have valid syntax?
+ Does this domain exist?
+ Can I send an email to this domain?
+ Does the user exist in this mail domain?

## How do I use it?

In your code, call:

    EmailStatus status = EmailValidatorService.check( "panchito_veloz@mydomain.com" ) 

This will return an EmailStatus object. It has the following properties

    boolean syntaxValid   // the address entered has valid email syntax 
    boolean domainValid   // the domain exists and mail servers can connect to it
    boolean verified      // the user exists in the remote mail server
	
Additionally, there is a subfield in EmailStatus called SmtpServer, which provides the following information:

    String domain					// email domain name
    String mxRecord					// name of preferred mx record
    boolean valid 					// has a MX record for the mail server
    boolean canConnect 				// can connect to this SMTP server
    boolean acceptsVerifyRequests 	// can accept VRFY calls
	
## Configuration

These options are configurable via your application's Config.groovy file:

<table>
<thead><th>Property</th><th>Default Value</th><th>What does it do?</th></thead>
<tbody>
<tr><td>emailvalidator.checkDomains</td><td>true</td><td>when enabled, will check a domain name for validity and try to see if a mail connection can be made.</td></tr>
<tr><td>emailvalidator.checkVRFY</td><td>false</td><td>when enabled, will issue a VRFY command to see if the user exists in the remote mail server. Little utility in reality since most servers would have this disabled</td></tr>
<tr><td>emailvalidator.useCache</td><td>true</td><td>uses a database cache to store smtp requests. This means that common domains ( like gmail ) are only checked once until your database melts down.</td></tr>
</tbody></table>
	
## Implementation details

+ Email syntax validation is done via the Apache Commons EmailValidator.  

+ Domain validity is calculated by getting the first MX Record for that domain and trying to connect to it.

+ The validity of an email is checked by issuing a VRFY command to the server. In most cases, this would be turned off.

+ For usage examples, please refer to the EmailValidatorServiceSpec[1] integration test.

## Gotchas / Tips

+ If you don't want to do domain checking, it is preferrable to use the Apache Commons EmailValidator rather than this whole plugin.

+ If you disable domain checking, the smtpServer property will be null.

+ Most mail servers will have the VRFY field disabled to prevent spam. This means that in most cases, you won't know if an email address is 100% valid unless you send an email to it. The verified field of the email status is going to most often be false. 

[1]:http://github.com/tomaslin/grails-email-validator/blob/master/test/integration/grails/plugins/emailvalidator/EmailValidatorServiceSpec.groovy