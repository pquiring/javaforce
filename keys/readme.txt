JavaForce Certificate Signing
=============================

JavaForce applets are now signed by a root certificate which is located in this folder.
To generate the root certificate:
  cd /keys
  ant genca
The root cert is good for 10 years.
Make sure you backup the files in this folder.

To sign an applet follow these steps:
Goto the projects folder and run:
  cd /project/...
  ant genkey
This generates the projects key and csr which is copied to this folder, then run in this folder:
  cd /keys
  ant -Dname=... signkey
to sign the csr which generates the cert for the project.  Where ... is the projects name.
Go back to the project folder and run:
  cd /project/...
  ant importcert
Then build the applet using:
  cd /project/...
  ant sign-jar

Java7 has new security policies:
  All JARs must include a manifest with:
    Trusted-Library:true
    permissions:all-permissions
    Codebase: <domain>
  Where domain is the domain where the applet is executed from, and the JARs must be signed as before.

To run the applet you must download and install the root certificate (javaforce.crt).
Goto your control panel, click on Java, click on Security Tab, click on Manage certificates, change certificate type to "Signer CA", click import and import the file above.
Now when you run an applet you have the option to always run the applet without a prompt.
This process is now automatic for Applets that call JF.loadCerts()
