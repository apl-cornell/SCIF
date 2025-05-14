+++
title = "Implementing Multiple Contracts and Error Messaging For a Compiler for Smart Contracts"
[extra]
bio = """
  Noah Schiff (insert bio).<br>
  Kabir Samsi is a third-year undergrad, interseted in programming languages, compilers and DSLs.<br>
  Stephanie Ma (insert bio).<br>
"""
latex=true
[[extra.authors]]
name = "Noah Schiff"
[[extra.authors]]
name = "Kabir Samsi"
[[extra.authors]]
name = "Stephanie Ma"
+++

## Background

Our project focuses on improving and adding new features to the compiler for [Scif](https://arxiv.org/pdf/2407.01204) – a language for representing smart contracts with secure control flow. SCIF as a programmming language uses information flow and its type system to help to prevent control-flow attacks and improve improve secure smart contracts.

As both a language design and implementation paper, the [SCIF technical report](https://arxiv.org/abs/2407.01204) extensively discusses the SCIF compiler and the correctness and performance of the Solidity code it generates for SCIF programs. As such, its authors hope to eventually publish the compiler as a research artifact similar to what is required for a [PLDI Research Artifact](https://pldi25.sigplan.org/track/pldi-2025-pldi-research-artifacts) or [OOPSLA Artifact](https://2025.splashcon.org/track/splash-2025-oopsla-artifacts). The [ACM discusses different badges](https://www.acm.org/publications/policies/artifact-review-and-badging-current) an artifact can be awarded. The goal for SCIF is that the compiler can be easily setup and run by any open source contributor and that the compiler can easily validate results described in papers.

In our project, we focused on a few primary aspects – adding on the much-desired feature of defining **multiple contracts in one file**, and allowing this to work with our the compiler's current control flow and functionality; improving the quality of error messaging for malformed files; and improving the structure of the compiler's build system.

The existing compiler is frustrating to setup and finicky to run. Furthermore, for potential contributors, it's challenging to know if they introduce a regression or improvement to the codebase. We aim to improve the experience for users and contributors.

## Implementation and Features

### Multiple Contracts

A significant push involved extending our compiler to be able to properly handle multiple contracts.

### Build System for Research Artifact

SCIF uses [SHErrLoc](https://www.cs.cornell.edu/projects/SHErrLoc/). It is written in Java but uses a different build system and uses different versions of the same dependencies that SCIF uses. Previously, SHErrLoc was duplicated in the repo and not properly linked as a submodule, causing conflicts with compiled bytecode class versions. The build system also did not properly include SHErrLoc, and conflicting versions of CUP could cause sporadic compilation and runtime issues. We work to fix this.

Previously, SCIF had no public reference manual for users. We introduce CI to build and publish a public language reference manual. Additionally, SCIF had no sanity checkers for contributors. We introduce GitHub actions to verify compilation and running of the compiler.

Finally, SCIF is currently only runnable through Gradle. This requires users to checkout the repository (and submodule), install Java and Gradle, and understand how to set up the repository. This seriously hinders the usability of SCIF as a language, as most users of a compiler simply want to run it. We begin work to untangle hardcoded paths to the local filesystem and package the compiler as a reusable, compiled JAR for distribution. 
<!-- We additionally are working to improve the build and run time of the compiler. We've began to see small improvements from our better integration of SHErrLoc, andNumerous builtin contracts are recompiled on every execution, and SHErrLoc ShAdditionally, builtin contract files are recompiled every time the compiler runs, which is un -->
The work here is still ongoing, but this will be a top priority and integral to larger SCIF project's continued success.

## Performance, Results and Testing

## Challenges

## Conclusions