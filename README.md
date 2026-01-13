# FileServ WebDAV Server

FileServ is a simple, lightweight WebDAV server written in Java.

It allows you to serve a local directory
using the WebDAV protocol, supporting both HTTP and HTTPS.

## Getting Started

The easiest way to see FileServ in action
is to use the provided demo script:

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
