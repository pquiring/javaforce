#!/bin/bash
USERNAME=digiboy86,javaforce
SITE=web.sf.net
FOLDER=/home/project-web/javaforce/htdocs/arch/i686
rsync -vl *.xz *.gz *.db *.files $USERNAME@$SITE:$FOLDER
