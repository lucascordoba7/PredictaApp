#!/usr/bin/env python3
"""
Dallara P217 LMP2 — "NOLE 24" Novak Djokovic tribute livery.
Generates a professional 5-view concept sheet (SVG + PNG) suitable as a
reference spec for Trading Paints recreation.

This is a hand-built vector concept sheet (not a photo render): clean studio
backdrop, exact colour callouts, authentic sponsor placement map.
"""
import math
import random

# ----------------------------------------------------------------------------
# Palette
# ----------------------------------------------------------------------------
NAVY      = "#0C1E3E"   # Deep Serbian navy (base)
NAVY_DK   = "#081530"   # Shadow navy
NAVY_HI   = "#16315c"   # Lit navy
WHITE     = "#F4F7FB"
WHITE_PUR = "#FFFFFF"
RED       = "#C8102E"   # Serbian / accent red
RED_DK    = "#8E0B20"
BALL      = "#D7F25B"   # Tennis-ball optic yellow-green (trajectory accents)
BALL_DK   = "#A9C72E"
GOLD      = "#E7C36A"   # Grand Slam trophy gold
SILVER    = "#C9D2DC"
BG        = "#15181C"   # Studio charcoal backdrop
BG_GRAD_T = "#23282E"
BG_GRAD_B = "#0D0F12"
INK       = "#E8ECF1"   # Text
INK_DIM   = "#9AA4B0"
PANEL     = "#1C2025"

W, H = 2000, 2680

# ----------------------------------------------------------------------------
# Small helpers
# ----------------------------------------------------------------------------
def star(cx, cy, r, rot=-90, fill=WHITE, opacity=1.0):
    pts = []
    for i in range(5):
        a_out = math.radians(rot + i * 72)
        a_in  = math.radians(rot + i * 72 + 36)
        pts.append((cx + r * math.cos(a_out), cy + r * math.sin(a_out)))
        pts.append((cx + r * 0.42 * math.cos(a_in), cy + r * 0.42 * math.sin(a_in)))
    p = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
    return f'<polygon points="{p}" fill="{fill}" opacity="{opacity}"/>'

def serbian_flag(x, y, w, h, eagle=False):
    """Small horizontal tricolour red/blue/white."""
    b = h / 3.0
    return (
        f'<g>'
        f'<rect x="{x}" y="{y}" width="{w}" height="{b:.1f}" fill="{RED}"/>'
        f'<rect x="{x}" y="{y+b:.1f}" width="{w}" height="{b:.1f}" fill="{NAVY}"/>'
        f'<rect x="{x}" y="{y+2*b:.1f}" width="{w}" height="{b:.1f}" fill="{WHITE_PUR}"/>'
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="none" stroke="#00000055" stroke-width="0.6"/>'
        f'</g>'
    )

def trophy(cx, cy, s, fill=GOLD, stroke=NAVY_DK, label=None):
    """Stylised generic Grand Slam trophy/cup icon (no real brand marks)."""
    g = [f'<g stroke="{stroke}" stroke-width="{0.9}">']
    g.append(f'<path d="M {cx-1.4*s} {cy-1.7*s} '
             f'Q {cx-1.4*s} {cy+0.2*s} {cx-0.5*s} {cy+0.4*s} '
             f'L {cx+0.5*s} {cy+0.4*s} '
             f'Q {cx+1.4*s} {cy+0.2*s} {cx+1.4*s} {cy-1.7*s} Z" fill="{fill}"/>')
    # handles
    g.append(f'<path d="M {cx-1.4*s} {cy-1.3*s} q {-0.8*s} {0.1*s} {-0.7*s} {0.9*s} q {0.1*s} {0.5*s} {0.7*s} {0.4*s}" fill="none" stroke-width="{1.1}"/>')
    g.append(f'<path d="M {cx+1.4*s} {cy-1.3*s} q {0.8*s} {0.1*s} {0.7*s} {0.9*s} q {-0.1*s} {0.5*s} {-0.7*s} {0.4*s}" fill="none" stroke-width="{1.1}"/>')
    # stem + base
    g.append(f'<rect x="{cx-0.22*s}" y="{cy+0.4*s}" width="{0.44*s}" height="{0.7*s}" fill="{fill}"/>')
    g.append(f'<rect x="{cx-0.9*s}" y="{cy+1.05*s}" width="{1.8*s}" height="{0.35*s}" rx="{0.1*s}" fill="{fill}"/>')
    g.append('</g>')
    if label:
        g.append(f'<text x="{cx}" y="{cy+2.2*s}" font-family="IBM Plex Mono, monospace" '
                 f'font-size="{0.85*s}" fill="{INK}" text-anchor="middle" font-weight="700">{label}</text>')
    return "".join(g)

def tennis_traj(x1, y1, x2, y2, bow, color=BALL, w=3.0, op=0.9, dash=None):
    mx, my = (x1 + x2) / 2, (y1 + y2) / 2
    # perpendicular offset for the bow
    dx, dy = x2 - x1, y2 - y1
    L = math.hypot(dx, dy) or 1
    nx, ny = -dy / L, dx / L
    cx, cy = mx + nx * bow, my + ny * bow
    d = f'M {x1:.1f} {y1:.1f} Q {cx:.1f} {cy:.1f} {x2:.1f} {y2:.1f}'
    da = f' stroke-dasharray="{dash}"' if dash else ''
    return f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{w}" opacity="{op}" stroke-linecap="round"{da}/>'

