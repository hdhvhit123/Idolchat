"""Generate HTML pages for Play Store assets; rendered to PNG by Chrome headless."""
import pathlib, sys

HERE = pathlib.Path(sys.argv[1])
FONT = pathlib.Path(sys.argv[2])  # dir with be_vietnam_pro_*.ttf
RAW = HERE / "raw"
HTML = HERE / "html_en"
HTML.mkdir(exist_ok=True)

def uri(p): return pathlib.Path(p).resolve().as_uri()

FONTS = f"""
@font-face{{font-family:BVP;font-weight:400;src:url('{uri(FONT/'be_vietnam_pro_regular.ttf')}')}}
@font-face{{font-family:BVP;font-weight:500;src:url('{uri(FONT/'be_vietnam_pro_medium.ttf')}')}}
@font-face{{font-family:BVP;font-weight:600;src:url('{uri(FONT/'be_vietnam_pro_semibold.ttf')}')}}
@font-face{{font-family:BVP;font-weight:700;src:url('{uri(FONT/'be_vietnam_pro_bold.ttf')}')}}
*{{margin:0;padding:0;box-sizing:border-box}}
html,body{{overflow:hidden}}
body{{font-family:BVP,'Segoe UI',sans-serif;-webkit-font-smoothing:antialiased}}
"""

BG = {
    "violet": "linear-gradient(160deg,#6C4DFF 0%,#9B3FD1 45%,#FF5C9A 100%)",
    "night":  "linear-gradient(165deg,#17142B 0%,#2A1650 55%,#6C2A7A 100%)",
    "mint":   "linear-gradient(160deg,#6C4DFF 0%,#4B8BE6 50%,#2ED3B7 100%)",
}

GLOW = """<div style="position:absolute;width:900px;height:900px;border-radius:50%;
 background:radial-gradient(circle,rgba(255,255,255,.22),rgba(255,255,255,0) 65%);left:-300px;top:-260px"></div>
<div style="position:absolute;width:800px;height:800px;border-radius:50%;
 background:radial-gradient(circle,rgba(255,92,154,.35),rgba(255,92,154,0) 65%);right:-320px;bottom:-200px"></div>"""

def caption(title, sub):
    return f"""<div style="position:absolute;left:0;right:0;top:120px;text-align:center;color:#fff;padding:0 70px">
 <div style="font-weight:700;font-size:78px;line-height:1.15;letter-spacing:-1px">{title}</div>
 <div style="font-weight:500;font-size:36px;line-height:1.35;margin-top:22px;opacity:.9">{sub}</div></div>"""

def phone(img, width=620, top=440):
    h = width * 2400 / 1080
    return f"""<div style="position:absolute;left:{(1080-width)//2-14}px;top:{top}px;width:{width+28}px;height:{h+28:.0f}px;
 background:#0E0D14;border-radius:70px;padding:14px;box-shadow:0 40px 90px rgba(20,0,60,.45),0 0 0 2px rgba(255,255,255,.18)">
 <img src="{uri(img)}" style="width:{width}px;height:{h:.0f}px;border-radius:56px;display:block"></div>"""

def page(name, w, h, body, bg):
    (HTML / f"{name}.html").write_text(f"""<!doctype html><html><head><meta charset="utf-8"><style>{FONTS}
body{{width:{w}px;height:{h}px;background:{bg};position:relative}}</style></head><body>{body}</body></html>""", encoding="utf-8")

shots = [
    ("01_studio", "02_studio_group_lysn.png", "violet", "Build idol chat stories<br>in a few taps", "Pick who is talking, type, done"),
    ("02_tap_to_chat", "04_player_typing.png", "night", "Replay it like<br>a real chat", "Tap to reveal each message, with “typing…”"),
    ("03_styles", "05_studio_instagram.png", "violet", "3 chat styles,<br>one tap to switch", "Soft pastel, neon dark, sunset gradient"),
    ("04_narrator", "03_studio_weverse_narrator.png", "night", "One-tap<br>narration", "“Seen”, “Next day”, “3 days later”…"),
    ("06_home", "01_home.png", "mint", "Start fast<br>with templates", "Or write your own, saved on your phone"),
    ("07_characters", "08_edit_character.png", "violet", "Your own<br>cast", "Names, colours and avatars you choose"),
]
for name, img, bg, t, s in shots:
    page(name, 1080, 1920, GLOW + caption(t, s) + phone(RAW / img), BG[bg])

