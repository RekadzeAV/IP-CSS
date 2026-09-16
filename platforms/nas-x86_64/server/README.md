# NAS x86_64 Server Configuration

This directory contains server configuration files for NAS x86_64 platform.

## Files

- `application.conf` - Ktor server configuration (HOCON format)
- `config.yaml` - Application configuration (YAML format)

## Configuration

The configuration files define:

- **Server settings**: Port (8081), host (0.0.0.0)
- **Database**: SQLite database path (`/var/packages/ip-css/var/db/ip-css.db`)
- **Storage**: Directories for recordings and screenshots
- **Security**: JWT configuration
- **CORS**: Allowed origins for web interface
- **Redis**: Cache configuration

## Environment Variables

You can override configuration using environment variables:

- `JWT_SECRET` - JWT secret key
- `DATABASE_PATH` - Database file path
- `RECORDINGS_PATH` - Recordings directory
- `SCREENSHOTS_PATH` - Screenshots directory
- `REDIS_HOST` - Redis host
- `REDIS_PORT` - Redis port
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins

## Usage

These configuration files are used by:

1. NAS package installations (Synology, QNAP, Asustor)
2. Docker containers
3. Manual server deployments

Copy the appropriate configuration file to your installation directory and adjust as needed.
