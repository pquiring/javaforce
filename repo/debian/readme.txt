This is the JavaForce DEB Repository.

You will need to install dpkg-dev package:
  sudo apt-get install dpkg-dev

Run gpg to create the key to sign the packages (Note:gnupg 2.1+ generates kbx files unless pubring.gpg already exists)
  mkdir ~/.gnupg
  chmod 700 ~/.gnupg
  touch ~/.gnupg/pubring.gpg
  gpg --gen-key
Backup the ~/.gnupg/*.gpg files (pubring.gpg and secring.gpg)

Then run update.sh to create the repo files and then upload everything to website that is specifed in javaforce.list

To use the repo copy javaforce.list to /etc/apt/sources.list.d
and copy javaforce.gpg to /etc/apt/trusted.gpg.d
This is performed by the iso creation scripts.
See install.sh
