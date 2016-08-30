var electron = require('electron'),
    fs = require('fs-extra'),
    path = require('path'),
    shell = require('shelljs'),
    packageJson = require(__dirname + '/package.json');

var ipc = electron.ipcMain;
var app = electron.app;
var dialog = electron.dialog;
var BrowserWindow = electron.BrowserWindow;
var Menu = electron.Menu;

// Report crashes to atom-shell.
// require('crash-reporter').start();

const devConfigFile = __dirname + '/config.json';
var devConfig = {};
if (fs.existsSync(devConfigFile)) {
  devConfig = require(devConfigFile);
}


const isDev = (packageJson.version.indexOf("DEV") !== -1);
const onMac = (process.platform === 'darwin');
const acceleratorKey = onMac ? "Command" : "Control";
const isInternal = (devConfig.hasOwnProperty('internal') && devConfig['internal'] === true);



// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the javascript object is GCed.
var mainWindow = null;

// make sure app.getDataPath() exists
// https://github.com/oakmac/cuttle/issues/92
fs.ensureDirSync(app.getPath('userData'));


//------------------------------------------------------------------------------
// Main
//------------------------------------------------------------------------------

const versionString = "Version   " + packageJson.version + "\nDate       " + packageJson["build-date"] + "\nCommit  " + packageJson["build-commit"];


function showVersion() {
  dialog.showMessageBox({type: "info", title: "Version", buttons: ["OK"], message: versionString});
}

var fileMenu = {
  label: 'File',
  submenu: [
  {
    label: 'Quit',
    accelerator: acceleratorKey + '+Q',
    click: function ()
    {
      app.quit();
    }
  }]
};

var helpMenu = {
  label: 'Help',
  submenu: [
  {
    label: 'Version',
    click: showVersion
  }]
};

var debugMenu = {
  label: 'Debug',
  submenu: [
  {
    label: 'Toggle DevTools',
    click: function ()
    {
      mainWindow.toggleDevTools();
    }
  }
  ]
};

var menuTemplate = [fileMenu, debugMenu, helpMenu];


// NOTE: not all of the browserWindow options listed on the docs page work
// on all operating systems
const browserWindowOptions = {
  height: 850,
  title: 'bru-9',
  width: 1400,
  icon: __dirname + '/img/logo_96x96.png'
};


//------------------------------------------------------------------------------
// Register IPC Calls from the Renderers
//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
// Ready
//------------------------------------------------------------------------------


// This method will be called when atom-shell has done everything
// initialization and ready for creating browser windows.
app.on('ready', function() {
  // Create the browser window.
  mainWindow = new BrowserWindow(browserWindowOptions);

  // and load the index.html of the app.
  mainWindow.loadURL('file://' + __dirname + '/index.html');

  var menu = Menu.buildFromTemplate(menuTemplate);

  Menu.setApplicationMenu(menu);

  // Emitted when the window is closed.
  mainWindow.on('closed', function() {
    // Dereference the window object, usually you would store windows
    // in an array if your app supports multi windows, this is the time
    // when you should delete the corresponding element.
    mainWindow = null;
    app.quit();
  });

  if (devConfig.hasOwnProperty('dev-tools') && devConfig['dev-tools'] === true) {
    mainWindow.openDevTools();
  }

});
