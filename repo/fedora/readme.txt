This is the JavaForce RPM Repository.

You will need to install dpkg-dev package:
  sudo yum install createrepo rpm-sign

Run gpg to create the key to sign the packages:
  gpg --gen-key
Backup the ~/.gnupg/*.gpg files (pubring.gpg and secring.gpg)

run update.sh to create the repo files
run rsync.sh to upload the repo files

To use the repo copy javaforce.repo to /etc/yum.repos.d
and copy javaforce.gpg to /etc/?/?
This is performed by the iso creation scripts.
