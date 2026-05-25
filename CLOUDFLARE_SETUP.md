# Cloudflare Setup Guide

This guide helps you configure Cloudflare to work with Flossk TTS.

## Common Issue: Subdomain Not Working

If your subdomain doesn't work but direct IP access does, follow these steps:

## Step 1: Configure Cloudflare SSL/TLS Mode

1. Log in to Cloudflare Dashboard
2. Select your domain
3. Go to **SSL/TLS** → **Overview**
4. Set **SSL/TLS encryption mode** to **"Flexible"**

**Why Flexible?**
- Your server runs on HTTP (port 80)
- Cloudflare will handle HTTPS for visitors
- Cloudflare connects to your server via HTTP (port 80)
- This is the correct mode when your origin server doesn't have SSL

**SSL/TLS Modes Explained:**
- **Off**: No encryption (not recommended)
- **Flexible**: HTTPS between visitor ↔ Cloudflare, HTTP between Cloudflare ↔ Your Server ✅ **Use this**
- **Full**: HTTPS both ways (requires SSL certificate on your server)
- **Full (strict)**: HTTPS both ways + valid certificate (requires valid SSL certificate)

## Step 2: Verify DNS Configuration

1. Go to **DNS** → **Records**
2. Find your subdomain record (e.g., `tts.example.com`)
3. Ensure it has the **orange cloud** icon (proxied) ☁️
   - Orange cloud = Proxied (traffic goes through Cloudflare)
   - Gray cloud = DNS-only (direct connection, bypasses Cloudflare)

**If gray cloud:**
- Click the record
- Toggle "Proxy status" to "Proxied" (orange cloud)
- Wait a few minutes for DNS propagation

## Step 3: Check Firewall Rules

Cloudflare uses specific IP ranges. Ensure your firewall allows Cloudflare IPs:

**Cloudflare IP Ranges:**
- IPv4: https://www.cloudflare.com/ips-v4
- IPv6: https://www.cloudflare.com/ips-v6

**Quick Test:**
```bash
# Check if Cloudflare can reach your server
# From Cloudflare's perspective, test from a Cloudflare IP
```

**Common Firewall Commands:**

**UFW (Ubuntu):**
```bash
# Allow Cloudflare IPv4
wget https://www.cloudflare.com/ips-v4 -O /tmp/cf_ips.txt
for ip in $(cat /tmp/cf_ips.txt); do ufw allow from $ip to any port 80; done

# Allow Cloudflare IPv6
wget https://www.cloudflare.com/ips-v6 -O /tmp/cf_ips_v6.txt
for ip in $(cat /tmp/cf_ips_v6.txt); do ufw allow from $ip to any port 80; done
```

**iptables:**
```bash
# Allow Cloudflare IPv4
wget https://www.cloudflare.com/ips-v4 -O /tmp/cf_ips.txt
for ip in $(cat /tmp/cf_ips.txt); do iptables -A INPUT -p tcp -s $ip --dport 80 -j ACCEPT; done
```

## Step 4: Verify Server Configuration

Ensure your `application.properties` has:

```properties
server.port=80
server.address=0.0.0.0
server.forward-headers-strategy=native
```

## Step 5: Test Connection

**Test from your server:**
```bash
# Test local connection
curl -H "Host: tts.example.com" http://localhost/admin

# Test with your actual subdomain
curl -H "Host: tts.example.com" http://YOUR_IP/admin
```

**Test from Cloudflare:**
1. Visit `https://tts.example.com/admin` in your browser
2. Check browser console for errors
3. Check server logs for connection attempts

## Troubleshooting

### Issue: "502 Bad Gateway" or "Connection Refused"

**Possible causes:**
1. SSL/TLS mode is "Full" but server doesn't have SSL → Set to "Flexible"
2. Firewall blocking Cloudflare IPs → Allow Cloudflare IP ranges
3. Server not listening on port 80 → Check `server.port=80` in config
4. Server not binding to 0.0.0.0 → Check `server.address=0.0.0.0`

**Solution:**
```bash
# Check if server is listening
netstat -tlnp | grep :80
# Or
ss -tlnp | grep :80

# Check firewall
sudo ufw status
# Or
sudo iptables -L -n
```

### Issue: "522 Connection Timeout"

**Possible causes:**
1. Firewall blocking Cloudflare IPs
2. Server not responding
3. Wrong port configured

**Solution:**
- Allow Cloudflare IP ranges in firewall
- Verify server is running: `ps aux | grep flossk-tts`
- Check server logs for errors

### Issue: Subdomain Shows Different Site or Error

**Possible causes:**
1. DNS not propagated (wait 5-15 minutes)
2. Browser cache (clear cache or use incognito)
3. Wrong Host header

**Solution:**
- Wait for DNS propagation
- Clear browser cache
- Test with `curl -H "Host: tts.example.com" http://YOUR_IP/admin`

### Issue: Works with IP but Not Subdomain

**Checklist:**
- ✅ SSL/TLS mode set to "Flexible"
- ✅ DNS record has orange cloud (proxied)
- ✅ Firewall allows Cloudflare IPs
- ✅ Server listening on port 80
- ✅ `server.forward-headers-strategy=native` in config

## Advanced: Using HTTPS on Origin (Optional)

If you want to use HTTPS on your server (Full SSL mode):

1. **Obtain SSL Certificate:**
   ```bash
   # Using Let's Encrypt
   sudo certbot certonly --standalone -d tts.example.com
   ```

2. **Configure Spring Boot for HTTPS:**
   ```properties
   server.port=443
   server.ssl.enabled=true
   server.ssl.key-store=/path/to/keystore.p12
   server.ssl.key-store-password=your-password
   server.ssl.key-store-type=PKCS12
   ```

3. **Set Cloudflare SSL/TLS to "Full" or "Full (strict)"**

## Quick Reference

**Cloudflare Dashboard:**
- SSL/TLS → Overview → Set to "Flexible"
- DNS → Records → Ensure orange cloud (proxied)
- Firewall → Rules → Allow Cloudflare IPs (if using firewall)

**Server Configuration:**
```properties
server.port=80
server.address=0.0.0.0
server.forward-headers-strategy=native
```

**Test Commands:**
```bash
# Local test
curl -H "Host: tts.example.com" http://localhost/admin

# External test
curl -H "Host: tts.example.com" http://YOUR_IP/admin

# Check listening ports
ss -tlnp | grep :80
```

## Support

If issues persist:
1. Check Cloudflare Analytics → Performance → Check for errors
2. Check server logs: `tail -f /var/log/flossk-tts/application.log`
3. Verify DNS: `dig tts.example.com`
4. Test direct IP access: `http://YOUR_IP/admin`
