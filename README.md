# logview
New generation sense log viewer

## Overview
The logview application is a log viewer divided into left and right panes.
The left pane displays the file browser and the right pane displays the contents of the log file.

## Features
- High speed performance.  
  Works fast, because it doesn't read the whole file.
- Normal search function  
  Search one line at a time.
- Smart search function  
  Even if a single event in a log file spans multiple lines, it is possible to view the entire log event in a single log file.
- Automatic update of log display  
  Even if it has been appended to the server-side log, it will continue to display the appended content. It is so-called `tail -f`.
- Permalink per line  
  This is useful for sharing logs because you can select a line or range of lines to create a permalink like Github.
- Compatible with various character codes.  
  The character code of the file is recognized automatically. This means that the file doesn't have to be UTF-8.

## Screenshot

![screenshot](https://raw.githubusercontent.com/piclane/logview/master/doc/screenshot.png)

### Header Pane
Icon and a list of breadcrumbs according to the path of the file browser.
This area can also be customized by editing the context.xml.

### File Browser Pane
The directory specified in context.xml will be used as the root directory and its contents will be displayed.
Any directory can be added to the root directory separately.

You can see the following items in the file browser table.
- File name
- Last modified
- File size
- Owner
- Group
- Permissions

### File Viewer Pane
Displays the log file selected by the file browser.

## How to use

### About context.xml

Normally, copy `web/META-INF/context.default.xml` to `web/META-INF/context.xml` and use it.

You can use environment variables of context.xml in the form of `$VAL` `${VAL}` `${VAL:-foo}` `${VAL:+bar}` for the value of a variable in context.xml, like Bash.
Also, you can refer to the contents of external files with `@/path/to/file`.

#### Variables in context.xml
- app/logview/rootDir  
  Specify the root directory.
- app/logview/dirs/&lt;any dir name&gt;  
  Specifies an additional directory to be placed directly under the root directory.
  This can be described in more than one way.
- app/logview/header  
  You can write HTML and place additional icons, etc. in the header section. It is recommended to read external files in the format `@/path/to/html`.
  ElementUI can be used for html and can be described as follows.
  ```html
  <el-button
    icon="el-icon-s-home"
    type="mini"
    @click="location.href = 'http://foobar.com/'"></el-button>
  ```

### Build

Copy `web/META-INF/context.default.xml` to `web/META-INF/context.xml` and build it with your settings.
The build requires JDK8, nodejs, and npm.

```bash
$ cd /path/to/logview
$ ./gradlew war
$ mv build/libs/*.war /path/to/tomcat-base/webapps/
```

### Using docker

For now, you'll need to build your own docker image, but that's easy because you don't have to create a non-docker environment!

```bash
$ cd /path/to/logview
$ docker build -t logview .
$ docker run -d -v /path/to/local/logview/root/:/var/lib/logview/ --rm --name logview -p 8080:8080 logview:latest
```
