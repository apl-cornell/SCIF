# Exceptions and Failures

Exception mechanisms are used for handling errors and unusual
conditions, or as alternate ways to return. For smart contracts however,
the EVM only provides a coarse-grained transaction mechanism that rolls
back all side effects of the current function call (except logging
instructions). SCIF makes the implementation of such logics easier and
less error-prone by providing exception and failure-handling mechanisms.

SCIF distinguishes between exceptions and failures. Intuitively,
exceptions are user-defined alternate return paths: the program keeps
executing normally, and there is no rollback. In contrast, failures
signal unrecoverable errors, such as out of gas or stack overflow, and
the function call that causes the failure is guaranteed to have been
rolled back.

For example, a hotel-booking contract can throw an exception when a
booking request is rejected while keeping the state change that the
request has been processed; the request initiator contract may resume
with a different request. For another example, an exchange contract may
need to handle out-of-gas failure when calling an unknown contract by
marking the particular transaction as problematic and skipping it in the
future; in this case, the effects of the call shall be rolled back.

Any code in SCIF finishes execution with one of those three cases:
normal termination, a user-defined exception, or a failure.

The exception-handling mechanism in SCIF uses the classic try-catch
syntax. The try block is always executed. If it terminates normally, all
catch blocks are ignored. If it throws an exception that matches one of
the catch conditions, the first matching catch block is executed right
after the exception is thrown, and no rollback occurs. Otherwise, i.e.,
in case of a failure or an uncaught exception, the current external call
frame terminates with a failure and is rolled back.

The failure-handling mechanism in SCIF uses the atomic-rescue(TBD)
syntax. The atomic block is executed with an external call to ensure
failures can be caught. If it terminates normally, catch blocks are
ignored. If it ends with a failure, the rescue block is executed, and
the effects of the atomic block are guaranteed to be rolled back. In
case the atomic block throws an exception, the current external
call frame terminates with a failure and is rolled back. To handle
exceptions, the programmer can nest a try-catch block inside an
atomic-rescue block.
