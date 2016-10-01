#!/bin/bash
USERNAME=digiboy86,javaforce
SITE=web.sf.net
FOLDER=/home/project-web/javaforce/htdocs/arch
rsync -v *.xz *.gz $USERNAME@$SITE:$FOLDER
