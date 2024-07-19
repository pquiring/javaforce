JavaForce now has a Windows winget repo for msi packages.

To install the repo (requires admin rights):
  winget source add JavaForce https://javaforce.sourceforge.net/windows/amd64 Microsoft.Rest

To install packages:
  winget install {package-name}

Notes:
  - only Windows 11 is supported
  - Windows Server is not supported yet (Microsoft is working on this)
