#!/bin/bash
wget https://java.net/projects/visualvm/downloads/download/release138/visualvm_138.zip
sudo unzip visualvm_138.zip -d /usr/lib
sudo mv /usr/lib/visualvm_138 /usr/lib/visualvm
sudo cp *.desktop /usr/share/applications
sudo cp *.png /usr/share/icons/hicolor/48x48/apps
sudo cp visualvm /usr/bin
sudo chmod +x /usr/bin/visualvm
sudo chmod +x /usr/lib/visualvm/bin/visualvm
jdeb visualvm-1.3.8_all.deb
mv *.deb ../ubuntu
rm visualvm_138.zip
