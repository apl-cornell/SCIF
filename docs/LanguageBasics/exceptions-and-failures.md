# Exceptions and Failures

Like any other program, smart contracts can run into errors. The EVM provides a coarse-grained transaction mechanism that rollbacks all side-effects of a transaction when this occurs. SCIF aids programmers in dealing with such scenarios more gracefully by providing exception and failure-handling mechanisms.

Intuitively, the distinction between exceptions and failures is that of recoverable, benign errors and severe ones that force part of the transaction to be rolled back. For example, a hotel-booking contract can throw an exception when a booking request is rejected while keeping the state change that the request has been processed; the request initiator contract may resume with a different request. For another example, an exchange contract may need to handle out-of-gas failure when calling an unknown contract by marking the particular transaction as problematic and skipping it in the future; in this case, the effects of the call shall be rolled back.

Any code in SCIF finishes execution with one of those three cases: normal termination, a user-defined exception, or a failure.

The exception-handling mechanism in SCIF uses the classic try-catch syntax. The try block is always executed. If it terminates normally, all catch blocks are ignored. If it throws an exception that matches one of the catch conditions, the first matching catch block is executed right after the exception is thrown, and no rollback occurs. Otherwise, i.e., in case of a failure or an uncaught exception, the current external call frame terminates with a failure and is rolled back.

The failure-handling mechanism in SCIF uses the atomic-rescue(TBD) syntax. The atomic block is executed with an external call to ensure failures can be caught. If it terminates normally, catch blocks are ignored. If it ends with a failure, the rescue block is executed, and the effects of the atomic block are guaranteed to be rolled back. In case the atomic block throws an exception, the current external call frame terminates with a failure and is rolled back. To handle exceptions, the programmer can nest a try-catch block inside an atomic-rescue block.
