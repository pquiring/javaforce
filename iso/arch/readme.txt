Before you start you need to add the custom key into pacman:

cd ~/.gnupg
gpg --output public.key --armor --export "Your name"

pacman-key --add public.key
pacman-key --lsign-key "Your name"

Then you can run jfLinux.sh
