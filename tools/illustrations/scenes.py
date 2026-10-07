from gen import *
F=440  # feet line
def P(*a,**k): return person(*a,**k)[0]
def jar(x,y,fill=0.6,w=90,h=110):
    top=y+h*(1-fill)
    return (f'<g><rect x="{x-w/2}" y="{y}" width="{w}" height="{h}" rx="18" fill="#DDEFEA" opacity=".55" stroke="#9CC9C0" stroke-width="4"/>'
            f'<clipPath id="j{x}"><rect x="{x-w/2}" y="{y}" width="{w}" height="{h}" rx="18"/></clipPath>'
            f'<g clip-path="url(#j{x})">'+''.join(coin(x-w/2+14+(i%4)*21, y+h-12-(i//4)*13, 11) for i in range(int(fill*28)))+'</g>'
            f'<rect x="{x-w/2-6}" y="{y-12}" width="{w+12}" height="16" rx="6" fill="{TEAL}"/></g>')
scenes={}
# 1 In short: the group together, sharing
p1,_,(rx,ry)=person(250,F,175,0,TEAL,'#33463F','short','down',beard=True)
s='\n'.join([plant(130,F-30,1.1),plant(600,F-30,1.0),
  P(300,F,190,5,'#2F5D50','#3B3B3B','short','hug',beard=True,grey=True),
  P(250,F,175,0,'#EFE6D2','#5B6B4A','short',('down','give'),beard=True),
  P(470,F,168,1,GOLD,CORAL,'long',('give','down'),dress=True),
  P(355,F,120,3,'#6E9A5B','#3E5C3A','short',('up','down')),
  P(410,F,115,2,'#3D7A6C',TEAL,'bun',('down','up'),dress=True),
  coin(360,F-150,15),coin(395,F-170,13),coin(330,F-178,11)])
scenes['contract_in_short']=s
# 2 About the group: circle of people around a table with a pot
s='\n'.join([f'<ellipse cx="360" cy="{F-50}" rx="190" ry="40" fill="#B67B47"/><rect x="190" y="{F-50}" width="18" height="50" fill="#8C5A31"/><rect x="512" y="{F-50}" width="18" height="50" fill="#8C5A31"/>',
  P(200,F-40,150,1,CORAL,'#444','wrap','front',dress=True),P(290,F-60,140,0,TEAL,'#444','short','front'),
  P(430,F-60,140,4,'#6E9A5B','#444','long','front',dress=True),P(520,F-40,150,5,'#EFE6D2','#444','short','front',beard=True),
  jar(360,F-170,0.55,80,95)])
scenes['contract_about']=s
# 3 Money rules: hands dropping coins in a jar, calendar
s='\n'.join([jar(360,170,0.45,150,190),
  P(170,F,175,2,TEAL,'#3B4A3A','short',('down','give')),P(550,F,170,4,CORAL,'#2F5D50','bun',('give','down'),dress=True),
  coin(305,140,14),coin(410,120,14),coin(360,95,12),
  '<g transform="translate(560,60)"><rect width="110" height="100" rx="12" fill="'+CREAM+'" stroke="#E2CFAE" stroke-width="3"/><rect width="110" height="26" rx="12" fill="'+CORAL+'"/>'
  +''.join(f'<circle cx="{18+(i%4)*25}" cy="{44+(i//4)*20}" r="6" fill="{TEAL if i<6 else "#D9CDB6"}"/>' for i in range(12))+'</g>'])
scenes['contract_money']=s
# 4 Loans: one person passes coins to another, handshake promise
s='\n'.join([P(250,F,175,0,TEAL,'#3B4A3A','short',('down','give'),beard=True),P(470,F,170,3,GOLD,CORAL,'long',('give','down'),dress=True),
  coin(360,F-170,16),coin(340,F-150,14),coin(382,F-146,13),
  f'<path d="M300 {F-235} Q360 {F-290} 420 {F-235}" stroke="{TEAL}" stroke-width="5" fill="none" stroke-dasharray="10 8" marker-end="url(#a)"/>',
  f'<path d="M420 {F-235} l-14 -4 l6 12z" fill="{TEAL}"/>',plant(620,F-30,0.9)])
scenes['contract_loans']=s
# 5 Decisions: people raising hands, ballot box with tick
s='\n'.join([f'<g transform="translate(300,{F-120})"><rect width="120" height="110" rx="10" fill="{TEAL}"/><rect x="30" y="-6" width="60" height="12" rx="4" fill="{INK}"/>'
  f'<path d="M58 -40 h34 v52 h-34z" fill="{CREAM}" transform="rotate(-8 75 -14)"/><path d="M40 55 l18 18 l34 -38" stroke="{CREAM}" stroke-width="10" fill="none" stroke-linecap="round" stroke-linejoin="round"/></g>',
  P(160,F,170,1,CORAL,'#3B3B3B','wrap',('down','up'),dress=True),P(240,F,160,5,'#6E9A5B','#3B4A3A','short',('up','down')),
  P(490,F,165,0,GOLD,'#3B4A3A','short',('down','up'),beard=True),P(570,F,155,4,'#3D7A6C',CORAL,'long',('up','down'),dress=True)])
scenes['contract_decisions']=s
# 6 Safe: big shield with lock over coins
s='\n'.join(['<path d="M360 70 L470 110 Q470 290 360 350 Q250 290 250 110Z" fill="'+TEAL+'"/><path d="M360 92 L450 124 Q448 276 360 326 Q272 276 270 124Z" fill="#0E8A7C"/>',
  '<rect x="318" y="190" width="84" height="70" rx="12" fill="'+GOLD+'"/><path d="M335 190 v-22 a25 25 0 0 1 50 0 v22" stroke="'+GOLD+'" stroke-width="12" fill="none"/><circle cx="360" cy="220" r="9" fill="#9A5A06"/><rect x="356" y="222" width="8" height="20" fill="#9A5A06"/>',
  ''.join(coin(250+i*30, F-18-(i%2)*8, 16) for i in range(8)),plant(140,F-30,1),plant(590,F-30,1.05)])
scenes['contract_safe']=s
# 7 Chat
def bubble(x,y,w,c,t=True): return f'<g><rect x="{x}" y="{y}" width="{w}" height="54" rx="20" fill="{c}"/><path d="M{x+(24 if t else w-24)} {y+52} l{-8 if t else 8} 18 l18 -18z" fill="{c}"/>'+''.join(f'<rect x="{x+18}" y="{y+14+i*14}" width="{w-36-i*30}" height="7" rx="3" fill="#fff" opacity=".85"/>' for i in range(2))+'</g>'
s='\n'.join([P(220,F,170,2,TEAL,'#3B4A3A','short','front'),P(500,F,165,4,CORAL,'#2F5D50','bun','front',dress=True),
  bubble(110,90,200,'#0E8A7C'),bubble(400,140,210,'#E2894B',False),bubble(170,200,150,'#5E8C4A')])
scenes['contract_chat']=s
# 8 Leave: open door, person walking with bag, waving
s='\n'.join([f'<rect x="440" y="{F-250}" width="140" height="250" rx="8" fill="#8C5A31"/><rect x="452" y="{F-238}" width="116" height="238" fill="#FBE7C2"/>',
  f'<path d="M452 {F-238} L520 {F-220} L520 {F+4} L452 {F}Z" fill="#B67B47"/>',
  P(300,F,175,0,'#6E9A5B','#3B4A3A','short',('up','front'),beard=True),
  f'<rect x="318" y="{F-95}" width="44" height="40" rx="8" fill="{GOLD}"/><path d="M328 {F-95} q12 -16 24 0" stroke="#9A5A06" stroke-width="4" fill="none"/>',plant(150,F-30,0.95)])
scenes['contract_leave']=s
# 9 Words explained: open book with a lightbulb
s='\n'.join([f'<path d="M180 330 Q270 290 360 330 L360 140 Q270 100 180 140Z" fill="{CREAM}" stroke="#E2CFAE" stroke-width="4"/><path d="M540 330 Q450 290 360 330 L360 140 Q450 100 540 140Z" fill="#FFF9EF" stroke="#E2CFAE" stroke-width="4"/>',
  ''.join(f'<rect x="{205}" y="{165+i*28}" width="{130-(i%2)*30}" height="8" rx="4" fill="#C9B79A"/><rect x="{385}" y="{165+i*28}" width="{130-((i+1)%2)*30}" height="8" rx="4" fill="#C9B79A"/>' for i in range(5)),
  f'<circle cx="360" cy="70" r="34" fill="{GOLD}"/><rect x="346" y="100" width="28" height="18" rx="5" fill="{TEAL}"/>'+''.join(f'<path d="M{360+math.cos(a)*46} {70+math.sin(a)*46} L{360+math.cos(a)*62} {70+math.sin(a)*62}" stroke="{GOLD}" stroke-width="6" stroke-linecap="round"/>' for a in [math.pi*(1+i/6) for i in range(7)])])
scenes['contract_words']=s
# 10 Goal: a target flag on a hill, jar filling
s='\n'.join([f'<path d="M80 {F} Q360 {F-180} 640 {F}Z" fill="#7FA864"/>',f'<rect x="356" y="{F-300}" width="8" height="140" fill="#5A3825"/><path d="M364 {F-300} l90 24 l-90 24z" fill="{CORAL}"/>',
  jar(210,F-150,0.8,80,100),jar(520,F-130,0.35,70,85),P(300,F,150,1,TEAL,'#3B4A3A','short',('down','up')),P(430,F,145,4,GOLD,'#2F5D50','long',('up','down'),dress=True)])
scenes['contract_goal']=s
# 11 Circle: people in a ring passing coins clockwise
pts=[(360+math.cos(a)*200, 300+math.sin(a)*55) for a in [math.pi*(0.5+2*i/6) for i in range(6)]]
order=sorted(range(6),key=lambda i:pts[i][1])
s=[f'<ellipse cx="360" cy="300" rx="200" ry="55" fill="none" stroke="{GOLD}" stroke-width="6" stroke-dasharray="14 12"/>']
shirts=[TEAL,CORAL,'#6E9A5B',GOLD,'#3D7A6C','#EFE6D2']; hairs=['short','wrap','short','long','bun','short']
for i in order:
    x,y=pts[i]; s.append(P(x,y+60,120,i,shirts[i],'#3B4A3A',hairs[i],'front',dress=hairs[i] in('long','bun','wrap')))
s.append(coin(360,250,20)); s.append(coin(560,300,12)); s.append(coin(160,300,12))
scenes['contract_circle']=''.join(s)
# 12 Savings: each person their own jar, growing
s='\n'.join([jar(200,F-130,0.3,70,90),jar(360,F-170,0.6,80,120),jar(520,F-210,0.9,90,160),
  f'<path d="M150 {F-250} Q360 {F-330} 560 {F-260}" stroke="{TEAL}" stroke-width="6" fill="none" stroke-linecap="round"/><path d="M560 {F-260} l-20 -12 l2 22z" fill="{TEAL}"/>',plant(630,F-30,0.9)])
scenes['contract_savings']=s
# 13 Example: notebook with sums and calculator
s='\n'.join([f'<rect x="180" y="90" width="260" height="320" rx="14" fill="{CREAM}" stroke="#E2CFAE" stroke-width="4"/>'+''.join(f'<circle cx="180" cy="{120+i*40}" r="7" fill="#C9B79A"/>' for i in range(8)),
  ''.join(f'<rect x="215" y="{130+i*36}" width="{150-(i*23)%60}" height="9" rx="4" fill="{"#C9B79A" if i<5 else TEAL}"/>' for i in range(7)),
  f'<rect x="470" y="190" width="130" height="190" rx="16" fill="{TEAL}"/><rect x="486" y="206" width="98" height="40" rx="6" fill="#DDEFEA"/>'+''.join(f'<rect x="{486+(i%3)*34}" y="{258+(i//3)*30}" width="26" height="22" rx="5" fill="{GOLD if i==11 else "#0E8A7C"}"/>' for i in range(12)),
  coin(560,120,18)])
scenes['contract_example']=s
# 14 Sign: hand with pen signing the contract
s='\n'.join([f'<rect x="200" y="70" width="280" height="350" rx="12" fill="#FFF9EF" stroke="#E2CFAE" stroke-width="4" transform="rotate(-4 340 245)"/>',
  ''.join(f'<rect x="235" y="{115+i*30}" width="{200-(i%3)*40}" height="8" rx="4" fill="#C9B79A" transform="rotate(-4 340 245)"/>' for i in range(7)),
  f'<path d="M250 360 q30 -40 50 -5 t50 -10 t45 0" stroke="{INK}" stroke-width="5" fill="none" stroke-linecap="round" transform="rotate(-4 340 245)"/>',
  f'<g transform="rotate(35 470 300)"><rect x="455" y="160" width="26" height="160" rx="8" fill="{CORAL}"/><path d="M455 320 l13 34 l13 -34z" fill="#3B2B20"/><rect x="455" y="160" width="26" height="22" rx="6" fill="{GOLD}"/></g>',
  f'<path d="M520 120 l10 22 l24 2 l-18 15 l6 24 l-22 -13 l-22 13 l6 -24 l-18 -15 l24 -2z" fill="{GOLD}"/>'])
scenes['contract_sign']=s
for k,v in scenes.items(): open(f'{k}.svg','w').write(wrap(v))
print(len(scenes))
