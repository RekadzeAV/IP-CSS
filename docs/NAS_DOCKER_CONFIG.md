# NAS Docker Configuration for IP-CSS

**Date:** 2026-06-09  
**Status:** ✅ Ready

---

## 📦 Docker Volume Configuration

### Option 1: Bind Mount (Recommended for Development)

Add to `docker-compose.yml` under `surveillance-api` service:

```yaml
services:
  surveillance-api:
    image: ip-css-surveillance:latest
    volumes:
      - ./data/recordings:/data/recordings:rw
```

### Option 2: NFS Volume (Recommended for Production)

Add to `docker-compose.yml`:

```yaml
services:
  surveillance-api:
    image: ip-css-surveillance:latest
    volumes:
      - nas-recordings:/data/recordings:rw

volumes:
  nas-recordings:
    driver: local
    driver_opts:
      type: nfs
      o: addr=192.168.10.37,rw,nolock,hard,intr
      device: ":/storage/recordings"
```

### Option 3: Pre-mounted Volume

Mount NAS on host system first, then bind mount:

```bash
# On Linux host
sudo mkdir -p /mnt/nas-recordings
sudo mount -t nfs 192.168.10.37:/storage/recordings /mnt/nas-recordings

# In docker-compose.yml
services:
  surveillance-api:
    volumes:
      - /mnt/nas-recordings:/data/recordings:rw
```

---

## 📁 Directory Structure

```
/data/recordings/
├── first-contour/
│   ├── camera-17/
│   │   ├── 2026-06-09/
│   │   │   ├── rec_14_30_00.mp4
│   │   │   └── rec_14_35_00.mp4
│   │   └── ...
│   ├── camera-20/
│   ├── camera-21/
│   ├── camera-22/
│   ├── camera-23/
│   ├── camera-24/
│   └── camera-26/
└── archive/
    └── ...
```

---

## 🔐 Permissions

### Set Correct Permissions

```bash
# On NAS server
chown -R 1000:1000 /storage/recordings
chmod -R 755 /storage/recordings

# Or in Docker container
docker exec -it surveillance-api chown -R 1000:1000 /data/recordings
```

---

## ✅ Verification

### Check Mount Status

```bash
# In Docker container
docker exec -it surveillance-api df -h /data/recordings

# Check directory listing
docker exec -it surveillance-api ls -la /data/recordings/
```

### Test Write Access

```bash
# Create test file
docker exec -it surveillance-api touch /data/recordings/test_write

# Verify on NAS
ssh admin@192.168.10.37 "ls -la /storage/recordings/test_write"
```

---

## 🚀 Deployment Steps

1. **Update docker-compose.yml** with NFS volume configuration
2. **Set environment variables** in `.env.nas`
3. **Mount NAS** on host system (if using Option 3)
4. **Restart containers**:
   ```bash
   docker-compose down
   docker-compose up -d
   ```
5. **Verify mount**:
   ```bash
   docker-compose exec surveillance-api df -h /data/recordings
   ```

---

## 📊 Monitoring

### Check Disk Usage

```bash
docker exec -it surveillance-api df -h /data/recordings
```

### View Recent Recordings

```bash
docker exec -it surveillance-api find /data/recordings -type f -mtime -1 -name "*.mp4"
```

---

*Created: 2026-06-09*  
*Version: 1.0*
