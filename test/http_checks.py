import urllib.request, urllib.parse, urllib.error, http.cookiejar, re, threading, socketserver, email, email.policy, time, json
from pathlib import Path
BASE='http://localhost:10109/Freshora'
Path('tmp').mkdir(exist_ok=True)
messages=[]
class SMTP(socketserver.StreamRequestHandler):
 def handle(self):
  self.wfile.write(b'220 localhost test SMTP\r\n')
  while True:
   line=self.rfile.readline()
   if not line:return
   command=line.decode().upper()
   if command.startswith('EHLO'):self.wfile.write(b'250-localhost\r\n250 8BITMIME\r\n')
   elif command.startswith('DATA'):
    self.wfile.write(b'354 Send message\r\n'); data=b''
    while True:
     part=self.rfile.readline()
     if part==b'.\r\n':break
     if not part:return
     data+=part
    messages.append(data);self.wfile.write(b'250 Accepted\r\n')
   elif command.startswith('QUIT'):self.wfile.write(b'221 Bye\r\n');return
   else:self.wfile.write(b'250 OK\r\n')
class Client:
 def __init__(self): self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
 def get(self,path,data=None):
  req=urllib.request.Request(BASE+path, data=None if data is None else urllib.parse.urlencode(data).encode())
  try:
   r=self.opener.open(req,timeout=20);return r.status,r.read().decode(),r.url
  except urllib.error.HTTPError as e:return e.code,e.read().decode(),e.url
 def post(self,path,values):
  _,body,_=self.get(path)
  token=re.search(r'name="csrf" value="([^"]+)"',body)
  assert token,('CSRF form absent',path)
  return self.get(path,dict(values,csrf=token.group(1)))
 def login(self,address,password):
  code,body,url=self.post('/login',{'email':address,'password':password})
  assert code==200 and not urllib.parse.urlparse(url).path.endswith('/login'),(code,url)
log=[]
def check(value,name):
 assert value,name
 log.append('PASS '+name);print(log[-1],flush=True)
smtp=socketserver.ThreadingTCPServer(('127.0.0.1',10259),SMTP)
threading.Thread(target=smtp.serve_forever,daemon=True).start()
try:
 c=Client()
 for path in ['/home','/products','/products?availability=all','/products?availability=out','/compare?sort=unitPrice','/shops','/register/customer','/register/seller','/forgot','/reset']:
  status,body,_=c.get(path);check(status==200 and 'Exception' not in body,'JSP '+path)
 seller=Client();seller.login('seller@freshora.demo','Freshora@123')
 code,body,_=seller.get('/seller');check(code==200,'seller dashboard JSP')
 for param in ['editListing','editVariant']:
  match=re.search(r'\?'+param+r'=(\d+)',body)
  if match:
   code,edit,_=seller.get('/seller?'+param+'='+match.group(1))
   check(code==200 and 'readonly value="'+match.group(1)+'"' in edit,param+' prefilled form')
 check(seller.get('/seller/suggestions?q=Ca')[0]==200,'seller suggestions JSON')
 check(c.get('/seller/suggestions?q=Ca')[2].endswith('/login'),'anonymous suggestions blocked')
 admin=Client();admin.login('admin@freshora.demo','Freshora@123');check(admin.get('/admin')[0]==200,'admin dashboard JSP')
 tag=str(int(time.time()*1000));address='iter1-http-'+tag+'@test.invalid';password='TestOnly@123'
 with Path('tmp/iter1-http-emails.txt').open('a') as saved:saved.write(address+'\n')
 status,body,url=c.post('/register/customer',{'email':address,'password':password,'confirm':password,'name':'Iteration test','phone':'0900000000'})
 check(status==200 and '/login?success=1' in url,'customer registration')
 c.login(address,password);other=Client();other.login(address,password)
 check(c.get('/seller')[0]==403,'customer cannot access seller')
 check(c.get('/admin')[0]==403,'customer cannot access admin')
 check(c.get('/account')[0]==200 and c.get('/cart')[0]==200,'account and cart JSP')
 check(c.get('/account',{'action':'profile','name':'No CSRF'})[0]==403,'missing CSRF rejected')
 code,body,url=c.post('/account',{'action':'password','old':password,'password':'Changed@123','confirm':'Changed@123'})
 check('/login' in url,'password change logs out current session')
 check('/login' in other.get('/account')[2],'password change revokes another session')
 c.login(address,'Changed@123')
 recovery=Client();code,body,_=recovery.post('/forgot',{'email':address})
 check(code==200 and len(messages)==1,'recovery email delivered via SMTP')
 msg=email.message_from_bytes(messages[0],policy=email.policy.default)
 token=re.search(r'/reset\?token=([a-f0-9]+)',msg.get_content()).group(1)
 code,body,url=recovery.post('/reset?token='+token,{'token':token,'password':'Recovered@123','confirm':'Recovered@123'})
 check('/login?success=1' in url,'email token resets password')
 check('/login' in c.get('/account')[2],'password recovery revokes previous session')
 code,body,url=recovery.post('/reset?token='+token,{'token':token,'password':'Again@123','confirm':'Again@123'})
 check('already been used' in body,'recovery token cannot be reused')
 Client().login(address,'Recovered@123');check(True,'login succeeds with recovered password')
 Path('tmp/iter1-http-results.txt').write_text('\n'.join(log))
finally:
 smtp.shutdown();smtp.server_close()
