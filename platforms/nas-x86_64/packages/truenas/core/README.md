# IP-CSS on TrueNAS CORE (FreeBSD Jail)

TrueNAS CORE is based on FreeBSD. You can run IP-CSS inside an **iocage jail** with Java and the server JAR.

## Requirements

- TrueNAS CORE 13.x or later (iocage)
- Jail with network access and storage mount for data

## Option 1: Script (recommended)

From a shell on TrueNAS (or SSH):

```bash
# Copy setup-jail.sh to the host and run as root
chmod +x setup-jail.sh
./setup-jail.sh
```

The script creates a jail, installs Java, mounts a dataset for data, and runs the IP-CSS server.

## Option 2: Manual jail setup

1. Create a dataset for IP-CSS data, e.g. `mnt/tank/ip-css`.
2. Create a jail (e.g. `ip-css`) with iocage, enable VNET if you need DHCP.
3. Mount the dataset into the jail, e.g. `/mnt/tank/ip-css` → `/var/db/ip-css` in the jail.
4. Inside the jail, install OpenJDK 17 and copy the server JAR and config.
5. Run the server with `java -jar server.jar -config=/path/to/config.yaml`.

## Ports

- **8080** — Web UI (map in jail properties)
- **8081** — API (map in jail properties)

## Data paths in jail

- Database: `/var/db/ip-css/db/ip-css.db`
- Recordings: `/var/db/ip-css/recordings`
- Config: `/var/db/ip-css/config/config.yaml`
