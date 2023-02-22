# Getting Started

<!-- 
    Here writes a short tutorial on how to download, install SCIF compiler and try compiling a short smart contract. 
-->

A SCIF contract can be compiled with either a local or online (TODO) SCIF compiler. 

## Prerequisite

To build a SCIF compiler, the following packages are required:

* JFlex
* Java 17 or later
* Ant
* Gradle

## Installing the SCIF compiler

SCIF project is hosted at [github](https://github.com/Neroysq/SCIF) and can be built with the help of gradle. The following instructions clone the SCIF project and build it using gradle.

```shell
git clone --recurse-submodules  https://github.com/Neroysq/Wyvern.git
./gradlew build
```

## Compiling a short SCIF compiler

The following instruction compiles a short SCIF example:

```shell
./scif -c test/contracts/basic/MethodDefinition.scif
```
