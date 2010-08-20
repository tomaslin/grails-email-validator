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

import org.apache.commons.net.smtp.SMTPReply
import org.apache.commons.net.smtp.SMTPClient
import org.xbill.DNS.Lookup
import org.xbill.DNS.Type

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.apache.commons.validator.EmailValidator

class EmailValidatorService {

  static transactional = false

  def apacheValidator = EmailValidator.getInstance()
  def checkDomains = ConfigurationHolder.config.emailvalidator.checkDomains == null ? true : ConfigurationHolder.config.emailvalidator.checkDomains
  def useCache = ConfigurationHolder.config.emailvalidator.useCache == null ? true : ConfigurationHolder.config.emailvalidator.useCache 
  def checkVRFY = ConfigurationHolder.config.emailvalidator.checkVRFY == null ? false : ConfigurationHolder.config.emailvalidator.checkVRFY

  EmailStatus check(String emailAddress) {

    EmailStatus status = new EmailStatus()

    if (apacheValidator.isValid(emailAddress)) {
      status.syntaxValid = true

      if (checkDomains) {

        def (user, domain) = emailAddress.split('@')

        // check if the smtpServer has already been used before
        SmtpServer smtpCachedValue = SmtpServer.findByDomain(domain)
        SmtpServer smtpCache

        if (!smtpCachedValue) {
          smtpCache = new SmtpServer(domain: domain)
          status.smtpServer = smtpCache

          // lookup the mx record associated with the domain
          def mxRecords = new Lookup(domain, Type.MX).run()
          if (mxRecords) {
            // get highest priority record
            mxRecords = mxRecords.sort {  it.priority as int }
            status.smtpServer.valid = true
            status.smtpServer.mxRecord = mxRecords[0].target as String
            // connect to server and check value
            status = validateMxRecord(status, user, checkVRFY)
          }
        } else {
          status.smtpServer = smtpCachedValue
          if (checkVRFY && status.smtpServer.acceptsVerifyRequests) {
            status = validateMxRecord(status, user, checkVRFY)
          }
        }
      }
    }
    return status
  }

  // attempts to connect to the server and issue a smtp request
  private def validateMxRecord = { status, user, checkVRFY ->
    try {
      SMTPClient client = new SMTPClient()
      client.connect(status.smtpServer.mxRecord, 25)
      // can connect
      if (SMTPReply.isPositiveCompletion(client.replyCode)) {
        status.smtpServer.canConnect = true
        if (checkVRFY) {
          // calls a VRFY request to the server. This is most likely turned off due to spam control.
          status.verified = client.verify(user)
          if (client.replyCode == 250 || client.replyCode == 550) {
            status.smtpServer.acceptsVerifyRequests = true
          } else {
            status.smtpServer.acceptsVerifyRequests = false
          }
        }
      }
      client.disconnect()
    } catch (Exception e) {
      // do nothing
    }
    if (useCache && (status.smtpServer.isDirty() || (status.smtpServer.id == null))) {
      status.smtpServer.save()
    }
    return status
  }
}