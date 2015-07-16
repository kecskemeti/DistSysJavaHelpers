# DistSysJavaHelpers
Helper classes to support simulations of large scale distributed systems

The purpose of these helpers to provide a job level representation of distributed systems as well as to allow a simple way to load and manage several trace file formats.

Website:
https://github.com/kecskemeti/DistSysJavaHelpers

Licensing:
GNU General Public License 3 and later

## Compilation & Installation

Prerequisites: Apache Maven 3, Java 1.6

After cloning the prerequisites, run the following in the main dir of the checkout:

`mvn clean install javadoc:javadoc`

The installed helper classes will be located in the default maven repository's (e.g., `~/.m2/repository`) following directory: 
`hu/mta/sztaki/lpds/cloud/simulator/DistSysJavaHelpers/[VERSION]/DistSysJavaHelpers-[VERSION].jar`

Where `[VERSION]` stands for the currently installed version of the example set.

The documentation for the helper classes will be generated in the following subfolder of the main dir of the checkout:

`target/site/apidocs`

## Remarks

##### Warning: the master branch of the helpers is intended as a development branch, and might not contain a functional version!
