"""Convert an 8-bit RGB non-interlaced PNG to RGBA (opaque alpha). Stdlib only."""
import struct, sys, zlib

src, dst = sys.argv[1], sys.argv[2]
data = open(src, 'rb').read()
assert data[:8] == b'\x89PNG\r\n\x1a\n'
pos, idat, ihdr = 8, b'', None
while pos < len(data):
    ln, = struct.unpack('>I', data[pos:pos+4]); typ = data[pos+4:pos+8]
    body = data[pos+8:pos+8+ln]; pos += 12 + ln
    if typ == b'IHDR': ihdr = body
    elif typ == b'IDAT': idat += body
w, h, bd, ct, _, _, il = struct.unpack('>IIBBBBB', ihdr)
assert bd == 8 and ct == 2 and il == 0, (bd, ct, il)
raw = zlib.decompress(idat); bpp = 3; stride = w * bpp
out, prev, i = bytearray(), bytearray(stride), 0
for y in range(h):
    f = raw[i]; line = bytearray(raw[i+1:i+1+stride]); i += 1 + stride
    for x in range(stride):
        a = line[x-bpp] if x >= bpp else 0; b = prev[x]; c = prev[x-bpp] if x >= bpp else 0
        if f == 1: line[x] = (line[x] + a) & 255
        elif f == 2: line[x] = (line[x] + b) & 255
        elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
        elif f == 4:
            p = a + b - c; pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
            line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
    out.append(0)
    for x in range(w): out += line[x*3:x*3+3] + b'\xff'
    prev = line
def chunk(t, b): return struct.pack('>I', len(b)) + t + b + struct.pack('>I', zlib.crc32(t + b) & 0xffffffff)
png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(bytes(out), 9)) + chunk(b'IEND', b'')
open(dst, 'wb').write(png); print(dst, w, h)