# export showcase: two exported images as cards
cards = f"""
<div style="position:absolute;left:70px;top:520px;width:560px;transform:rotate(-6deg);border-radius:36px;overflow:hidden;
 box-shadow:0 40px 90px rgba(20,0,60,.5)"><img src="{uri(RAW/'07_export_weverse.png')}" style="width:100%;display:block"></div>
<div style="position:absolute;right:70px;top:760px;width:560px;transform:rotate(5deg);border-radius:36px;overflow:hidden;
 box-shadow:0 40px 90px rgba(20,0,60,.55)"><img src="{uri(RAW/'07_export_instagram.png')}" style="width:100%;display:block"></div>
<div style="position:absolute;left:50%;bottom:120px;transform:translateX(-50%);background:#fff;color:#4A2BD6;font-weight:700;
 font-size:40px;padding:26px 56px;border-radius:999px;box-shadow:0 20px 50px rgba(20,0,60,.35);white-space:nowrap">⤴ Save & share instantly</div>"""
page("05_export", 1080, 1920, GLOW + caption("Export as a long image", "Save to your gallery, post it anywhere") + cards, BG["violet"])

# icon 512 (full-bleed square; Play applies its own mask)
svg = """<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#6C63FF"/><stop offset="1" stop-color="#FF6584"/></linearGradient></defs>
<rect width="108" height="108" fill="url(#g)"/>
<path fill="#FFFFFF" fill-opacity=".70" d="M48,30h22a10,10 0,0 1,10 10v12a10,10 0,0 1,-10 10h-2l0,7l-8,-7h-12a10,10 0,0 1,-10 -10v-12a10,10 0,0 1,10 -10z"/>
<path fill="#FFFFFF" d="M36,44h24a10,10 0,0 1,10 10v12a10,10 0,0 1,-10 10h-14l-9,8l0,-8h-1a10,10 0,0 1,-10 -10v-12a10,10 0,0 1,10 -10z"/>
<path fill="#FF6584" d="M48,70c-0.6,0 -9,-5.2 -9,-10.8c0,-3 2.3,-5.2 5,-5.2c1.7,0 3.1,0.9 4,2.2c0.9,-1.3 2.3,-2.2 4,-2.2c2.7,0 5,2.2 5,5.2c0,5.6 -8.4,10.8 -9,10.8z"/></svg>"""
(HERE / "icon.svg").write_text(svg, encoding="utf-8")
page("icon_512", 512, 512, svg, "#6C63FF")

# feature graphic 1024x500
icon_small = svg.replace('width="512" height="512"', 'width="150" height="150"')
fg = f"""{GLOW}
<div style="position:absolute;left:64px;top:96px;width:150px;height:150px;border-radius:36px;overflow:hidden;
 box-shadow:0 18px 40px rgba(20,0,60,.35)">{icon_small}</div>
<div style="position:absolute;left:64px;top:272px;color:#fff;width:560px">
 <div style="font-weight:700;font-size:62px;letter-spacing:-1px;line-height:1.05">Idol Chat Maker</div>
 <div style="font-weight:500;font-size:25px;margin-top:14px;white-space:nowrap;opacity:.92">Create idol chat stories, export as images</div></div>
<div style="position:absolute;left:600px;top:60px;width:210px;height:466px;background:#0E0D14;border-radius:30px;padding:7px;
 transform:rotate(-7deg);box-shadow:0 24px 50px rgba(20,0,60,.5)"><img src="{uri(RAW/'02_studio_group_lysn.png')}" style="width:196px;border-radius:24px;display:block"></div>
<div style="position:absolute;left:790px;top:30px;width:210px;height:466px;background:#0E0D14;border-radius:30px;padding:7px;
 transform:rotate(6deg);box-shadow:0 24px 50px rgba(20,0,60,.55)"><img src="{uri(RAW/'05_studio_instagram.png')}" style="width:196px;border-radius:24px;display:block"></div>"""
page("feature_1024x500", 1024, 500, fg, BG["violet"])
print("ok")
