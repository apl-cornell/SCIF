# Getting Started

A SCIF contract can be compiled with the SCIF compiler. The latest version of the compiler is available as a prebuilt JAR at https://github.com/apl-cornell/SCIF/releases/download/latest/SCIF.jar.

## Build the Compiler

To build the SCIF compiler, the following packages are required:

* JFlex
* Java 21
* Ant
* Gradle

SCIF project is hosted at [github](https://github.com/apl-cornell/SCIF).

```shell
git clone --recurse-submodules https://github.com/apl-cornell/SCIF.git
```

## Example Usage

The following instruction compiles a short SCIF program using the prebuilt JAR.

```shell
java -ea -jar SCIF.jar -c test/contracts/multiContract/Dexible.scif
```

You can also run it with Gradle.
```shell
./gradlew run --args "test/contracts/multiContract/Dexible.scif"
```