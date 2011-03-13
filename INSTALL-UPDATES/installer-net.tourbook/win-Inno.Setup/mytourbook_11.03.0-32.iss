[Setup]
AppName=MyTourbook (32bit)
AppVerName=MyTourbook 11.3.0 (32bit)
AppCopyright=MyTourbook Contributors 2005, 2011
AppID={{37852811-BC7D-411C-8122-E69CCA892582}

LicenseFile=M:\ws_mt-with-GIT\mytourbook\net.tourbook\gpl.txt

OutputDir=M:\mytourbook-PRODUCT\11.3.0
OutputBaseFilename=mytourbook_11.3.0.win32.win32.x86.setup

DefaultGroupName=MyTourbook
DefaultDirName={pf}\MyTourbook

VersionInfoVersion=11.3.0
VersionInfoCompany=MyTourbook

WizardImageBackColor=clWhite
WizardSmallImageFile=M:\ws_mt-with-GIT\mytourbook\net.tourbook\icons\application\tourbook48-32-win-installer.bmp

LanguageDetectionMethod=locale
UninstallDisplayIcon=M:\ws_mt-with-GIT\mytourbook\net.tourbook\icons\application\tourbook16.ico

[Files]
Source: M:\MyTourbook-Product\11.3.0\mytourbook_11.3.0.win32.win32.x86\mytourbook\*; DestDir: {app}; Flags: recursesubdirs; Tasks: ; Languages:

[InstallDelete]
Name: {app}\*; Type: files
Name: {app}\configuration; Type: filesandordirs
Name: {app}\features; Type: filesandordirs
Name: {app}\plugins; Type: filesandordirs
Name: {app}\workspace; Type: filesandordirs
Name: {app}; Type: dirifempty

[Icons]
Name: {group}\MyTourbook; Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe
Name: {group}\Uninstall MyTourbook; Filename: {app}\{uninstallexe}; IconFilename: {uninstallexe}
Name: {commondesktop}\MyTourbook; Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe; WorkingDir: {app}; Tasks: desktopicon\common
Name: {userdesktop}\MyTourbook; Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe; WorkingDir: {app}; Tasks: desktopicon\user
Name: {userappdata}\Microsoft\Internet Explorer\Quick Launch\MyTourbook; Filename: {app}\MyTourbook.exe; WorkingDir: {app}; Tasks: " quicklaunchicon"

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:GroupCreateIcons}
Name: desktopicon\common; Description: {cm:AllUsers}; GroupDescription: {cm:GroupCreateIcons}; Flags: exclusive
Name: desktopicon\user; Description: {cm:CurrentUser}; GroupDescription: {cm:GroupCreateIcons}; Flags: exclusive unchecked
Name: quicklaunchicon; Description: {cm:CreateQuickLaunchIcon}


[Run]
Filename: {app}\mytourbook.exe; WorkingDir: {app}; Description: {cm:StartMyTourbook}; Flags: postinstall nowait

[Languages]
Name: de; MessagesFile: compiler:Languages\German.isl; LicenseFile: M:\ws_mt-with-GIT\mytourbook\net.tourbook\gpl-de.rtf
Name: en; MessagesFile: compiler:Default.isl

[CustomMessages]
en.GroupCreateIcons=Additional icons:
en.CreateDesktopIcon=Create a &desktop icon
en.AllUsers=For &all users
en.CurrentUser=For the &current user only

de.GroupCreateIcons=Icons erstellen:
de.CreateDesktopIcon=Symbol auf dem &Desktop erstellen
de.AllUsers=F�r &alle Benutzer
de.CurrentUser=Nur f�r den aktuellen &Benutzer

en.CreateQuickLaunchIcon=Create a &quick launch icon
de.CreateQuickLaunchIcon=&Schnellstart Symbol erstellen

en.StartMyTourbook=Run &MyTourbook
de.StartMyTourbook=&MyTourbook starten


