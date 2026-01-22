# Ideas

## Storage

Build a real storage system and not only a filesystem-based showcase.

### Storage into different backends

1. Store files simply in a given data directory. No rocket science.
2. Store files in multiple locations with bigger redundancy.
3. Store files remotely in some storage.
4. Store files in-memory.

### Combine multiple backends for redundancy and performance

1. Use the in-memory backend for caching
   and the file system backend for persistence.
2. Use storage classes to manage speed and redundancy. 

### Storage with fine-grained access control

Use sidecar files or metadate table to store permissions.
Use hierarchy to set permissions for lower levels.
Use file tags, user/group roles and some mapping to manage permissions.

### Storage with automatic processing of files

Create previews for images.
Create thumbnails for videos.
Do some OCR for scans.
Create resumes for documents via some AI tool.
Extract metadata from documents.

## Authentication

### OAuth Authenticator

Authenticate against some OAuth provider.

### OAuth Backend

Implement an simple OAuth backend.

## Tests

### A performance tester

Add module fileserv-test-performance.
It should create a product like fileserv-test-generate-hierarchy,
a native binary when built via native profile.

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
