# Connection Library

A Metratec Java library for connection interfaces for other Java projects. This library contains TCP, UDP, RS232, USB and the Metratec MPS connection.

## Build the library

To create the library, please run the `mvn package` command. This command also creates the documentation.

## Install the library

* If you have a project without a project management system, copy the library to your project library folder.

* If you use Maven as your project management system, please install this library manually to your local maven repository with the following command: `mvn install-file -Dfile=metratec-connection-library-1.23.1.jar`. (If mvn does not find the pom file, please use a defined maven install plugin with this command: `mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=metratec-connection-library-1.23.1.jar`). Then you can add the library to the dependencies in your pom file:

```xml
<dependency>
    <groupId>com.metratec.lib.connection</groupId>
    <artifactId>metratec-connection-library</artifactId>
    <version>1.23.1</version>
</dependency>
```

## Documentation

The library classes and methods are documented via javadoc and is available as HTML files and as jar file.

## License

MIT License

Copyright (c) 2023 metratec GmbH

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
