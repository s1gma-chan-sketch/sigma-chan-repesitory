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
reader_thread = threading.Thread(target=read, daemon=True)
reader_thread.start()
deadline = time.monotonic() + 150
stage = 0
next_command_at = 0.0
try:
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            raise RuntimeError(f'Server exited unexpectedly: {proc.returncode}')
        log = ''.join(lines)
        if stage == 0 and 'Done (' in log:
            proc.stdin.write('summon sigma_companions:companion 0 -60 0 {Profile:"konata",Mode:"STAY",Needs:{Satiety:50d,Hydration:80d,Energy:90d,PendingNutrition:24,DigestionSeconds:3}}\n')
            proc.stdin.flush()
            stage = 1
            next_command_at = time.monotonic() + 5
        elif stage == 1 and time.monotonic() >= next_command_at:
            proc.stdin.write('data get entity @e[type=sigma_companions:companion,x=0,y=-60,z=0,distance=..8,limit=1,sort=nearest] Profile\n')
            proc.stdin.write('data get entity @e[type=sigma_companions:companion,x=0,y=-60,z=0,distance=..8,limit=1,sort=nearest] Needs\n')
            proc.stdin.write('summon sigma_companions:companion 4 -60 0 {Profile:"mio",Mode:"STAY"}\n')
            proc.stdin.flush()
            stage = 2
            next_command_at = time.monotonic() + 3
        elif stage == 2 and time.monotonic() >= next_command_at:
            proc.stdin.write('data get entity @e[type=sigma_companions:companion,x=4,y=-60,z=0,distance=..8,limit=1,sort=nearest] Profile\n')
            proc.stdin.write('save-all flush\nstop\n')
            proc.stdin.flush()
            stage = 3
        elif stage == 3 and 'Stopping server' in log:
            break
        time.sleep(0.2)
    if stage != 3:
        raise RuntimeError(f'Server smoke did not reach clean shutdown; stage={stage}')
    proc.wait(timeout=90)
    reader_thread.join(timeout=5)
    log = ''.join(lines)
    for marker in ['Loading Minecraft 1.21.1', 'Sigma Companions initialized',
                   'has the following entity data: "konata"', 'has the following entity data: "kagami"',
                   'DigestionSeconds: 0', 'PendingNutrition: 0', 'Stopping server']:
        if marker not in log:
            raise RuntimeError(f'Missing smoke marker: {marker}')
    for error in ['Exception in server tick loop', 'Failed to start the minecraft server',
                  'Failed to parse recipe', 'Parsing error loading recipe',
                  'Exception loading entity', 'Unable to summon entity', 'No entity was found']:
        if error in log:
            raise RuntimeError(f'Server error: {error}')
    if proc.returncode != 0:
        raise RuntimeError(f'Server exit code: {proc.returncode}')
    print('PASS: Fabric 1.21.1 startup, Konata spawn, delayed needs NBT readback, legacy profile migration, clean shutdown.')
except BaseException as error:
    print(f'::error::Smoke failed at stage {stage}: {error}', flush=True)
    for line in lines[-100:]:
        clean = line.rstrip().replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print('SMOKE ' + clean, flush=True)
    print('::error::See uploaded server-diagnostics artifact and smoke output above.', flush=True)
    proc.terminate()
    try:
        proc.wait(timeout=15)
    except subprocess.TimeoutExpired:
        proc.kill()
    raise