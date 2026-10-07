"""Harvana illustrations: warm, textured flat scenes. Writes one SVG per scene."""
import math, os
SK=['#8D5524','#C68642','#6B3E26','#A8653A','#E0AC69','#5A3825']
HAIR='#2B1B12'
INK='#053D3A'; TEAL='#0B6B61'; GOLD='#F4B63D'; CORAL='#F27C5C'; CREAM='#FFF4E2'; LEAF='#5E8C4A'; OLIVE='#7A8450'
def coin(x,y,r=14):
    return (f'<g><ellipse cx="{x}" cy="{y+r*0.18}" rx="{r}" ry="{r}" fill="#C27A12"/>'
            f'<circle cx="{x}" cy="{y}" r="{r}" fill="{GOLD}"/><circle cx="{x}" cy="{y}" r="{r*0.72}" fill="none" stroke="#FFE39A" stroke-width="{r*0.12}"/>'
            f'<text x="{x}" y="{y+r*0.36}" font-family="Georgia,serif" font-weight="700" font-size="{r*1.0}" text-anchor="middle" fill="#9A5A06">H</text></g>')
def person(x,feet,h=170,skin=0,shirt=TEAL,pants='#3B4A3A',hair='short',arms='down',dress=False,beard=False,grey=False,face=1):
    s=h/170.0; S=SK[skin%len(SK)]; hc='#E8E2D6' if grey else HAIR
    head_r=17*s; hy=feet-h+head_r; neck=hy+head_r
    tw=46*s; th=62*s; ty=neck+4*s; leg=feet-(ty+th)
    out=[]
    # legs / skirt
    if dress:
        out.append(f'<path d="M{x-tw/2-4*s} {ty+th*0.55} L{x+tw/2+4*s} {ty+th*0.55} L{x+tw/2+14*s} {feet-14*s} L{x-tw/2-14*s} {feet-14*s}Z" fill="{pants}"/>')
        out.append(f'<rect x="{x-12*s}" y="{feet-16*s}" width="7*s" height="14" fill="{S}"/>'.replace('7*s',str(7*s)))
        out.append(f'<rect x="{x+5*s}" y="{feet-16*s}" width="{7*s}" height="{14*s}" fill="{S}"/>')
        out.append(f'<rect x="{x-12*s}" y="{feet-16*s}" width="{7*s}" height="{14*s}" fill="{S}"/>')
    else:
        out.append(f'<rect x="{x-tw/2+3*s}" y="{ty+th-6*s}" width="{tw/2-4*s}" height="{leg+6*s}" rx="{5*s}" fill="{pants}"/>')
        out.append(f'<rect x="{x+1*s}" y="{ty+th-6*s}" width="{tw/2-4*s}" height="{leg+6*s}" rx="{5*s}" fill="{pants}"/>')
    out.append(f'<ellipse cx="{x-tw/4}" cy="{feet}" rx="{11*s}" ry="{4.5*s}" fill="#2A2420"/><ellipse cx="{x+tw/4}" cy="{feet}" rx="{11*s}" ry="{4.5*s}" fill="#2A2420"/>')
    # arms
    def arm(side,pose):
        sx=x+side*(tw/2-4*s); sy=ty+10*s
        if pose=='down': ex,ey=sx+side*6*s, sy+52*s
        elif pose=='up': ex,ey=sx+side*14*s, sy-48*s
        elif pose=='front': ex,ey=sx+side*30*s, sy+26*s
        elif pose=='give': ex,ey=sx+side*44*s, sy+14*s
        elif pose=='hug': ex,ey=sx+side*-6*s, sy+40*s
        return f'<path d="M{sx} {sy} Q{(sx+ex)/2+side*8*s} {(sy+ey)/2} {ex} {ey}" stroke="{shirt}" stroke-width="{13*s}" fill="none" stroke-linecap="round"/><circle cx="{ex}" cy="{ey}" r="{6.5*s}" fill="{S}"/>', (ex,ey)
    a=arms if isinstance(arms,tuple) else (arms,arms)
    L,lh=arm(-1,a[0]); R,rh=arm(1,a[1])
    out.append(f'<rect x="{x-tw/2}" y="{ty}" width="{tw}" height="{th}" rx="{16*s}" fill="{shirt}"/>')
    out.append(f'<path d="M{x-8*s} {ty} L{x} {ty+12*s} L{x+8*s} {ty}Z" fill="{S}"/>')
    out.append(L); out.append(R)
    out.append(f'<rect x="{x-5*s}" y="{neck-4*s}" width="{10*s}" height="{10*s}" fill="{S}"/>')
    out.append(f'<circle cx="{x}" cy="{hy}" r="{head_r}" fill="{S}"/>')
    if hair=='short': out.append(f'<path d="M{x-head_r} {hy-2*s} Q{x-head_r} {hy-head_r-8*s} {x} {hy-head_r-6*s} Q{x+head_r} {hy-head_r-8*s} {x+head_r} {hy-2*s} Q{x+head_r*0.6} {hy-head_r*0.7} {x} {hy-head_r*0.75} Q{x-head_r*0.6} {hy-head_r*0.7} {x-head_r} {hy-2*s}Z" fill="{hc}"/>')
    elif hair=='bun': out.append(f'<circle cx="{x}" cy="{hy-head_r-6*s}" r="{9*s}" fill="{hc}"/><path d="M{x-head_r-1} {hy+2*s} Q{x-head_r-2} {hy-head_r-4*s} {x} {hy-head_r-3*s} Q{x+head_r+2} {hy-head_r-4*s} {x+head_r+1} {hy+2*s} Q{x+head_r*0.5} {hy-head_r*0.6} {x} {hy-head_r*0.65} Q{x-head_r*0.5} {hy-head_r*0.6} {x-head_r-1} {hy+2*s}Z" fill="{hc}"/>')
    elif hair=='long': out.append(f'<path d="M{x-head_r-3*s} {hy+head_r*1.3} Q{x-head_r-6*s} {hy-head_r-6*s} {x} {hy-head_r-4*s} Q{x+head_r+6*s} {hy-head_r-6*s} {x+head_r+3*s} {hy+head_r*1.3} L{x+head_r*0.75} {hy+head_r*0.2} Q{x+head_r*0.5} {hy-head_r*0.55} {x} {hy-head_r*0.6} Q{x-head_r*0.5} {hy-head_r*0.55} {x-head_r*0.75} {hy+head_r*0.2}Z" fill="{hc}"/>')
    elif hair=='wrap': out.append(f'<path d="M{x-head_r-3*s} {hy-1*s} Q{x-head_r} {hy-head_r-16*s} {x+2*s} {hy-head_r-14*s} Q{x+head_r+6*s} {hy-head_r-12*s} {x+head_r+3*s} {hy-1*s}Z" fill="{CORAL}"/><path d="M{x-head_r+2*s} {hy-head_r*0.55} Q{x} {hy-head_r-2*s} {x+head_r-2*s} {hy-head_r*0.55}" stroke="{GOLD}" stroke-width="{2.5*s}" fill="none"/>')
    if beard: out.append(f'<path d="M{x-head_r*0.85} {hy+2*s} Q{x} {hy+head_r*1.45} {x+head_r*0.85} {hy+2*s} Q{x} {hy+head_r*0.75} {x-head_r*0.85} {hy+2*s}Z" fill="{hc}"/>')
    out.append(f'<circle cx="{x-6*s}" cy="{hy-1*s}" r="{1.9*s}" fill="#22160F"/><circle cx="{x+6*s}" cy="{hy-1*s}" r="{1.9*s}" fill="#22160F"/>')
    if face: out.append(f'<path d="M{x-5*s} {hy+7*s} Q{x} {hy+11*s} {x+5*s} {hy+7*s}" stroke="#6B2E1C" stroke-width="{1.8*s}" fill="none" stroke-linecap="round"/>')
    out.append(f'<circle cx="{x-9*s}" cy="{hy+5*s}" r="{3*s}" fill="#E7745A" opacity=".35"/><circle cx="{x+9*s}" cy="{hy+5*s}" r="{3*s}" fill="#E7745A" opacity=".35"/>')
    return '\n'.join(out), lh, rh
