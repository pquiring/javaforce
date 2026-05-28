These docker files are used to generate docker containers for different Linux distros with build tools installed to build and package JavaForce.

To build arm64 images on amd64 system add "--platform linux/arm64" after the build command.

Then use repo/docker-arm64-qemu.sh to run arm64 image on amd64 system.
