SMTPRelay Server
================

Periodically retrieves messages from POP3 server and re-sends to another SMTP server.

Typical usage:
  Install SMTP/POP3 service in insecure network
   - SMTP configured in digest mode (not internet accessable)
   - POP3 configured with an admin account (internet accessable)
  Install SMTPRelay service in secure network and relay messages from POP3 server in insecure network to corporate SMTP service in secure network.
   - recommend using POP3 on port 443 with src.ip configured to SMTPRelay IP address to avoid brute force attacks to POP3

WebSite: http://jfsmtprelay.sourceforge.net

Source : http://javaforce.sourceforge.net