# ----------------------------------------------------------------------------
# Djokovic portrait — tasteful monochrome blended profile silhouette.
# Stylised, low-contrast, NOT an oversized likeness. Built from a profile path
# + halftone dot field, tinted to sit "into" the navy bodywork.
# ----------------------------------------------------------------------------
def portrait(cx, cy, scale, flip=False, clip_id="pclip"):
    s = scale
    f = -1 if flip else 1
    def P(px, py):
        return f"{cx + f*px*s:.1f} {cy + py*s:.1f}"
    # A simplified left-facing profile (forehead, nose, lips, chin, jaw, neck, hair).
    path = (
        f'M {P(38,-46)} '
        f'C {P(20,-52)} {P(-2,-50)} {P(-12,-38)} '          # hairline / forehead top
        f'C {P(-20,-28)} {P(-19,-18)} {P(-22,-8)} '          # brow
        f'C {P(-30,-4)} {P(-34,4)} {P(-30,8)} '              # nose bridge -> tip
        f'C {P(-27,11)} {P(-24,12)} {P(-26,16)} '            # nostril
        f'C {P(-28,20)} {P(-22,22)} {P(-20,24)} '           # upper lip
        f'C {P(-22,28)} {P(-18,30)} {P(-16,33)} '            # lips/chin
        f'C {P(-18,40)} {P(-10,46)} {P(2,46)} '             # chin -> jaw
        f'C {P(16,46)} {P(26,40)} {P(30,30)} '             # jawline
        f'L {P(34,48)} L {P(52,48)} '                       # neck down
        f'L {P(52,-30)} '                                    # back of head up
        f'C {P(52,-44)} {P(48,-48)} {P(38,-46)} Z'          # crown
    )
    halftone = []
    rnd = random.Random(24)
    for _ in range(220):
        px = rnd.uniform(-34, 52)
        py = rnd.uniform(-50, 48)
        r = rnd.uniform(0.5, 1.7)
        halftone.append(f'<circle cx="{cx + f*px*s:.1f}" cy="{cy + py*s:.1f}" r="{r:.2f}" fill="{WHITE_PUR}" opacity="{rnd.uniform(0.05,0.22):.2f}"/>')
    return (
        f'<clipPath id="{clip_id}"><path d="{path}"/></clipPath>'
        f'<path d="{path}" fill="{NAVY_HI}" opacity="0.55"/>'
        f'<path d="{path}" fill="none" stroke="{WHITE_PUR}" stroke-width="1.1" opacity="0.30"/>'
        f'<g clip-path="url(#{clip_id})">{"".join(halftone)}</g>'
    )

# ----------------------------------------------------------------------------
# Tennis-court line graphic (plan), used on hood / roof / engine cover.
# ----------------------------------------------------------------------------
def court(x, y, w, h, col=WHITE_PUR, op=0.9, sw=1.6):
    lines = []
    R = lambda *a: lines.append(f'<rect x="{a[0]:.1f}" y="{a[1]:.1f}" width="{a[2]:.1f}" height="{a[3]:.1f}" fill="none" stroke="{col}" stroke-width="{sw}" opacity="{op}"/>')
    L = lambda x1,y1,x2,y2,o=op: lines.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{col}" stroke-width="{sw}" opacity="{o}"/>')
    R(x, y, w, h)                                   # outer (doubles)
    inset = w * 0.11
    R(x+inset, y, w-2*inset, h)                     # singles sidelines
    # service line + centre service line (court runs along length = h)
    sl1 = y + h*0.22; sl2 = y + h*0.78
    L(x+inset, sl1, x+w-inset, sl1)
    L(x+inset, sl2, x+w-inset, sl2)
    L(x+w/2, sl1, x+w/2, sl2)                       # centre service line
    L(x, y+h/2, x+w, y+h/2, min(1.0, op+0.1))       # net line
    # centre marks
    L(x+w/2, y, x+w/2, y+h*0.03)
    L(x+w/2, y+h, x+w/2, y+h*0.97)
    return "".join(lines)

