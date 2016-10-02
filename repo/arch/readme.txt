This is the JavaForce Arch Repository.

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

There are two repos for each arch (x32 and x64).
Run the following scripts in each one:

update.sh to build repo files.

rsync.sh to upload to sourceforge.
