This is the JavaForce PAC Repository.

You need to run:
  gpg --gen-key
To build a key to sign packages.
During the command you may need to run:
  dd if=/dev/sda of=/dev/zero
To generate enough entropy
If you get error that pinentry is not found, you need to create a symlink to pinentry.curses:
  cd /usr/bin
  rm pinentry
  ln -s pinentry.curses pinentry

Then run update.sh to build repo files.

Then run rsync.sh to upload to sourceforge.