# ----------------------------------------------------------------------------
# VIEW: Left / Right side profile of the P217
# ----------------------------------------------------------------------------
def side_view(ox, oy, sc, mirror=False, label=""):
    """Stylised but proportionally faithful LMP2 (P217) side silhouette.
    Local units ~ millimetres/10. Car ~ length 465 x height ~115 (local)."""
    # Both sides drawn nose-left so all numbers/wordmarks stay readable (the
    # livery is symmetric); the portrait is flipped between sides so each
    # sidepod reads correctly toward the nose.
    g = [f'<g transform="translate({ox},{oy}) scale({sc},{sc})">']
    cid = "rclip" if mirror else "lclip"

    # ---- body outline (nose at left ~x0, tail at right ~x465) ----
    body = (
        "M 6 86 "                       # splitter front tip (ground)
        "L 2 78 "                       # nose lower
        "C 0 60 8 50 34 48 "            # low nose rise
        "C 70 46 96 47 120 44 "        # front deck to fender top
        "C 134 30 150 26 168 28 "      # windscreen base -> A pillar
        "C 184 18 206 14 224 18 "      # roof / canopy
        "C 246 12 262 18 270 30 "      # airbox / roof intake peak
        "C 286 26 300 28 318 30 "      # engine cover deck
        "C 360 30 396 32 430 40 "      # cover to tail top
        "L 462 40 "                     # rear deck edge -> shark-fin/wing area
        "L 462 70 "                     # tail vertical
        "C 450 74 440 74 430 74 "      # rear diffuser exit
        "L 300 76 "                     # underfloor
        "C 250 80 210 82 170 84 "
        "L 120 86 "
        "L 6 86 Z"
    )
    g.append(f'<path d="{body}" fill="{NAVY}" stroke="{NAVY_DK}" stroke-width="1.2"/>')

    # shading: lower body darker
    g.append(f'<path d="M 6 86 L 120 86 L 170 84 C 210 82 250 80 300 76 L 430 74 C 440 74 450 74 462 70 L 462 80 L 6 90 Z" fill="{NAVY_DK}" opacity="0.6"/>')
    # upper highlight sweep
    g.append(f'<path d="M 40 49 C 120 45 220 16 270 31 C 320 30 400 33 460 41 L 460 47 C 380 38 300 36 260 40 C 210 24 120 50 44 54 Z" fill="{NAVY_HI}" opacity="0.5"/>')

    # ---- canopy / cockpit glass ----
    g.append(f'<path d="M 150 30 C 170 18 205 15 226 20 C 236 24 238 30 232 33 L 168 35 C 156 35 150 33 150 30 Z" fill="#0a1424" stroke="{NAVY_DK}" stroke-width="0.8" opacity="0.92"/>')
    g.append(f'<path d="M 158 30 C 176 22 200 20 218 23" fill="none" stroke="{WHITE}" stroke-width="0.8" opacity="0.3"/>')

    # ---- wheels (covered, with arches cut) ----
    for wx in (66, 392):
        g.append(f'<path d="M {wx-34} 86 A 34 34 0 0 1 {wx+34} 86 Z" fill="{BG}" opacity="0.0"/>')
        g.append(f'<circle cx="{wx}" cy="86" r="33" fill="#121212" stroke="#000" stroke-width="1.5"/>')
        g.append(f'<circle cx="{wx}" cy="86" r="14" fill="#2a2d31"/>')
        g.append(f'<circle cx="{wx}" cy="86" r="4.5" fill="{SILVER}"/>')
        for k in range(6):
            a = math.radians(k*60)
            g.append(f'<line x1="{wx}" y1="86" x2="{wx+12*math.cos(a):.1f}" y2="{86+12*math.sin(a):.1f}" stroke="#3a3e44" stroke-width="2"/>')
        # arch outline
        g.append(f'<path d="M {wx-36} 86 A 36 36 0 0 1 {wx+36} 86" fill="none" stroke="{NAVY_DK}" stroke-width="2"/>')

    # ---- front splitter + dive planes ----
    g.append(f'<path d="M 2 78 L 8 88 L 40 89 L 36 80 Z" fill="{NAVY_DK}"/>')
    g.append(f'<rect x="14" y="66" width="26" height="3" rx="1.5" fill="{RED}"/>')

    # ---- shark fin (rear, tall) with Grand Slam references ----
    fin = "M 360 31 L 462 40 L 462 14 C 430 14 392 22 360 31 Z"
    g.append(f'<path d="{fin}" fill="{NAVY}" stroke="{NAVY_DK}" stroke-width="1"/>')
    g.append(f'<path d="{fin}" fill="{NAVY_HI}" opacity="0.3"/>')
    # red leading edge on fin
    g.append(f'<path d="M 360 31 C 392 22 430 14 462 14" fill="none" stroke="{RED}" stroke-width="2.4"/>')
    # GS year tally on fin (Grand Slam reference)
    gs = [("AO","10"),("RG","3"),("W","7"),("US","4")]
    fx = 392
    for i,(t,n) in enumerate(gs):
        yy = 20 + i*4.4
        g.append(f'<text x="{fx}" y="{yy}" font-family="IBM Plex Mono, monospace" font-size="3.4" fill="{WHITE}" font-weight="700">{t}</text>')
        g.append(f'<text x="{fx+34}" y="{yy}" font-family="IBM Plex Mono, monospace" font-size="3.4" fill="{BALL}" font-weight="700" text-anchor="end">{n}</text>')

    # ---- rear wing ----
    g.append(f'<rect x="436" y="20" width="30" height="4" rx="1" fill="#0a1424" stroke="{NAVY_DK}" stroke-width="0.6"/>')
    g.append(f'<rect x="462" y="14" width="5" height="30" fill="{NAVY_DK}"/>')   # endplate
    g.append(serbian_flag(463, 16, 4, 9))

    # ============ LIVERY GRAPHICS =============
    # Tennis-ball trajectory sweep flowing nose->tail
    g.append(tennis_traj(20, 60, 250, 44, -26, color=BALL, w=3.2, op=0.95))
    g.append(tennis_traj(120, 60, 430, 50, -20, color=BALL_DK, w=2.0, op=0.8, dash="6 5"))
    g.append(tennis_traj(60, 70, 360, 64, 14, color=RED, w=2.4, op=0.85))

    # Red accent band along lower flank
    g.append(f'<path d="M 40 72 C 160 70 300 70 432 70 L 432 76 C 300 76 160 76 40 78 Z" fill="{RED}" opacity="0.95"/>')
    g.append(f'<path d="M 40 78 C 160 77 300 77 432 76 L 432 78 C 300 79 160 79 40 80 Z" fill="{WHITE}" opacity="0.85"/>')

    # White door panel hosting the BIG number 24
    g.append(f'<path d="M 180 44 C 230 40 300 42 330 50 L 326 70 C 280 70 220 70 184 70 Z" fill="{WHITE}" opacity="0.96"/>')
    g.append(f'<text x="256" y="68" font-family="IBM Plex Sans, Arial, sans-serif" font-size="34" font-weight="800" fill="{NAVY}" text-anchor="middle" font-style="italic">24</text>')
    # tiny "GRAND SLAMS" under number
    g.append(f'<text x="256" y="72.5" font-family="IBM Plex Mono, monospace" font-size="3.0" fill="{RED}" text-anchor="middle" font-weight="700" letter-spacing="0.5">24 GRAND SLAMS</text>')

    # Portrait blended into sidepod (ahead of rear wheel, behind door)
    g.append(f'<g transform="translate(338,55) scale(0.34)">{portrait(0,0,1.0,flip=mirror,clip_id=cid)}</g>')

    # Tennis court strip across engine cover (visible sliver on side)
    g.append(court(300, 31, 56, 7, op=0.5, sw=0.7))

    # 4 GS trophies clean on upper front deck
    for i,(lab) in enumerate(["AO","RG","W","US"]):
        g.append(trophy(96 + i*16, 41, 4.4, label=None))
    # mini sponsor wordmarks (authentic placement: nose, sidepod, engine cover)
    g.append(f'<text x="78" y="40" font-family="IBM Plex Sans, Arial" font-size="6" font-weight="800" fill="{WHITE}" text-anchor="middle">LACOSTE</text>')
    g.append(f'<text x="150" y="60" font-family="IBM Plex Sans, Arial" font-size="6.5" font-weight="800" fill="{NAVY}" text-anchor="middle">HEAD</text>')
    g.append(f'<text x="392" y="60" font-family="IBM Plex Sans, Arial" font-size="5" font-weight="700" fill="{WHITE}" text-anchor="middle">HUBLOT</text>')
    g.append(f'<text x="350" y="38" font-family="IBM Plex Sans, Arial" font-size="3.6" font-weight="700" fill="{INK}" text-anchor="middle">WATERDROP</text>')
    g.append(f'<text x="256" y="46" font-family="IBM Plex Sans, Arial" font-size="3.4" font-weight="700" fill="{NAVY}" text-anchor="middle">ATP TOUR</text>')

    # Mirror with Serbian flag
    g.append(f'<g transform="rotate({-8 if not mirror else -8} 176 30)">{serbian_flag(170, 24, 9, 5)}</g>')

    # 24 stars scattered subtly across navy upper bodywork
    rnd = random.Random(7)
    placed = 0
    spots = [(58,42),(86,38),(118,38),(300,34),(330,36),(356,35),(384,34),(410,36),
             (436,36),(146,32),(248,40),(276,38),(312,42),(70,52),(420,52),(390,46),
             (350,46),(330,44),(110,50),(132,46),(160,42),(208,40),(230,38),(456,32)]
    for (sx,sy) in spots[:24]:
        g.append(star(sx, sy, 2.0, fill=WHITE, opacity=0.5))
        placed += 1

    g.append('</g>')
    # label
    g.append(f'<text x="{ox}" y="{oy+sc*100+30}" font-family="IBM Plex Mono, monospace" font-size="22" fill="{INK_DIM}" font-weight="600" letter-spacing="2">{label}</text>')
    return "".join(g)

