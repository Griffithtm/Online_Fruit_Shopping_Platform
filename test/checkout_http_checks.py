import urllib.request,urllib.parse,urllib.error,http.cookiejar,re,time
from pathlib import Path
Path('tmp/checkout').mkdir(parents=True,exist_ok=True)
base='http://localhost:10109/Freshora'
class Client:
 def __init__(self):self.o=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
 def req(self,path,data=None):
  try:
   r=self.o.open(base+path,None if data is None else urllib.parse.urlencode(data).encode());return r.status,r.read().decode(),r.url
  except urllib.error.HTTPError as e:return e.code,e.read().decode(),e.url
 def token(self,path):return re.search(r'name="csrf" value="([^"]+)"',self.req(path)[1]).group(1)
 def post(self,path,d):return self.req(path,dict(d,csrf=self.token(path)))
def check(ok,name):
 assert ok,name
 print('PASS',name,flush=True)
c=Client();check(c.req('/checkout')[2].endswith('/login'),'checkout requires login')
email='checkout-http-'+str(int(time.time()*1000))+'@test.invalid';Path('tmp/checkout/email.txt').write_text(email)
c.post('/register/customer',dict(email=email,password='TestOnly@123',confirm='TestOnly@123',name='Checkout Test',phone='0900000000'))
c.post('/login',dict(email=email,password='TestOnly@123'))
c.post('/account',dict(action='saveAddress',name='Checkout Test',phone='0900000000',line='1 Test',ward='Test',district='Test',city='Hà Nội',primary='on'))
c.post('/cart',dict(action='add',id=1,quantity=1))
body=c.req('/checkout')[1];address=re.search(r'<option value="(\d+)"',body).group(1)
status,body,_=c.req('/checkout?address='+address);check(status==200 and 'Place order' in body,'checkout JSP and delivery quote')
key=re.search(r'name="key" value="([^"]+)"',body).group(1);csrf=c.token('/account')
check(c.req('/checkout',dict(key=key))[0]==403,'CSRF enforced')
status,body,url=c.req('/checkout',dict(key=key,csrf=csrf));check(status==200 and '/orders?placed=' in url,'COD order placed with stock reservation')
oid=re.search(r'name="id" value="(\d+)"',body).group(1)
status,body,url2=c.req('/checkout',dict(key=key,csrf=csrf));check(url2==url,'duplicate submission returns same order')
other=Client();other.post('/login',dict(email='customer@freshora.demo',password='Freshora@123'))
status,body,_=other.post('/orders',dict(action='cancel',id=oid));check('Order not found.' in body,'other customer cannot cancel order')
status,body,_=c.post('/orders',dict(action='cancel',id=oid));check(status==200 and 'CANCELLED' in body,'customer cancels and releases stock')
status,body,_=c.post('/orders',dict(action='cancel',id=oid));check(status==200 and 'CANCELLED' in body,'repeat cancellation is safe')
