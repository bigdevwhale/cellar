"""Draws the README logo from the launcher icon's shapes: a dusty bottle with a red capsule in a candlelit arch, as docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling


def main():
    n = S * K
    u = n / 108
    im = Image.new("RGBA", (n, n), (0x17, 0x12, 0x0F, 255))
    d = ImageDraw.Draw(im)

    def rect(x0, y0, x1, y1, fill):
        d.rectangle([x0 * u, y0 * u, x1 * u, y1 * u], fill=fill)

    def poly(points, fill):
        d.polygon([(x * u, y * u) for x, y in points], fill=fill)

    # The arch, its dark inside, the floor.
    d.pieslice([22 * u, 12 * u, 86 * u, 76 * u], 180, 360, fill=(0x5A, 0x46, 0x36))
    rect(22, 44, 86, 92, (0x5A, 0x46, 0x36))
    d.pieslice([28 * u, 18 * u, 80 * u, 70 * u], 180, 360, fill=(0x2A, 0x1F, 0x18))
    rect(28, 44, 80, 92, (0x2A, 0x1F, 0x18))
    # Candlelight behind the bottle.
    glow = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([32 * u, 28 * u, 76 * u, 72 * u], fill=(0xE9, 0xA4, 0x47, 90))
    im = Image.alpha_composite(im, glow.filter(ImageFilter.GaussianBlur(9 * u)))
    d = ImageDraw.Draw(im)
    rect(22, 84, 86, 92, (0x3F, 0x2A, 0x19))
    d.ellipse([40 * u, 81.6 * u, 68 * u, 86.4 * u], fill=(0x0C, 0x09, 0x07))
    # The bottle.
    body = [(45, 83), (45, 52), (46, 47.5), (50.5, 42), (50.5, 25), (57.5, 25), (57.5, 42), (62, 47.5), (63, 52), (63, 83)]
    poly(body, (0x2E, 0x4A, 0x2B))
    poly([(57.5, 25), (57.5, 42), (62, 47.5), (63, 52), (63, 83), (58, 83), (58, 52), (55.5, 42), (55.5, 25)], (0x20, 0x34, 0x1E))
    rect(47.5, 52, 49.5, 80, (0x55, 0x6E, 0x52))
    poly([(45, 52), (46, 47.5), (50.5, 42), (57.5, 42), (62, 47.5), (63, 52)], (0x5C, 0x66, 0x52))
    rect(50, 21, 58, 30, (0xA3, 0x26, 0x2A))
    rect(50, 21, 53, 30, (0xC2, 0x40, 0x45))
    rect(46.5, 60, 61.5, 75, (0xEA, 0xDC, 0xBE))
    rect(49, 65, 59, 66.2, (0x3A, 0x2A, 0x1C))
    rect(50, 69, 58, 70, (0x3A, 0x2A, 0x1C))

    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