# ----------------------------------------------------------------------------
# VIEW: Top (plan) — the hero for hood/roof/engine-cover court graphics
# ----------------------------------------------------------------------------
def top_view(ox, oy, sc, label=""):
    g = [f'<g transform="translate({ox},{oy}) scale({sc})">']
    # plan outline: length along x (0..465), width along y (centre 0, +-95)
    g.append('<g transform="translate(0,95)">')
    outline = (
        "M 8 0 "
        "C 8 -14 14 -22 40 -26 "         # nose taper (top half)
        "C 80 -34 110 -40 140 -52 "      # front fender bulge
        "C 150 -64 150 -78 150 -86 "
        "L 168 -90 "
        "C 210 -94 250 -94 300 -90 "     # body sides
        "C 360 -86 410 -80 452 -70 "     # rear fender
        "L 462 -54 "
        "L 462 54 "                       # tail width
        "C 410 80 360 86 300 90 "
        "C 250 94 210 94 168 90 "
        "L 150 86 "
        "C 150 78 150 64 140 52 "
        "C 110 40 80 34 40 26 "
        "C 14 22 8 14 8 0 Z"
    )
    g.append(f'<path d="{outline}" fill="{NAVY}" stroke="{NAVY_DK}" stroke-width="1.4"/>')
    # central spine highlight
    g.append(f'<path d="M 20 0 C 120 0 360 0 460 0" stroke="{NAVY_HI}" stroke-width="30" opacity="0.35" fill="none"/>')

    # cockpit canopy (centre)
    g.append(f'<ellipse cx="200" cy="0" rx="46" ry="30" fill="#0a1424" stroke="{NAVY_DK}" stroke-width="1"/>')
    g.append(f'<ellipse cx="200" cy="0" rx="46" ry="30" fill="none" stroke="{WHITE}" stroke-width="0.6" opacity="0.25"/>')

    # wheels (4) as dark rounded rects
    for (wx,wy) in [(66,-78),(66,78),(392,-78),(392,78)]:
        g.append(f'<rect x="{wx-30}" y="{wy-16}" width="60" height="32" rx="8" fill="#121212" stroke="#000" stroke-width="1.2"/>')

    # ===== Tennis court graphics on hood (front), roof gap, engine cover (rear) =====
    # Hood court
    g.append(f'<g transform="translate(40,-44) rotate(0)">{court(0,0,80,44,op=0.9,sw=1.4)}</g>')
    # Engine cover court (rear deck, larger)
    g.append(f'<g transform="translate(262,-46)">{court(0,0,170,92,op=0.85,sw=1.6)}</g>')
    # central red service line running the spine (ball trajectory tie-in)
    g.append(tennis_traj(20,0,460,0,0,color=RED,w=4,op=0.9))
    g.append(tennis_traj(120,-6,440,-30,-40,color=BALL,w=3,op=0.9))
    g.append(tennis_traj(120,6,440,30,40,color=BALL_DK,w=2.4,op=0.8,dash="7 5"))

    # White wings front & rear with number echoes
    g.append(f'<rect x="2" y="-30" width="10" height="60" rx="3" fill="{NAVY_DK}"/>')   # front wing
    g.append(f'<rect x="458" y="-58" width="8" height="116" rx="2" fill="{NAVY_DK}"/>')  # rear wing
    g.append(serbian_flag(459,-58,7,12))
    g.append(serbian_flag(459,46,7,12))

    # Big 24 on nose (visible top)
    g.append(f'<text x="80" y="6" font-family="IBM Plex Sans, Arial" font-size="40" font-weight="800" fill="{WHITE}" text-anchor="middle" font-style="italic">24</text>')
    # roof 24 (on canopy surround)
    g.append(f'<text x="200" y="-40" font-family="IBM Plex Sans, Arial" font-size="14" font-weight="800" fill="{WHITE}" text-anchor="middle">NOLE</text>')

    # 4 trophies clean on rear deck corners
    for i,lab in enumerate(["AO","RG","W","US"]):
        g.append(trophy(300+i*38, -70, 6, label=lab))

    # Sponsors authentic placement (engine cover + nose)
    g.append(f'<text x="350" y="6" font-family="IBM Plex Sans, Arial" font-size="13" font-weight="800" fill="{WHITE}" text-anchor="middle">LACOSTE</text>')
    g.append(f'<text x="200" y="40" font-family="IBM Plex Sans, Arial" font-size="9" font-weight="800" fill="{WHITE}" text-anchor="middle">HEAD</text>')

    # stars along centre
    for i in range(8):
        g.append(star(150+i*18, -2, 2.2, fill=WHITE, opacity=0.4))

    g.append('</g>')  # translate
    g.append('</g>')  # scale
    g.append(f'<text x="{ox}" y="{oy+sc*200+28}" font-family="IBM Plex Mono, monospace" font-size="22" fill="{INK_DIM}" font-weight="600" letter-spacing="2">{label}</text>')
    return "".join(g)

