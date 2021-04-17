#!/bin/bash
USERNAME=digiboy86,javaforce
SITE=web.sf.net
FOLDER=/home/project-web/javaforce/htdocs/arch/x86_64
rsync -vl *.xz *.gz *.db *.files *.sig $USERNAME@$SITE:$FOLDER
