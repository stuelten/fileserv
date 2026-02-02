# FileServ WebDAV Server

FileServ is a simple, lightweight WebDAV server written in Java.

It allows you to serve a local directory
using the WebDAV protocol, supporting both HTTP and HTTPS.

## Getting Started

### Single File Distribution (Recommended)

You can build a single executable file that includes all dependencies:

```bash
./mvnw clean package -DskipTests
```

This will produce an executable at `fileserv-app/target/fileserv`. You can run it directly:

```bash
./fileserv-app/target/fileserv --help
```

### Using the Demo Script

Alternatively, you can use the provided demo script:

```bash
./start-demo.sh
```

This script will:

1. Build the project using Maven.
2. Generate a self-signed SSL certificate (`keystore.p12`)
   if it doesn't exist.
3. Create a default `passwords.txt` file if it doesn't exist.
4. Start the server serving the `./data` directory.

Once started,
you can access the server at
`https://localhost:8443` or `http://localhost:8080`.

## Features

- **WebDAV Support**: Full support for WebDAV operations (PROPFIND, MKCOL, PUT, GET, DELETE, MOVE, COPY, LOCK, UNLOCK).
- **HTTP & HTTPS**: Can serve content over both secure and insecure connections.
- **Authentication**: Supports Basic Authentication
- **Proxy Support**: Can be configured to trust `X-Forwarded-*` headers when running behind a reverse proxy.
- **Zero Configuration**: Sensible defaults allow you to start serving files immediately -- at least for demo purposes :)

## File Storage

By default, FileServ serves the `./data` directory.
You can specify any other directory
on the command line.

```bash
java -jar fileserv-app.jar /path/to/your/data
```

## Authentication

FileServ supports several ways to configure authentication:

### Password File

Use the `--passwd` option to specify a file containing credentials.
The file should contain
one user per line in the format `username:password`.

At the moment, no secure password storage is implemented.

```bash
java -jar fileserv-app.jar --passwd=mypasswords.txt
```

### CLI Options

You can also specify users directly on the command line
using `-u` (or `--user`) and `-p` (or `--password`):

```bash
java -jar fileserv-app.jar -u alice -p secret -u bob -p password
```

### Authentication Plugins

You can use authentication plugins via the `--auth` option.
The format is `type:key=value,key2=value2`.

Currently supported types:

- **file**: Reads users from a file.
  - `path`: Path to the password file.
- **ldap**: Authenticates against an LDAP server.
  - `url`: LDAP server URL (e.g., `ldap://localhost:389`).
  - `dnPattern`: Pattern for user DN (e.g., `uid=%s,ou=users,dc=example,dc=com`).
- **dummy**: A dummy authenticator for testing.
  - `accept`: If set to `true` (default), all requests are accepted. If `false`, all requests are revoked.

Example:
```bash
java -jar fileserv-app.jar --auth ldap:url=ldap://localhost:389,dnPattern=uid=%s,ou=users,dc=example,dc=com
```

Multiple `--auth` options can be provided.

### Default Credentials

If no authentication is configured, FileServ defaults to:

- **Username**: `demo`
- **Password**: (A random 6-digit number, displayed in the console on startup)

## CLI Documentation

FileServ provides the following command-line options:

| Option                | Description                                  | Default               |
|-----------------------|----------------------------------------------|-----------------------|
| `[root]`              | Positional argument: Data directory to serve | `./data`              |
| `--config`            | Path to a configuration properties file      | `fileserv.properties` |
| `--http-port`         | HTTP port (set to 0 to disable)              | `8080`                |
| `--https-port`        | HTTPS port                                   | `8443`                |
| `-u`, `--user`        | User name for authentication                 |                       |
| `-p`, `--password`    | Password for authentication                  |                       |
| `--passwd`            | Path to a passwords file                     |                       |
| `--auth`              | Authenticator plugin configuration           |                       |
| `--keystore`          | Path to the keystore file (SSL)              | `keystore.p12`        |
| `--keystore-password` | Keystore password                            | `changeit`            |
| `--key-pass`          | Key password                                 | (same as keystore)    |
| `--behind-proxy`      | Trust `X-Forwarded-*` headers                | `true`                |
| `--help`              | Show help message and exit                   |                       |
| `--version`           | Print version information and exit           |                       |