# ----------------------------------------------------------------------------
# VIEW: Front
# ----------------------------------------------------------------------------
def front_view(ox, oy, sc, label=""):
    g = [f'<g transform="translate({ox},{oy}) scale({sc})">']
    g.append('<g transform="translate(95,0)">')
    # nose body (width -95..95, height 0..86)
    body = (
        "M -95 70 L -95 58 C -95 48 -86 44 -70 42 "
        "C -50 30 -30 24 0 24 C 30 24 50 30 70 42 "
        "C 86 44 95 48 95 58 L 95 70 "
        "C 60 76 40 78 0 78 C -40 78 -60 76 -95 70 Z"
    )
    g.append(f'<path d="{body}" fill="{NAVY}" stroke="{NAVY_DK}" stroke-width="1.2"/>')
    g.append(f'<path d="{body}" fill="{NAVY_HI}" opacity="0.25"/>')
    # canopy
    g.append(f'<path d="M -34 26 C -20 14 20 14 34 26 C 24 22 -24 22 -34 26 Z" fill="#0a1424" stroke="{NAVY_DK}" stroke-width="0.8"/>')
    g.append(f'<ellipse cx="0" cy="20" rx="30" ry="10" fill="#0a1424" stroke="{NAVY_DK}" stroke-width="0.8"/>')
    # front wing + splitter
    g.append(f'<rect x="-92" y="78" width="184" height="6" rx="2" fill="{NAVY_DK}"/>')
    g.append(f'<rect x="-92" y="78" width="184" height="2.4" fill="{RED}"/>')
    g.append(f'<rect x="-50" y="84" width="100" height="3" rx="1" fill="#0a1424"/>')
    # dive planes
    g.append(f'<path d="M -95 60 L -108 64 L -95 68 Z" fill="{NAVY_DK}"/>')
    g.append(f'<path d="M 95 60 L 108 64 L 95 68 Z" fill="{NAVY_DK}"/>')
    # wheels peeking
    g.append(f'<rect x="-112" y="48" width="20" height="34" rx="5" fill="#121212"/>')
    g.append(f'<rect x="92" y="48" width="20" height="34" rx="5" fill="#121212"/>')

    # livery: tennis court on hood front
    g.append(f'<g transform="translate(-40,30)">{court(0,0,80,14,op=0.85,sw=1.2)}</g>')
    # red + white chevron under nose
    g.append(f'<path d="M -70 44 L 0 50 L 70 44 L 70 50 L 0 56 L -70 50 Z" fill="{RED}"/>')
    g.append(f'<path d="M -70 50 L 0 56 L 70 50 L 70 52.5 L 0 58 L -70 52.5 Z" fill="{WHITE}"/>')
    # ball trajectory arcs
    g.append(tennis_traj(-80,60,80,60,-12,color=BALL,w=2.6,op=0.9))
    # big 24 centred on nose
    g.append(f'<text x="0" y="68" font-family="IBM Plex Sans, Arial" font-size="22" font-weight="800" fill="{WHITE}" text-anchor="middle" font-style="italic">24</text>')
    # sponsor on nose tip
    g.append(f'<text x="0" y="40" font-family="IBM Plex Sans, Arial" font-size="6" font-weight="800" fill="{WHITE}" text-anchor="middle">LACOSTE</text>')
    # Serbian flags on mirrors
    g.append(serbian_flag(-44,28,7,4))
    g.append(serbian_flag(37,28,7,4))
    # stars
    for sx in (-78,-66,66,78):
        g.append(star(sx,52,2.0,opacity=0.45))
    g.append('</g>')
    g.append('</g>')
    g.append(f'<text x="{ox}" y="{oy+sc*92+26}" font-family="IBM Plex Mono, monospace" font-size="22" fill="{INK_DIM}" font-weight="600" letter-spacing="2">{label}</text>')
    return "".join(g)

