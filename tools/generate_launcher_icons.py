#!/usr/bin/env python3
"""Genera los iconos raster (PNG) del launcher a partir del diseno vectorial."""
import math
import os
from PIL import Image, ImageDraw

OUT = "app/src/main/res"
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

NAVY = (11, 19, 43, 255)
NAVY_2 = (21, 34, 56, 255)
SILVER_TOP = (237, 242, 247, 255)
SILVER_MID = (203, 213, 225, 255)
SILVER_BOT = (148, 163, 184, 255)
CYAN = (0, 212, 255, 255)
CYAN_LIGHT = (56, 189, 248, 255)
ORANGE = (255, 152, 0, 255)
RED = (229, 57, 53, 255)
SLATE = (100, 116, 139, 255)
DARK = (11, 19, 43, 255)


def rounded_rect(draw, box, radius, fill):
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def gradient_rounded(size, box, radius, c0, c1, c2):
    """Rellena un rectangulo redondeado con un degradado vertical de 3 paradas."""
    x0, y0, x1, y1 = box
    altura = max(1, y1 - y0)
    capa = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(y0, y1):
        t = (y - y0) / altura
        if t < 0.55:
            u = t / 0.55
            color = tuple(round(c0[i] + (c1[i] - c0[i]) * u) for i in range(4))
        else:
            u = (t - 0.55) / 0.45
            color = tuple(round(c1[i] + (c2[i] - c1[i]) * u) for i in range(4))
        ImageDraw.Draw(capa).rectangle([x0, y, x1, y + 1], fill=color)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(box, radius=radius, fill=255)
    return Image.composite(capa, Image.new("RGBA", (size, size), (0, 0, 0, 0)), mask)


def dibujar_fondo(img, d):
    d.rectangle([0, 0, img.size[0], img.size[1] // 2], fill=NAVY_2)
    d.rectangle([0, img.size[1] // 2, img.size[0], img.size[1]], fill=NAVY)


def dibujar_split(d, s):
    """Cuerpo del split metalico con display digital."""
    escala = s / 108.0
    box = (22 * escala, 40 * escala, 86 * escala, 70 * escala)
    radio = 4 * escala
    d.rounded_rectangle(box, radius=radio, fill=SILVER_MID)
    # Display
    d.rounded_rectangle(
        (54 * escala, 47 * escala, 81 * escala, 63 * escala),
        radius=2 * escala,
        fill=DARK,
    )
    # Digitos "18" en cian
    d.rectangle((59.5 * escala, 51.5 * escala, 60.8 * escala, 59.5 * escala), fill=CYAN)
    d.rectangle((60.8 * escala, 51.5 * escala, 62.8 * escala, 53.2 * escala), fill=CYAN)
    d.rectangle((61.0 * escala, 51.5 * escala, 62.8 * escala, 52.8 * escala), fill=CYAN)
    d.ellipse((70.5 * escala, 50.3 * escala, 76.7 * escala, 56.5 * escala), outline=CYAN, width=max(1, int(1.7 * escala)))
    d.ellipse((72.2 * escala, 52.0 * escala, 75.0 * escala, 54.8 * escala), fill=DARK)
    d.ellipse((70.5 * escala, 55.5 * escala, 76.7 * escala, 61.7 * escala), outline=CYAN, width=max(1, int(1.7 * escala)))
    d.ellipse((72.2 * escala, 57.2 * escala, 75.0 * escala, 60.0 * escala), fill=DARK)
    # Grado
    d.ellipse((77.3 * escala, 47.1 * escala, 79.9 * escala, 49.7 * escala), fill=CYAN)
    # Rejilla
    d.rectangle((30 * escala, 73 * escala, 78 * escala, 76 * escala), fill=SLATE)


def dibujar_ondas(d, s):
    escala = s / 108.0
    d.rounded_rectangle(
        (26 * escala, 76.5 * escala, 78 * escala, 84 * escala),
        radius=4 * escala,
        fill=CYAN,
    )
    d.rounded_rectangle(
        (32 * escala, 84.5 * escala, 73 * escala, 90 * escala),
        radius=3 * escala,
        fill=CYAN_LIGHT,
    )


def dibujar_llama(d, s):
    escala = s / 108.0
    d.polygon(
        [
            (16 * escala, 81 * escala),
            (17 * escala, 73 * escala),
            (22 * escala, 64 * escala),
            (23 * escala, 71 * escala),
            (27 * escala, 75 * escala),
            (27 * escala, 70 * escala),
            (31 * escala, 68 * escala),
            (36 * escala, 78 * escala),
            (35 * escala, 84 * escala),
            (27 * escala, 88 * escala),
            (18 * escala, 87 * escala),
        ],
        fill=ORANGE,
    )
    d.polygon(
        [
            (22 * escala, 81 * escala),
            (23 * escala, 75 * escala),
            (26 * escala, 70 * escala),
            (27 * escala, 76 * escala),
            (30 * escala, 79 * escala),
            (28 * escala, 84 * escala),
            (24 * escala, 84 * escala),
        ],
        fill=RED,
    )


def construir(size, redondo=False):
    img = Image.new("RGBA", (size, size), NAVY)
    d = ImageDraw.Draw(img)
    dibujar_fondo(img, d)
    dibujar_split(d, size)
    dibujar_ondas(d, size)
    dibujar_llama(d, size)

    if redondo:
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
        img.putalpha(mask)
    else:
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle(
            [0, 0, size - 1, size - 1], radius=int(size * 0.18), fill=255
        )
        alpha = img.getchannel("A")
        img.putalpha(Image.composite(alpha, Image.new("L", (size, size), 0), mask))
    return img


def main():
    for nombre, tam in DENSITIES.items():
        carpeta = os.path.join(OUT, f"mipmap-{nombre}")
        os.makedirs(carpeta, exist_ok=True)
        construir(tam).convert("RGB").save(os.path.join(carpeta, "ic_launcher.png"), "PNG", optimize=True)
        construir(tam, redondo=True).save(os.path.join(carpeta, "ic_launcher_round.png"), "PNG", optimize=True)
        print(f"mipmap-{nombre}: {tam}x{tam}")


if __name__ == "__main__":
    main()
