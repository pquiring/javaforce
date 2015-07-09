jfWebProxy
----------

Desc : A FULL Proxy server (with https/SSL support) that proxies requests thru a webserver using JSP or PHP (formerly jGAEProxy)
The JSP version can be loaded on Google App Engine (GAE).
Works with Firefox and Chrome.
Great for getting around blocking firewalls or accessing websites not available in your country for FREE.

How it works:
The proxy is split up into two parts:
 - a website you load with JSP or PHP pages.  These pages will act as a proxy service.
 - a local Java service on your PC will run the real proxy service which uses the special website.

Installing JSP pages on GAE
===========================

To install you'll need "Java SDK" (version 7 or lower) and "Google AppEngine Java SDK" in your path.

Edit jsp\WEB-INF\appengine-web.xml and change "your_id" to your app ID you obtained at http://appengine.google.com

Then run:
  cd jsp
  appcfg update .

Install JSP on Tomcat
=====================
Run 'ant war' and upload the generated war file.

Install PHP on Apache
=====================
Copy the php files to /var/www/html

Running Local Server
====================

There are 3 ways to run the proxy server:

1) Use the MSI package (Windows only)

   - install the MSI package
   - run GAEProxy from the start menu
   - right-click on the "GP" in the system tray and click on show
   - set "Proxy Host" to your_id.appspot.com (or PHP domain) and click on "save and restart"

2) Run the app directly

  run.bat
   - right-click on the "GP" in the system tray and click on show
   - set "Proxy Host" to your_id.appspot.com (or PHP domain) and click on "save and restart"

3) Run the command line version app (good for debugging)

  runcli.bat your_id.appspot.com jsp/php [-secure]
   - all log is printed to stdout

Then set your browser to use localhost:8080 to use the proxy for all protocols.

The secure option forces the proxy server to access the website redirect page securely for all requests (more private).
  Normally only https requests use a secure connection to the proxy website.

HTTPS Support
=============

To CONNECT to secure websites (https) a special process is used.
The proxy server will create a fake certificate for EACH secure site you connect to which are all
signed by a root certificate created when this project is compiled.
All you need to do is import this generated root cert into your system.
While the proxy service is running on your local system open:
  ~/.jfwebproxy/localhost.crt
    or
  %userprofile%\.jfwebproxy\localhost.crt
and import it into your browser.
Chrome:
  - Just double-click the file and click on "Install..."
  - Make sure to import it into the "Trusted Root Cert Authorities" and not the default store.
Firefox:
  - Install it in advanced options -> Certificates -> Authorities tab -> Import and select the websites option.
Now secure sites will work, even though they are actually connecting to a local secure server (sneaky eh?)

PAC
===

If you want to use the proxy only for certain websites create a Proxy AutoConfig (PAC) like this:

  function FindProxyForURL(url, host) {
    if (shExpMatch(host, "*.pandora.com"))
    {
      return "PROXY 127.0.0.1:8080";
    }

    // All other requests go directly
    return "DIRECT";
  }

Save it to a file, and set your browsers PAC URL to this file.
  file:///c:/path/proxy.pac

Author : Peter Quiring (pquiring at gmail dot com)

Enjoy!