# ----------------------------------------------------------------------------
# VIEW: Rear
# ----------------------------------------------------------------------------
def rear_view(ox, oy, sc, label=""):
    g = [f'<g transform="translate({ox},{oy}) scale({sc})">']
    g.append('<g transform="translate(95,0)">')
    body = (
        "M -95 70 L -95 50 C -95 42 -88 38 -74 36 "
        "C -50 30 -28 30 0 30 C 28 30 50 30 74 36 "
        "C 88 38 95 42 95 50 L 95 70 "
        "C 60 76 40 78 0 78 C -40 78 -60 76 -95 70 Z"
    )
    g.append(f'<path d="{body}" fill="{NAVY}" stroke="{NAVY_DK}" stroke-width="1.2"/>')
    g.append(f'<path d="{body}" fill="{NAVY_DK}" opacity="0.4"/>')
    # diffuser
    g.append(f'<rect x="-70" y="70" width="140" height="14" rx="2" fill="#0a1018"/>')
    for k in range(7):
        xx = -64 + k*18
        g.append(f'<line x1="{xx}" y1="70" x2="{xx}" y2="84" stroke="#2a2d31" stroke-width="1.6"/>')
    # rear wing high
    g.append(f'<rect x="-86" y="6" width="172" height="7" rx="2" fill="#0a1424" stroke="{NAVY_DK}" stroke-width="0.6"/>')
    g.append(f'<rect x="-86" y="6" width="172" height="2.6" fill="{RED}"/>')
    g.append(f'<rect x="-86" y="6" width="6" height="44" fill="{NAVY_DK}"/>')   # endplates
    g.append(f'<rect x="80" y="6" width="6" height="44" fill="{NAVY_DK}"/>')
    g.append(serbian_flag(-86,14,6,12))
    g.append(serbian_flag(80,14,6,12))
    # shark fin base
    g.append(f'<path d="M -3 12 L 3 12 L 2 36 L -2 36 Z" fill="{NAVY}" stroke="{NAVY_DK}" stroke-width="0.6"/>')
    # tail lights
    g.append(f'<rect x="-60" y="44" width="14" height="6" rx="2" fill="{RED}"/>')
    g.append(f'<rect x="46" y="44" width="14" height="6" rx="2" fill="{RED}"/>')
    g.append(f'<circle cx="0" cy="60" r="4" fill="{RED}"/>')  # rain light
    # wheels
    g.append(f'<rect x="-112" y="46" width="20" height="36" rx="5" fill="#121212"/>')
    g.append(f'<rect x="92" y="46" width="20" height="36" rx="5" fill="#121212"/>')

    # livery: engine cover court sliver + trajectories
    g.append(f'<g transform="translate(-40,32)">{court(0,0,80,12,op=0.7,sw=1.0)}</g>')
    g.append(tennis_traj(-80,40,80,40,-10,color=BALL,w=2.4,op=0.9))
    g.append(f'<path d="M -74 56 L 0 60 L 74 56 L 74 60 L 0 64 L -74 60 Z" fill="{RED}"/>')
    # big 24 on tail
    g.append(f'<text x="0" y="58" font-family="IBM Plex Sans, Arial" font-size="20" font-weight="800" fill="{WHITE}" text-anchor="middle" font-style="italic">24</text>')
    # GRAND SLAM tally on rear bodywork
    g.append(f'<text x="0" y="29" font-family="IBM Plex Mono, monospace" font-size="6" font-weight="800" fill="{WHITE}" text-anchor="middle" letter-spacing="1">24 GRAND SLAMS</text>')
    # sponsors authentic rear placement
    g.append(f'<text x="0" y="40" font-family="IBM Plex Sans, Arial" font-size="8" font-weight="800" fill="{WHITE}" text-anchor="middle">HUBLOT</text>')
    g.append(f'<text x="-44" y="50" font-family="IBM Plex Sans, Arial" font-size="4.5" font-weight="700" fill="{INK}" text-anchor="middle">WATERDROP</text>')
    g.append(f'<text x="44" y="50" font-family="IBM Plex Sans, Arial" font-size="4.5" font-weight="700" fill="{INK}" text-anchor="middle">ATP TOUR</text>')
    # 4 trophies on wing mainplane
    for i,lab in enumerate(["AO","RG","W","US"]):
        g.append(trophy(-42+i*28, 2, 3.4))
    # stars
    for sx in (-78,-66,66,78):
        g.append(star(sx,52,2.0,opacity=0.45))
    g.append('</g>')
    g.append('</g>')
    g.append(f'<text x="{ox}" y="{oy+sc*92+26}" font-family="IBM Plex Mono, monospace" font-size="22" fill="{INK_DIM}" font-weight="600" letter-spacing="2">{label}</text>')
    return "".join(g)