### Configuration File

Instead of passing all options via CLI,
you can use a properties file (default `fileserv.properties`).
Options in the file should match the long option names
(e.g., `http-port=9090`).

You can also use:

- **System Properties**: Prefixed with `fileserv.` (e.g., `-Dfileserv.http-port=9090`)
- **Environment Variables**: Prefixed with `FILESERV_` (e.g., `FILESERV_HTTP_PORT=9090`)

## Development & Testing Tools

### Build and Test

You can run all tests and build the project and Docker image
using the provided `bin/build.sh` script:

```bash
./bin/build.sh [clean]
```

### Test Data Generator

The project includes a test utility to generate random directory hierarchies for testing purposes.
You can build it either as überjar/fat-jar or as a Java-based native executable.

#### Java Version (Native Executable)

The Java version is implemented using `picocli`
and can be compiled into a standalone native executable using GraalVM.
This provides instant startup and eliminates the need for a JRE.

**Location**: `fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy` (after build)

**Build**:
To build the native executable, ensure you have a GraalVM-compatible JDK (version 21+) and run:
```bash
JAVA_HOME=/path/to/graalvm ./mvnw clean package -pl fileserv-test-generate-hierarchy -DskipTests
```

**Usage**:
```bash
./fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy [OPTIONS] <target_dir>
```

#### Options & Example

**Options**:
- `-s`, `--size <size>`: Total size of all files combined (e.g., `20mb`, `500kb`). Default: `2mb`.
- `-c`, `--count <items>`: Total number of files and directories to create. Default: `100`.
- `-r`, `--ratio-dir-to-files <R>`: Ratio of files to directories (e.g., `12` means ~1 dir per 12 files). Default: `10`.
- `-d`, `--depth <max_depth>`: Maximum depth of the directory tree. Default: `4`.

**Example**:
```bash
# Generate 1000 files with a combined size of 20MB (roughly).
# For each 12 files, one dir is created.
# The maximal depth of the hierarchy is 6.
# All files and dirs are created inside the directory test-data.
./fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy --size 20mb --count 1000 --ratio-dir-to-files 12 --depth 6 test-data
```

This creates a directory `test-data`
containing approximately 1000 items (files and directories)
with a total size of 20MB,
spread across a hierarchy up to 6 levels deep.

## Installation via Homebrew

You can install the test utilities via Homebrew using our custom Tap.

### 1. Setup the Tap

The Homebrew formula is maintained
in a separate repository [homebrew-tap](https://github.com/stuelten/homebrew-tap).
To install it, you first need to add the Tap to your local Homebrew:

```bash
brew tap stuelten/tap
```

### 2. Install the Test Apps

Once the Tap is added, you can install the test applications:

```bash
brew install fileserv-test-apps
```

This will download the pre-built native executables and install them into your system path.

### 3. Usage

After installation, you can run the tools directly from any directory:

```bash
fileserv-test-generate-hierarchy --help
```

### 4. Repository Structure

Following Homebrew's best practices, we use two separate repositories:
- **[fileserv](https://github.com/stuelten/fileserv)**: The main project containing the source code and build logic.
- **[homebrew-tap](https://github.com/stuelten/homebrew-tap)**: A dedicated repository for Homebrew formulae.

## Community

- **[Contributing](CONTRIBUTING.md)**: Guidelines for contributing to FileServ.
- **[Code of Conduct](CODE_OF_CONDUCT.md)**: Our expectations for community behavior.
- **[Security Policy](SECURITY.md)**: How to report security vulnerabilities.
- **[License](LICENSE)**: FileServ is licensed under the Apache License 2.0.