def plant(x,y,s=1):
    return (f'<g transform="translate({x},{y}) scale({s})"><path d="M-16 0 L16 0 L12 30 L-12 30Z" fill="#B86A3C"/><rect x="-18" y="-4" width="36" height="7" rx="3" fill="#C97B4A"/>'
            + ''.join(f'<path d="M0 -2 Q{dx*0.5} {-30-abs(dx)*0.3} {dx} {-46+abs(dx)*0.4}" stroke="{LEAF}" stroke-width="3" fill="none"/><ellipse cx="{dx}" cy="{-46+abs(dx)*0.4}" rx="9" ry="5" transform="rotate({dx*2} {dx} {-46+abs(dx)*0.4})" fill="{LEAF if dx%2 else "#76A35F"}"/>' for dx in (-22,-10,0,12,24))
            + '</g>')
def wrap(inner, w=720, h=480):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
<defs><filter id="grain" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency=".9" numOctaves="2" seed="4"/><feColorMatrix values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 .22 0"/><feComposite in2="SourceGraphic" operator="in"/></filter>
<filter id="soft"><feGaussianBlur stdDeviation="8"/></filter></defs>
<ellipse cx="{w/2}" cy="{h-34}" rx="{w*0.36}" ry="18" fill="#000" opacity=".18" filter="url(#soft)"/>
<g id="art"><circle cx="{w/2}" cy="{h/2+10}" r="{h*0.46}" fill="#F6E7C8" opacity=".14"/>{inner}</g>
<g filter="url(#grain)"><use href="#art"/></g>
</svg>'''
