#!/usr/bin/env python3
"""Disposable CI smoke test: real server, entity spawn/readback, clean shutdown.
WARNING: accepts the Minecraft EULA and replaces run/server.properties.
Do not point this script at a production server directory.
"""
from pathlib import Path
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / 'run'
RUN.mkdir(exist_ok=True)
(RUN / 'eula.txt').write_text('eula=true\n')
(RUN / 'server.properties').write_text(
    'online-mode=false\nserver-port=25599\nlevel-type=minecraft\\:flat\n'
    'spawn-protection=0\nmax-tick-time=180000\nsync-chunk-writes=false\n'
    'view-distance=4\nsimulation-distance=4\n'
)
proc = subprocess.Popen(
    ['./gradlew', 'runServer', '--no-daemon', '--console=plain'], cwd=ROOT,
    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    text=True, bufsize=1
)
lines = []
log_file = (RUN / 'smoke.log').open('w', encoding='utf-8')
def read():
    for line in proc.stdout:
        lines.append(line)
        log_file.write(line)
        log_file.flush()
        print(line, end='', flush=True)
    log_file.close()
threading.Thread(target=read, daemon=True).start()
deadline = time.monotonic() + 150
stage = 0
try:
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            raise RuntimeError(f'Server exited unexpectedly: {proc.returncode}')
        log = ''.join(lines)
        if stage == 0 and 'Done (' in log:
            proc.stdin.write('summon sigma_companions:companion 0 -60 0 {Profile:"mio"}\n')
            proc.stdin.flush()
            stage = 1
        elif stage == 1 and 'Summoned new' in log:
            proc.stdin.write('data get entity @e[type=sigma_companions:companion,limit=1] Profile\n')
            proc.stdin.flush()
            stage = 2
        elif stage == 2 and 'has the following entity data: "mio"' in log:
            proc.stdin.write('save-all flush\nstop\n')
            proc.stdin.flush()
            stage = 3
        elif stage == 3 and 'Stopping server' in log:
            break
        time.sleep(0.2)
    if stage != 3:
        raise RuntimeError(f'Server smoke did not reach clean shutdown; stage={stage}')
    proc.wait(timeout=90)
    thread.join(timeout=5)
    log = ''.join(lines)
    for marker in ['Loading Minecraft 1.21.1', 'Sigma Companions initialized',
                   'has the following entity data: "mio"', 'Stopping server']:
        if marker not in log:
            raise RuntimeError(f'Missing smoke marker: {marker}')
    for error in ['Exception in server tick loop', 'Failed to start the minecraft server',
                  'Failed to parse recipe', 'Parsing error loading recipe']:
        if error in log:
            raise RuntimeError(f'Server error: {error}')
    if proc.returncode != 0:
        raise RuntimeError(f'Server exit code: {proc.returncode}')
    print('PASS: Fabric 1.21.1 startup, companion spawn, profile NBT readback, clean shutdown.')
except BaseException as error:
    print(f'::error::Smoke failed at stage {stage}: {error}', flush=True)
    for line in lines[-70:]:
        print('::error::' + line.rstrip().replace('%', '%25'), flush=True)
    proc.terminate()
    try:
        proc.wait(timeout=15)
    except subprocess.TimeoutExpired:
        proc.kill()
    raise