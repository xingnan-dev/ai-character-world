param([int]$FrontendPort = 15173)
$ErrorActionPreference = 'Stop'
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $FrontendPort)
try { $listener.Start() } catch { throw "Port $FrontendPort is already in use; no process was stopped." } finally { try { $listener.Stop() } catch {} }
$root = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $root 'frontend')
try { npm run dev -- --mode acceptance --host 127.0.0.1 --port $FrontendPort } finally { Pop-Location }