# ----------------------------------------------------------------------------
# Footer: palette + sponsor/legend
# ----------------------------------------------------------------------------
def swatch(x, y, col, name, hexv):
    return (f'<rect x="{x}" y="{y}" width="46" height="46" rx="6" fill="{col}" stroke="#000" stroke-width="0.5"/>'
            f'<text x="{x+56}" y="{y+19}" font-family="IBM Plex Sans, Arial" font-size="16" fill="{INK}" font-weight="700">{name}</text>'
            f'<text x="{x+56}" y="{y+38}" font-family="IBM Plex Mono, monospace" font-size="14" fill="{INK_DIM}">{hexv}</text>')

# ----------------------------------------------------------------------------
# Compose document
# ----------------------------------------------------------------------------
def build():
    s = []
    s.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="IBM Plex Sans, Arial, sans-serif">')
    # defs
    s.append('<defs>')
    s.append(f'<radialGradient id="bg" cx="50%" cy="32%" r="80%">'
             f'<stop offset="0%" stop-color="{BG_GRAD_T}"/>'
             f'<stop offset="100%" stop-color="{BG_GRAD_B}"/></radialGradient>')
    s.append(f'<linearGradient id="floor" x1="0" y1="0" x2="0" y2="1">'
             f'<stop offset="0%" stop-color="#1d2127"/><stop offset="100%" stop-color="#0a0c0f"/></linearGradient>')
    s.append('</defs>')
    s.append(f'<rect width="{W}" height="{H}" fill="url(#bg)"/>')

    # ---- header ----
    s.append(f'<rect x="0" y="0" width="{W}" height="150" fill="{NAVY_DK}"/>')
    s.append(f'<rect x="0" y="146" width="{W}" height="4" fill="{RED}"/>')
    s.append(serbian_flag(60, 48, 70, 54))
    s.append(f'<text x="150" y="74" font-family="IBM Plex Sans, Arial" font-size="38" font-weight="800" fill="{WHITE}">DALLARA P217 · LMP2</text>')
    s.append(f'<text x="150" y="112" font-family="IBM Plex Sans, Arial" font-size="24" font-weight="600" fill="{BALL}">“NOLE 24” — NOVAK DJOKOVIC CAREER TRIBUTE LIVERY</text>')
    s.append(f'<text x="{W-60}" y="66" font-family="IBM Plex Sans, Arial" font-size="64" font-weight="800" fill="{WHITE}" text-anchor="end" font-style="italic">24</text>')
    s.append(f'<text x="{W-60}" y="100" font-family="IBM Plex Mono, monospace" font-size="15" fill="{BALL}" text-anchor="end">GRAND SLAM TITLES</text>')
    s.append(f'<text x="{W-60}" y="124" font-family="IBM Plex Mono, monospace" font-size="13" fill="{INK_DIM}" text-anchor="end">CONCEPT SHEET · TRADING PAINTS REF · STUDIO LIGHTING</text>')

    # ---- studio floor plates behind each view ----
    def plate(x,y,w,h):
        s.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="url(#floor)" stroke="#2a2f36" stroke-width="1"/>')

    # Hero: LEFT side
    plate(60, 180, W-120, 360)
    s.append(side_view(110, 230, 3.8, mirror=False, label="LEFT SIDE PROFILE"))

    # RIGHT side
    plate(60, 580, W-120, 360)
    s.append(side_view(110, 630, 3.8, mirror=True, label="RIGHT SIDE PROFILE"))

    # TOP / plan
    plate(60, 980, W-120, 470)
    s.append(top_view(150, 1010, 3.8, label="TOP / PLAN VIEW"))

    # FRONT + REAR
    plate(60, 1490, (W-150)/2-25, 430)
    s.append(front_view(230, 1540, 4.2, label="FRONT VIEW"))
    plate(60+(W-150)/2+35, 1490, (W-150)/2-25, 430)
    s.append(rear_view(1230, 1540, 4.2, label="REAR VIEW"))

    # ---- footer panels ----
    fy = 1960
    s.append(f'<rect x="60" y="{fy}" width="{W-120}" height="640" rx="14" fill="{PANEL}" stroke="#2a2f36" stroke-width="1"/>')
    s.append(f'<text x="92" y="{fy+44}" font-family="IBM Plex Sans, Arial" font-size="22" font-weight="800" fill="{WHITE}">COLOUR SPEC</text>')
    pal = [(NAVY,"Serbian Navy",NAVY),(NAVY_DK,"Shadow Navy",NAVY_DK),(WHITE_PUR,"Race White","#FFFFFF"),
           (RED,"Serbian Red",RED),(BALL,"Optic Ball","#D7F25B"),(GOLD,"Slam Gold",GOLD)]
    for i,(c,n,hx) in enumerate(pal):
        s.append(swatch(92, fy+70+i*60, c, n, hx))

    # sponsor placement legend
    lx = 560
    s.append(f'<text x="{lx}" y="{fy+44}" font-family="IBM Plex Sans, Arial" font-size="22" font-weight="800" fill="{WHITE}">SPONSOR PLACEMENT (authentic — wordmark refs, no logos faked)</text>')
    legend = [
        ("LACOSTE","Title — engine cover spine + nose"),
        ("HEAD","Sidepod leading edge (both sides)"),
        ("HUBLOT","Rear deck / rear quarter"),
        ("WATERDROP","Lower sidepod + rear bumper"),
        ("ATP TOUR","Cockpit surround + door secondary"),
    ]
    for i,(n,loc) in enumerate(legend):
        yy = fy+78+i*46
        s.append(f'<rect x="{lx}" y="{yy-20}" width="150" height="32" rx="5" fill="{NAVY}" stroke="{RED}" stroke-width="1"/>')
        s.append(f'<text x="{lx+75}" y="{yy+2}" font-family="IBM Plex Sans, Arial" font-size="16" font-weight="800" fill="{WHITE}" text-anchor="middle">{n}</text>')
        s.append(f'<text x="{lx+170}" y="{yy+2}" font-family="IBM Plex Sans, Arial" font-size="15" fill="{INK_DIM}">{loc}</text>')

    # design elements checklist
    ex = 560; ey = fy+330
    s.append(f'<text x="{ex}" y="{ey}" font-family="IBM Plex Sans, Arial" font-size="22" font-weight="800" fill="{WHITE}">DESIGN ELEMENTS</text>')
    items = [
        "Race #24 — 24 Grand Slam titles (doors, nose, roof, tail)",
        "Tennis-court line graphics — hood, roof, engine cover",
        "Monochrome Djokovic profile — blended into both sidepods",
        "Serbian tricolour — mirrors, wing endplates, accents",
        "Grand Slam refs (AO·RG·W·US tally) — shark fin &amp; rear",
        "Four GS trophies — clean on upper bodywork",
        "24 stars — integrated across navy panels",
        "Tennis-ball trajectory sweeps — optic-yellow flow lines",
    ]
    for i,t in enumerate(items):
        yy = ey+30+i*34
        s.append(f'<circle cx="{ex+8}" cy="{yy-5}" r="4" fill="{BALL}"/>')
        s.append(f'<text x="{ex+24}" y="{yy}" font-family="IBM Plex Sans, Arial" font-size="15.5" fill="{INK}">{t}</text>')

    s.append(f'<text x="{W/2}" y="{H-24}" font-family="IBM Plex Mono, monospace" font-size="13" fill="{INK_DIM}" text-anchor="middle">Vector concept sheet · WEC / Le Mans prototype styling · proportions schematic for livery mapping · not an official Djokovic / sponsor product</text>')

    s.append('</svg>')
    return "".join(s)

if __name__ == "__main__":
    svg = build()
    with open("djokovic_p217_livery_sheet.svg","w") as f:
        f.write(svg)
    print("SVG written:", len(svg), "bytes")
    try:
        import cairosvg
        cairosvg.svg2png(bytestring=svg.encode(), write_to="djokovic_p217_livery_sheet.png",
                         output_width=2000, output_height=2680)
        print("PNG written")
    except Exception as e:
        print("PNG render failed:", e)
