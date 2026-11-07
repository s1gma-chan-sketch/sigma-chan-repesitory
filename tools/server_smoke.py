#!/usr/bin/env python3
"""Start a real Fabric dedicated server, exercise registration, stop cleanly.
Only run in a disposable CI checkout. Accepts the Minecraft EULA for this test environment.
"""
from pathlib import Path
import os
import subprocess
import sys
import threading
import time

ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'run'
RUN.mkdir(exist_ok=True)
(RUN/'eula.txt').write_text('eula=true\n')
(RUN/'server.properties').write_text('online-mode=false\nserver-port=25599\nlevel-type=minecraft\\:flat\nspawn-protection=0\nmax-tick-time=180000\nsync-chunk-writes=false\nview-distance=4\nsimulation-distance=4\n')
proc=subprocess.Popen(['./gradlew','runServer','--no-daemon','--console=plain'],cwd=ROOT,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
lines=[]
def read():
    for line in proc.stdout:
        lines.append(line)
        print(line,end='',flush=True)
thread=threading.Thread(target=read,daemon=True);thread.start()
deadline=time.monotonic()+360
sent=False
try:
    while time.monotonic()<deadline:
        if proc.poll() is not None:
            raise RuntimeError(f'Server exited before startup: {proc.returncode}')
        if not sent and any('Done (' in line for line in lines):
            proc.stdin.write('sigma\nstop\n');proc.stdin.flush();sent=True
        if sent and any('Stopping server' in line for line in lines):
            break
        time.sleep(.2)
    if not sent: raise RuntimeError('Server did not become ready within 6 minutes')
    proc.wait(timeout=90)
    log=''.join(lines)
    for marker in ['Loading Minecraft 1.21.1','Sigma Companions initialized','sigma_companions']:
        if marker not in log: raise RuntimeError(f'Missing smoke marker: {marker}')
    if 'Exception in server tick loop' in log or 'Failed to start the minecraft server' in log:
        raise RuntimeError('Server startup failed')
    print('Dedicated server smoke test passed.')
except BaseException:
    proc.terminate()
    try: proc.wait(timeout=15)
    except subprocess.TimeoutExpired: proc.kill()
    raise