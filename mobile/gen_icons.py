#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 G-ai 安卓启动图标（简洁风格：深色圆角方块 + 白色 G + 青色节点点）"""
from PIL import Image, ImageDraw, ImageFont
import os

BASE = "/home/user/Doubao/chats/38441427699972610/G-ai/res"

def font_path(size):
    for p in [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/opt/python3.12/lib/python3.12/site-packages/PIL/fonts/DejaVuSans-Bold.ttf",
    ]:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()

def make_icon(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = size * 0.22
    # 背景圆角方块（深色渐变感：两层层叠）
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=(13, 18, 32, 255))
    d.rounded_rectangle([size * 0.03, size * 0.03, size * 0.97, size * 0.97],
                        radius=r * 0.9, fill=(16, 24, 40, 255))
    # 内圈描边
    d.rounded_rectangle([size * 0.08, size * 0.08, size * 0.92, size * 0.92],
                        radius=r * 0.7, outline=(45, 62, 90, 255), width=max(1, size // 64))
    # 白色 G 字母
    f = font_path(int(size * 0.52))
    bbox = d.textbbox((0, 0), "G", font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text(((size - tw) / 2 - bbox[0], (size - th) / 2 - bbox[1] - size * 0.02), "G",
           font=f, fill=(255, 255, 255, 255))
    # 青色节点点（右上）
    dot_r = size * 0.075
    cx, cy = size * 0.72, size * 0.26
    d.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r], fill=(34, 211, 238, 255))
    d.ellipse([cx - dot_r * 0.45, cy - dot_r * 0.45, cx + dot_r * 0.45, cy + dot_r * 0.45],
              fill=(200, 247, 253, 255))
    return img

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for folder, s in sizes.items():
    d = os.path.join(BASE, folder)
    os.makedirs(d, exist_ok=True)
    make_icon(s).save(os.path.join(d, "ic_launcher.png"))
    print("generated", folder, s)

# 分享/帮助里用的 512 大图
out = os.path.join(BASE, "drawable")
os.makedirs(out, exist_ok=True)
make_icon(256).save(os.path.join(out, "ic_gai_256.png"))
print("generated drawable/ic_gai_256.png")
