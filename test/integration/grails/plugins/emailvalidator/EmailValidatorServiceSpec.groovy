/*
 * Copyright 2010 DMC Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.emailvalidator

import grails.plugin.spock.*
import spock.lang.Unroll
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class EmailValidatorServiceSpec extends IntegrationSpec {

  def emailValidatorService

  def setup(){
      ConfigurationHolder.config.emailvalidator.checkVRFY = true
      ConfigurationHolder.config.emailvalidator.checkDomains = true
      ConfigurationHolder.config.emailvalidator.useCache = true
      emailValidatorService = new EmailValidatorService()
  }

  @Unroll( "validating #email" )
  def "check email syntax"() {
      when: "checking emails"
        def status = emailValidatorService.check( email )

      then:
        status.syntaxValid == valid

      where:
        email                               | valid
        'tomaslin+mememe@gmail.com'         | true
        'tomaslin@gmail.com'                | true
        'sj34£kls@4d@d-ddds0.com'           | false
        'tomaslin'                          | false
        'tomaslin@sjkfld@sjfdkl@djk.com'    | false
  }

  @Unroll( "check domains #email" )
  def "check domain validity"(){
    when: "check domain"
      def status = emailValidatorService.check( email )

    then:
      status.isDomainValid == valid

    where:
      email                           | valid   | note
      'tomaslin@alumni.uwaterloo.ca'  | true    | 'valid email'
      'david@davidbrown.name'         | false   | 'valid domain, no SMTP'
      'you@grailsmoms.com'            | false   | 'invalid domain'
  }

  def "check cache"(){
    setup:
      SmtpServer.list()*.delete()

    when: 'first load from remote'
      emailValidatorService.check( 'tomaslin@gmail.com' )

    then:
      SmtpServer.count() == 1

    then:
      emailValidatorService.check( 'nottomaslin@gmail.com')
      SmtpServer.count() == 1

    then:
      emailValidatorService.check( 'tomaslin@hotmail.com')
      SmtpServer.count() == 2

  }

  def "check ability to verify domains"(){
    setup:
      SmtpServer.list()*.delete()

    expect: "first time we get an invalid request, it should return that we can verify on this domain"
      emailValidatorService.check( 'blahdaaaa@dealchecker.co.uk' ).smtpServer.acceptsVerifyRequests == true

    and: "but then we hit a 252"
      emailValidatorService.check( 'tomas.lin@dealchecker.co.uk' ).smtpServer.acceptsVerifyRequests == false

    and: "cannot be confident of results, so no longer accept verify requests"
      emailValidatorService.check( 'blahdaaaa@dealchecker.co.uk' ).smtpServer.acceptsVerifyRequests == false
  }

  @Unroll( "check params test #test" )
  def "check configuration options"(){

    when:
      SmtpServer.list()*.delete()
      ConfigurationHolder.config.emailvalidator.checkVRFY = vrfy
      ConfigurationHolder.config.emailvalidator.checkDomains = domains
      ConfigurationHolder.config.emailvalidator.useCache = useCache
      def validatorService = new EmailValidatorService()

      validatorService.check( 'paquito@hotmail.com' )
      validatorService.check( 'paquito@dealchecker.co.uk' )
      def status = validatorService.check( 'raulito@dealchecker.co.uk' )

    then:
      SmtpServer.count() == cacheSize
      smtpServerIsNull == ( status.smtpServer == null )

    and:
      if( !smtpServerIsNull ){
        status.smtpServer.canConnect = domainValid
        status.smtpServer.acceptsVerifyRequests == verifyVal
      }
    
    where:
      test  | vrfy  | domains   | useCache  | cacheSize | verifyVal | domainValid | smtpServerIsNull
      1     | false | false     | false     | 0         | false     | false       | true
      2     | false | true      | false     | 0         | false     | true        | false
      3     | false | true      | true      | 2         | false     | true        | false
      4     | true  | true      | true      | 2         | true      | true        | false
  }
  
}