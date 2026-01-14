# Ideas

## Storage

### Storage with fine-grained access control

### Storage with automatic processing of files

## Authentication

### OAuth Authenticator

### OAuth Backend

## Tests

### A performance tester

Add module fileserv-test-performance.
It should create a product like fileserv-test-generate-hierarchy, a native binary when built via native profile.

The module should test basic operations (like the CRUD tester below),
but with a focus on performance.

It starts slow and grows over time
- in changes per file
- in concurrent accesses
- in size of changes

It should report curves for 
- response time
- throughput
- concurrent accesses

It should help in identifying bottlenecks. When does performance degrade, because
- the server does not have enough memory
- the network's bandwidth is saturated
- the server's filesystem cannot deliver more I/Os
- the server's filesystem cannot deliver more throughput
- the server cannot handle more connections simultaneously
- the authentication slows the server

### CRUD test

Add module fileserv-test-crud.
It should create a product like fileserv-test-generate-hierarchy, a native binary when built via native profile.

The module should implement a webdav tester, which creates, reads, updates and deletes files.

Options to control it should be:
1. Number of files to test
2. Number of threads to use
3. Size of files
4. URL and authentication to use
5. Format of the reports to print out
